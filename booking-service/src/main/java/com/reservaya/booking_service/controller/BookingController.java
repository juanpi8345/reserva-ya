package com.reservaya.booking_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reservaya.booking_service.model.BookingResponse;
import com.reservaya.booking_service.model.CreateBookingRequest;
import com.reservaya.booking_service.model.CreateBookingResponse;
import com.reservaya.booking_service.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/bookings")
public class BookingController {
	
	private final BookingService bookingService;
	
	@PostMapping
	public ResponseEntity<CreateBookingResponse> createBooking(@RequestBody @Valid CreateBookingRequest request,
																@RequestHeader("X-User-Id") Long userId) {
		CreateBookingResponse response = bookingService.publishCreatedBookingEvent(userId, request);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}
	
	@GetMapping
	public ResponseEntity<List<BookingResponse>> getBookings() {
		List<BookingResponse> response = bookingService.getBookings();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<BookingResponse> getBookingById(@RequestHeader("X-User-Id") Long userId,
														  @PathVariable UUID id) {
		BookingResponse response = bookingService.getBookingById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/user")
	public ResponseEntity<List<BookingResponse>> getUserBookings(@RequestHeader("X-User-Id") Long userId) {
		List<BookingResponse> response = bookingService.getUserBookings(userId);
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancelUserBooking(@RequestHeader("X-User-Id") Long userId, @PathVariable UUID id){
		bookingService.publishCancelledBookingEvent(id, userId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
