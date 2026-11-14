package com.reservaya.booking_service.exception;

import org.apache.kafka.common.errors.ResourceNotFoundException;

public class BookingNotFoundException extends ResourceNotFoundException {

	private static final long serialVersionUID = 1L;

	public BookingNotFoundException(String message) {
		super(message);
	}
}
