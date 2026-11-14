package com.reservaya.booking_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

	@NotNull(message = "The resource type is required (HOTEL or FLIGHT)")
	private ResourceType resourceType;

	@NotNull(message = "The resource ID is required")
	private UUID resourceId;

	@NotNull(message = "The check-in date is required")
	@FutureOrPresent(message = "The check-in date must be today or in the future")
	private LocalDateTime checkIn;

	@NotNull(message = "The check-out date is required")
	@Future(message = "The check-out date must be in the future")
	private LocalDateTime checkOut;

	@NotNull(message = "The total amount is required")
	@DecimalMin(value = "0.01", inclusive = true, message = "The total amount must be greater than 0")
	@Digits(integer = 10, fraction = 2, message = "The amount must have a maximum of 10 integer digits and 2 decimal places")
	private BigDecimal totalAmount;
}
