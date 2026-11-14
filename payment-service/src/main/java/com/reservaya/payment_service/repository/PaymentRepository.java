package com.reservaya.payment_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.reservaya.payment_service.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	boolean existsByBookingId(UUID bookingId);

	Optional<Payment> findByBookingId(UUID bookingId);
}
