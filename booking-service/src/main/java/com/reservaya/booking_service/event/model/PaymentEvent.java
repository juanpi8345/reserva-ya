package com.reservaya.booking_service.event.model;

import java.util.UUID;

import com.reservaya.booking_service.model.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

	private UUID bookingId;
	private Long userId;
	private PaymentStatus paymentStatus;

}
