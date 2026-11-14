package com.reservaya.notification_service.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservaya.notification_service.entity.Notification;
import com.reservaya.notification_service.entity.UserContact;
import com.reservaya.notification_service.event.model.BookingCancelledEvent;
import com.reservaya.notification_service.event.model.BookingCreatedEvent;
import com.reservaya.notification_service.event.model.PaymentEvent;
import com.reservaya.notification_service.model.GetNotificationResponse;
import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.NotificationStatus;
import com.reservaya.notification_service.model.NotificationType;
import com.reservaya.notification_service.notification.NotificationStrategyRegistry;
import com.reservaya.notification_service.notification.strategy.NotificationStrategy;
import com.reservaya.notification_service.repository.NotificationRepository;
import com.reservaya.notification_service.service.NotificationService;
import com.reservaya.notification_service.service.UserContactService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationStrategyRegistry strategyRegistry;
    private final UserContactService userContactService;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void sendNotification(NotificationType eventType, NotificationContext context) {
        String idempotencyKey = buildIdempotencyKey(eventType, context);

        if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Notificación con idempotencyKey={} ya fue procesada. Ignorando evento duplicado.", idempotencyKey);
            return;
        }

        Notification notification = new Notification();
        notification.setType(eventType);
        notification.setUserId(context.getUserId());
        notification.setIdempotencyKey(idempotencyKey);
        notification.setPayload(serializePayload(context));

        UserContact contact = userContactService.getUserContact(context.getUserId());

        if (contact == null) {
            log.warn("No se encontró contacto para userId={}. Notificación guardada como PENDING.", context.getUserId());
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
            return;
        }

        context.setEmail(contact.getEmail());
        NotificationStrategy strategy = strategyRegistry.get(eventType);
        strategy.sendNotification(context);

        notification.setSentTo(contact.getEmail());
        notification.setStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendPendingNotifications(Long userId, String email) {
        List<Notification> pendingNotifications =
                notificationRepository.findAllByUserIdAndStatus(userId, NotificationStatus.PENDING);

        if (pendingNotifications.isEmpty()) return;
        
        log.info("Procesando {} notificación(es) pendiente(s) para userId={}", pendingNotifications.size(), userId);

        for (Notification notification : pendingNotifications) {
            try {
                NotificationContext context = deserializePayload(notification.getType(), notification.getPayload());
                context.setEmail(email);

                NotificationStrategy strategy = strategyRegistry.get(notification.getType());
                strategy.sendNotification(context);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentTo(email);
                notificationRepository.save(notification);

                log.info("Notificación pendiente enviada con éxito. Id={}, Tipo={}, UserId={}",
                        notification.getId(), notification.getType(), userId);
            } catch (Exception e) {
                log.error("Error al enviar notificación pendiente Id={}: {}", notification.getId(), e.getMessage(), e);
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);
            }
        }
    }

    private String serializePayload(NotificationContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar payload de notificación: {}", e.getMessage(), e);
            throw new RuntimeException("Error al serializar el evento para la notificación", e);
        }
    }

    private NotificationContext deserializePayload(NotificationType type, String payload) {
        try {
            return switch (type) {
                case PAYMENT_APPROVED, PAYMENT_FAILED -> objectMapper.readValue(payload, PaymentEvent.class);
                case BOOKING_CANCELLED -> objectMapper.readValue(payload, BookingCancelledEvent.class);
                case BOOKING_CREATED -> objectMapper.readValue(payload, BookingCreatedEvent.class);
            };
        } catch (JsonProcessingException e) {
            log.error("Error al deserializar payload para tipo {}: {}", type, e.getMessage(), e);
            throw new RuntimeException("Error al deserializar payload de notificación", e);
        }
    }

    private String buildIdempotencyKey(NotificationType type, NotificationContext context) {
        if (context instanceof PaymentEvent e) {
            return type.name() + ":" + e.getBookingId();
        }
        if (context instanceof BookingCancelledEvent e) {
            return type.name() + ":" + e.getBookingId();
        }
        if (context instanceof BookingCreatedEvent e) {
            return type.name() + ":" + e.getBookingId();
        }
        return type.name() + ":" + context.getUserId();
    }

	@Override
	public List<GetNotificationResponse> getNotifications(Long userId) {
	    return notificationRepository.findAllByUserId(userId)
	            .stream()
	            .map(this::adaptToNotificationResponse)
	            .toList();
	}

	private GetNotificationResponse adaptToNotificationResponse(Notification notification) {
		GetNotificationResponse response = new GetNotificationResponse();
		response.setId(notification.getId());
		response.setCreatedAt(notification.getCreatedAt());
		response.setSentTo(notification.getSentTo());
		response.setStatus(notification.getStatus());
		response.setType(notification.getType());
		response.setUserId(notification.getUserId());
		return response;
	}
}
