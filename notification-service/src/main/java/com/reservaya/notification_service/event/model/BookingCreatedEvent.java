package com.reservaya.notification_service.event.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.reservaya.notification_service.model.NotificationContext;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingCreatedEvent extends NotificationContext{

    private UUID bookingId;
    private UUID resourceId;
    private String resourceType;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
