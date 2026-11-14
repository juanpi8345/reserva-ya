package com.reservaya.notification_service.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class GetNotificationResponse {
    private Long id;
    private Long userId;
    private String sentTo;
    private NotificationType type;
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
