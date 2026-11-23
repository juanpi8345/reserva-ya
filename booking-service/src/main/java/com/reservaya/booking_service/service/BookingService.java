package com.reservaya.booking_service.service;

import java.util.List;
import java.util.UUID;

import com.reservaya.booking_service.event.model.PaymentEvent;
import com.reservaya.booking_service.model.BookingResponse;
import com.reservaya.booking_service.model.CreateBookingRequest;
import com.reservaya.booking_service.model.CreateBookingResponse;

public interface BookingService {

	CreateBookingResponse createBooking(Long userId, CreateBookingRequest request);
	BookingResponse getBookingById(UUID id);
	List<BookingResponse> getUserBookings(Long userId);
	List<BookingResponse> getBookings();
	void cancelBooking(UUID id, Long userId);
	void processBookingPayment(PaymentEvent paymentEvent);
}
