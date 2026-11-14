package com.reservaya.notification_service.notification.strategy.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.reservaya.notification_service.event.model.BookingCancelledEvent;
import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.NotificationType;
import com.reservaya.notification_service.notification.strategy.NotificationStrategy;
import com.reservaya.notification_service.service.impl.EmailServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCancelledStrategy implements NotificationStrategy {

    private final EmailServiceImpl emailService;

    @Override
    public void sendNotification(NotificationContext context) {
        if (!(context instanceof BookingCancelledEvent bookingCancelledEvent)) {
            throw new IllegalArgumentException("BookingCancelledStrategy esperaba BookingCancelledEvent pero recibió: "
            		+ context.getClass().getSimpleName()
            );
        }
        
        log.info("Enviando notificación de cancelación para bookingId={}", bookingCancelledEvent.getBookingId());

        Map<String, Object> variables = Map.of(
                "bookingId", bookingCancelledEvent.getBookingId().toString(),
                "cancelledAt", bookingCancelledEvent.getCancelledAt()
        );

        emailService.send(
                context.getEmail(),
                "Tu reserva fue cancelada",
                "booking_cancelled",
                variables
        );
    }

    @Override
    public NotificationType supports() {
        return NotificationType.BOOKING_CANCELLED;
    }
}
