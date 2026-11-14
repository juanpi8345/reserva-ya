package com.reservaya.notification_service.service;

import java.util.List;

import com.reservaya.notification_service.model.GetNotificationResponse;
import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.NotificationType;

public interface NotificationService {

    void sendNotification(NotificationType eventType, NotificationContext context);
    void sendPendingNotifications(Long userId, String email);
    List<GetNotificationResponse> getNotifications(Long userId);
}
