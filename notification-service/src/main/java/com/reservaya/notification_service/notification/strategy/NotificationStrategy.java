package com.reservaya.notification_service.notification.strategy;

import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.NotificationType;

public interface NotificationStrategy {
    void sendNotification(NotificationContext context);
    NotificationType supports();
}
