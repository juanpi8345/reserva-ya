package com.reservaya.payment_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reservaya.payment_service.model.GetPaymentResponse;
import com.reservaya.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {
	
	private final PaymentService paymentService;
	
	@GetMapping
	public ResponseEntity<List<GetPaymentResponse>> getAllPayments(){
		return ResponseEntity.ok(paymentService.getAllPayments());
	}
	
	@GetMapping("/{bookingId}")
	public ResponseEntity<GetPaymentResponse> getPayment(@PathVariable UUID bookingId){
		return ResponseEntity.ok(paymentService.getPayment(bookingId));
	}
	

}
