package com.reservaya.booking_service.exception;

public class BookingNotAvailableException extends RuntimeException{

	private static final long serialVersionUID = -5384956988598776066L;

	public BookingNotAvailableException(String message) {
		super(message);
	}
	
}
