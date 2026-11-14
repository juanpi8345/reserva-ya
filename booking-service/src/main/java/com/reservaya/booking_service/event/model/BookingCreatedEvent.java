package com.reservaya.booking_service.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.reservaya.booking_service.model.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {

	private UUID bookingId;
	private Long userId;
	private UUID resourceId;
	private ResourceType resourceType;
	private LocalDateTime checkIn;
	private LocalDateTime checkOut;
	private BigDecimal totalAmount;
	private LocalDateTime createdAt;
}
