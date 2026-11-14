package com.reservaya.payment_service.model;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, String message) {

}
