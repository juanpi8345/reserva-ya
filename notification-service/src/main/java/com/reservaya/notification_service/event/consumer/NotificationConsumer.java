package com.reservaya.notification_service.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.reservaya.notification_service.event.model.BookingCancelledEvent;
import com.reservaya.notification_service.event.model.BookingCreatedEvent;
import com.reservaya.notification_service.event.model.PaymentEvent;
import com.reservaya.notification_service.event.model.UserCreatedEvent;
import com.reservaya.notification_service.model.NotificationType;
import com.reservaya.notification_service.service.NotificationService;
import com.reservaya.notification_service.service.UserContactService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final UserContactService userContactService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-created.name}",
            groupId = "${app.kafka.notification-group}"
    )
    public void consumeUserCreatedEvent(
            @Payload UserCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("user.created recibido. Key={}, Partición={}, Offset={}, UserId={}",
                key, partition, offset, event.getUserId());

        userContactService.saveUserContact(event);
        notificationService.sendPendingNotifications(event.getUserId(), event.getEmail());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-approved.name}",
            groupId = "${app.kafka.notification-group}"
    )
    public void consumePaymentApprovedEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("payment.approved recibido. Key={}, Partición={}, Offset={}, BookingId={}",
                key, partition, offset, event.getBookingId());

        notificationService.sendNotification(NotificationType.PAYMENT_APPROVED, event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-failed.name}",
            groupId = "${app.kafka.notification-group}"
    )
    public void consumePaymentFailedEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("payment.failed recibido. Key={}, Partición={}, Offset={}, BookingId={}",
                key, partition, offset, event.getBookingId());

        notificationService.sendNotification(NotificationType.PAYMENT_FAILED, event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.booking-created.name}",
            groupId = "${app.kafka.notification-group}"
    )
    public void consumeBookingCreatedEvent(
            @Payload BookingCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("booking.created recibido. Key={}, Partición={}, Offset={}, BookingId={}",
                key, partition, offset, event.getBookingId());

        notificationService.sendNotification(NotificationType.BOOKING_CREATED, event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.booking-cancelled.name}",
            groupId = "${app.kafka.notification-group}"
    )
    public void consumeBookingCancelled(
            @Payload BookingCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("booking.cancelled recibido. Key={}, Partición={}, Offset={}, BookingId={}",
                key, partition, offset, event.getBookingId());

        notificationService.sendNotification(NotificationType.BOOKING_CANCELLED, event);
    }
}
