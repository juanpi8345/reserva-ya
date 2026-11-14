package com.reservaya.payment_service.exception;

import org.apache.kafka.common.errors.ResourceNotFoundException;

public class PaymentNotFoundException extends ResourceNotFoundException {

	private static final long serialVersionUID = 1L;

	public PaymentNotFoundException(String message) {
		super(message);
	}
}
