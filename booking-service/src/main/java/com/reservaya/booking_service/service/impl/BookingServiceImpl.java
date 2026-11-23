package com.reservaya.booking_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.reservaya.booking_service.entity.Booking;
import com.reservaya.booking_service.entity.OutboxEvent;
import com.reservaya.booking_service.event.model.BookingCancelledEvent;
import com.reservaya.booking_service.event.model.BookingCreatedEvent;
import com.reservaya.booking_service.event.model.PaymentEvent;
import com.reservaya.booking_service.exception.BookingNotAvailableException;
import com.reservaya.booking_service.exception.BookingNotFoundException;
import com.reservaya.booking_service.exception.InvalidBookingDatesException;
import com.reservaya.booking_service.model.BookingResponse;
import com.reservaya.booking_service.model.BookingStatus;
import com.reservaya.booking_service.model.CreateBookingRequest;
import com.reservaya.booking_service.model.CreateBookingResponse;
import com.reservaya.booking_service.model.EventType;
import com.reservaya.booking_service.model.OutboxStatus;
import com.reservaya.booking_service.model.PaymentStatus;
import com.reservaya.booking_service.model.ResourceType;
import com.reservaya.booking_service.repository.BookingRepository;
import com.reservaya.booking_service.repository.OutboxEventRepository;
import com.reservaya.booking_service.service.BookingService;
import com.reservaya.booking_service.util.Constants;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final OutboxEventRepository outboxEventRepository;
	private final ObjectMapper objectMapper;

	@Caching(evict = { @CacheEvict(value = "allBookings", key = "'all'"),
			@CacheEvict(value = "userBookings", key = "#userId") })
	@Transactional
	@Override
	public CreateBookingResponse createBooking(Long userId, CreateBookingRequest request) {
		Booking booking = new Booking();
		LocalDateTime checkIn = request.getCheckIn();
		LocalDateTime checkOut = request.getCheckOut();
		ResourceType resourceType = request.getResourceType();
		UUID resourceId = request.getResourceId();
		BigDecimal totalAmount = request.getTotalAmount();

		validateNewBooking(resourceId, checkIn, checkOut);

		booking.setCheckIn(checkIn);
		booking.setCheckOut(checkOut);
		booking.setResourceType(resourceType);
		booking.setResourceId(resourceId);
		booking.setStatus(BookingStatus.PENDING);
		booking.setTotalAmount(totalAmount);
		booking.setUserId(userId);

		Booking savedBooking = bookingRepository.save(booking);

		BookingCreatedEvent createdEvent = BookingCreatedEvent.builder().checkIn(checkIn).checkOut(checkOut)
				.bookingId(savedBooking.getId()).createdAt(LocalDateTime.now()).resourceId(resourceId)
				.totalAmount(totalAmount).userId(userId).resourceType(resourceType).build();

		OutboxEvent event = OutboxEvent.builder().eventType(EventType.BOOKING_CREATED)
				.partitionKey(savedBooking.getId()).status(OutboxStatus.PENDING)
				.payload(objectMapper.writeValueAsString(createdEvent))
				.build();

		outboxEventRepository.save(event);

		return new CreateBookingResponse(savedBooking.getId(), savedBooking.getStatus());
	}

	private void validateNewBooking(UUID resourceId, LocalDateTime checkIn, LocalDateTime checkout) {
		if (!checkIn.isBefore(checkout))
			throw new InvalidBookingDatesException(Constants.INVALID_BOOKING_DATES);

		boolean existsOverlappingBooking = bookingRepository.existsOverlappingBooking(resourceId, checkIn, checkout);

		if (Boolean.TRUE.equals(existsOverlappingBooking))
			throw new BookingNotAvailableException(Constants.OVERLAPPING_BOOKING_REASON);
	}

	@Cacheable(value = "bookings", key = "#id")
	@Override
	public BookingResponse getBookingById(UUID id) {
		Booking booking = bookingRepository.findById(id)
				.orElseThrow(() -> new BookingNotFoundException(Constants.RESOURCE_NOT_FOUND));
		return BookingResponse.fromEntity(booking);
	}

	@Cacheable(value = "userBookings", key = "#userId")
	@Override
	public List<BookingResponse> getUserBookings(Long userId) {
		return bookingRepository.findByUserId(userId).stream().map(BookingResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Cacheable(value = "allBookings", key = "'all'")
	@Override
	public List<BookingResponse> getBookings() {
		return bookingRepository.findAll().stream().map(BookingResponse::fromEntity).collect(Collectors.toList());
	}

	@Transactional
	@Caching(evict = { @CacheEvict(value = "bookings", key = "#id"),
			@CacheEvict(value = "userBookings", key = "#userId"), @CacheEvict(value = "allBookings", key = "'all'") })
	@Override
	public void cancelBooking(UUID id, Long userId) {
		Booking booking = bookingRepository.findByUserIdAndId(userId, id)
				.orElseThrow(() -> new BookingNotFoundException(Constants.RESOURCE_NOT_FOUND));
		if (BookingStatus.CANCELLED.equals(booking.getStatus()))
			return;
		booking.setStatus(BookingStatus.CANCELLED);

		bookingRepository.save(booking);
		
		BookingCancelledEvent cancelledEvent = BookingCancelledEvent.builder()
		        .bookingId(booking.getId())
		        .userId(userId)
		        .cancelledAt(LocalDateTime.now())
		        .build();

		OutboxEvent event = OutboxEvent.builder().eventType(EventType.BOOKING_CANCELLED).partitionKey(booking.getId())
				.status(OutboxStatus.PENDING)
				.payload(objectMapper.writeValueAsString(cancelledEvent))
				.build();

		outboxEventRepository.save(event);
	}

	@Transactional
	@Caching(evict = { @CacheEvict(value = "bookings", key = "#paymentEvent.getBookingId()"),
			@CacheEvict(value = "userBookings", key = "#paymentEvent.getUserId()"),
			@CacheEvict(value = "allBookings", key = "'all'") })
	@Override
	public void processBookingPayment(PaymentEvent paymentEvent) {
		Booking booking = bookingRepository.findByUserIdAndId(paymentEvent.getUserId(), paymentEvent.getBookingId())
				.orElseThrow(() -> new BookingNotFoundException(Constants.RESOURCE_NOT_FOUND));

		if (!BookingStatus.PENDING.equals(booking.getStatus())) {
			log.warn("El pago para bookingId={} ya fue procesado con anterioridad. Ignorando evento duplicado.",
					booking.getId());
			return;
		}

		BookingStatus bookingStatus = PaymentStatus.APPROVED.equals(paymentEvent.getPaymentStatus())
				? BookingStatus.CONFIRMED
				: BookingStatus.PAYMENT_FAILED;
		booking.setStatus(bookingStatus);
		bookingRepository.save(booking);
	}
}
