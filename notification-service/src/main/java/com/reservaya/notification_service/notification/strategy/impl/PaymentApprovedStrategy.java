package com.reservaya.notification_service.notification.strategy.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.reservaya.notification_service.event.model.PaymentEvent;
import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.NotificationType;
import com.reservaya.notification_service.notification.strategy.NotificationStrategy;
import com.reservaya.notification_service.service.impl.EmailServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentApprovedStrategy implements NotificationStrategy {

    private final EmailServiceImpl emailService;

    @Override
    public void sendNotification(NotificationContext context) {
        if (!(context instanceof PaymentEvent paymentEvent)) {
            throw new IllegalArgumentException(
                "PaymentApprovedStrategy esperaba PaymentEvent pero recibió: "
                + context.getClass().getSimpleName()
            );
        }
        log.info("Enviando confirmación de pago para bookingId={}", paymentEvent.getBookingId());

        Map<String, Object> variables = Map.of(
                "bookingId", paymentEvent.getBookingId().toString()
        );

        emailService.send(
                context.getEmail(),
                "¡Tu reserva fue confirmada! 🎉",
                "payment_approved",
                variables
        );
    }

    @Override
    public NotificationType supports() {
        return NotificationType.PAYMENT_APPROVED;
    }
}
