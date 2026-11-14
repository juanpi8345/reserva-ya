package com.reservaya.booking_service.util;

public class Constants {
	
	public static final String OVERLAPPING_BOOKING_REASON = "The resource is already booked for the selected dates.";
	public static final String INTERNAL_SERVER_ERROR = "Internal server error.";
	public static final String RESOURCE_NOT_FOUND = "Resource not found.";
	public static final String MISSING_PARAMETER = "Missing required parameter: ";
	public static final String INVALID_PARAMETER = "Invalid value for parameter: ";
	public static final String VALIDATION_DEFAULT_ERROR = "Validation failed.";
	public static final String INVALID_REQUEST_BODY = "The request body is invalid or malformed.";
	public static final String INVALID_BOOKING_DATES = "The check-out date must be after the check-in date.";
}
