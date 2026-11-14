package com.reservaya.booking_service.model;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, String message) {

}
