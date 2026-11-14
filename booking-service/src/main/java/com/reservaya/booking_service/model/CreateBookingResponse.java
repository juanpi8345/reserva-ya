package com.reservaya.booking_service.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)  
public record CreateBookingResponse(UUID bookingId, BookingStatus status) {

}
