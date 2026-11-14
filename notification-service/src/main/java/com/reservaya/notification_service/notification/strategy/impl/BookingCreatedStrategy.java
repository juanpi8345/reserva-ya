package com.reservaya.notification_service.notification.strategy.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.reservaya.notification_service.event.model.BookingCreatedEvent;
import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.NotificationType;
import com.reservaya.notification_service.notification.strategy.NotificationStrategy;
import com.reservaya.notification_service.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedStrategy implements NotificationStrategy {

    private final EmailService emailService;

    @Override
    public void sendNotification(NotificationContext context) {
        if (!(context instanceof BookingCreatedEvent event)) {
            throw new IllegalArgumentException(
                "BookingCreatedStrategy esperaba BookingCreatedEvent pero recibió: "
                + context.getClass().getSimpleName()
            );
        }

        log.info("Enviando notificación de reserva creada para bookingId={}", event.getBookingId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("bookingId", event.getBookingId() != null ? event.getBookingId().toString() : "");
        variables.put("resourceType", event.getResourceType() != null ? event.getResourceType() : "");
        variables.put("checkIn", event.getCheckIn());
        variables.put("checkOut", event.getCheckOut());
        variables.put("totalAmount", event.getTotalAmount());

        emailService.send(
                context.getEmail(),
                "¡Tu reserva fue recibida! 🛎️",
                "booking_created",
                variables
        );
    }

    @Override
    public NotificationType supports() {
        return NotificationType.BOOKING_CREATED;
    }
}
