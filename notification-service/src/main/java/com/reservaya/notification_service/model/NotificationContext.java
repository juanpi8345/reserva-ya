package com.reservaya.notification_service.model;

import lombok.Data;

@Data
public class NotificationContext {
    protected Long userId;
    protected String email;
}
