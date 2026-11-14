package com.reservaya.notification_service.event.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.reservaya.notification_service.model.NotificationContext;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingCancelledEvent extends NotificationContext{
	private UUID bookingId;
	private LocalDateTime cancelledAt;
}
