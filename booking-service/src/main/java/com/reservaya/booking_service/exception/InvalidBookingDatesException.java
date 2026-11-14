package com.reservaya.booking_service.exception;

public class InvalidBookingDatesException extends RuntimeException {
	private static final long serialVersionUID = 1962252096506370243L;

	public InvalidBookingDatesException(String message) {
        super(message);
    }
}