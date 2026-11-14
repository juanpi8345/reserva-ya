package com.reservaya.payment_service.service;

import java.util.List;
import java.util.UUID;

import com.reservaya.payment_service.event.model.BookingCreatedEvent;
import com.reservaya.payment_service.model.GetPaymentResponse;

public interface PaymentService {
	void processBookingPayment(BookingCreatedEvent event);
	List<GetPaymentResponse> getAllPayments();
	GetPaymentResponse getPayment(UUID bookingId);
}
