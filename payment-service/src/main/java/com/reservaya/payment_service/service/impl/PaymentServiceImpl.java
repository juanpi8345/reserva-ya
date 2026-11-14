package com.reservaya.payment_service.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reservaya.payment_service.entity.Payment;
import com.reservaya.payment_service.event.model.BookingCreatedEvent;
import com.reservaya.payment_service.event.model.PaymentEvent;
import com.reservaya.payment_service.event.producer.PaymentProducer;
import com.reservaya.payment_service.exception.PaymentNotFoundException;
import com.reservaya.payment_service.model.GetPaymentResponse;
import com.reservaya.payment_service.model.PaymentStatus;
import com.reservaya.payment_service.repository.PaymentRepository;
import com.reservaya.payment_service.service.PaymentService;
import com.reservaya.payment_service.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentProducer paymentProducer;


	@Transactional
	@Caching(evict = {
			@CacheEvict(value = "payments", key = "'all'"),
			@CacheEvict(value = "bookingPayment", key = "#event.getBookingId()") 
			})
	@Override
	public void processBookingPayment(BookingCreatedEvent event) {
		log.info("Iniciando procesamiento de pago para bookingId={}, userId={}, monto={}",
				event.getBookingId(), event.getUserId(), event.getTotalAmount());

		if (paymentRepository.existsByBookingId(event.getBookingId())) {
			log.warn("El pago para bookingId={} ya fue procesado con anterioridad. Ignorando evento duplicado.",
					event.getBookingId());
			return;
		}
		
		//Simular pago, aproximadamente el 90% seran exitosos
		boolean isPaymentSuccessful = Math.random() >= 0.1;
		processPayment(event,isPaymentSuccessful);
		
	}

	private void processPayment(BookingCreatedEvent event, boolean isPaymentSuccessful) {
		PaymentStatus paymentStatus = isPaymentSuccessful ? PaymentStatus.APPROVED : PaymentStatus.FAILED;
		Payment payment = Payment.builder()
				.bookingId(event.getBookingId())
				.userId(event.getUserId())
				.amount(event.getTotalAmount() != null ? event.getTotalAmount() : BigDecimal.ZERO)
				.status(paymentStatus)
				.build();

		Payment savedPayment = paymentRepository.save(payment);
		log.info("Pago {} con ID={} para bookingId={}",paymentStatus.toString(), savedPayment.getId(), event.getBookingId());

		PaymentEvent paymentEvent = PaymentEvent.builder()
				.bookingId(event.getBookingId())
				.userId(event.getUserId())
				.paymentStatus(paymentStatus)
				.build();

		paymentProducer.sendPaymentEvent(paymentEvent,paymentStatus);
	}

	@Cacheable(value = "payments", key = "'all'")
	@Override
	public List<GetPaymentResponse> getAllPayments() {
		List<Payment> payments = paymentRepository.findAll();
		List<GetPaymentResponse> paymentsResponse = new ArrayList<GetPaymentResponse>();
		payments.forEach(payment ->{
			GetPaymentResponse paymentResponse = adaptToPaymentResponse(payment);
			paymentsResponse.add(paymentResponse);
		});
		return paymentsResponse;
	}

	@Cacheable(value = "bookingPayment", key = "#bookingId")
	@Override
	public GetPaymentResponse getPayment(UUID bookingId) {
		Payment payment = paymentRepository.findByBookingId(bookingId)
							.orElseThrow(() -> new PaymentNotFoundException(Constants.RESOURCE_NOT_FOUND));
		return adaptToPaymentResponse(payment);
	}
	
	private GetPaymentResponse adaptToPaymentResponse(Payment payment) {
		GetPaymentResponse response = 
				new GetPaymentResponse(payment.getId(),payment.getBookingId(),payment.getUserId(),
						 payment.getAmount(), payment.getStatus(), payment.getProcessedAt());
		return response;
	}
	
}
