package com.reservaya.notification_service.service;

import java.util.Map;

public interface EmailService {

    void send(String to, String subject, String templateName, Map<String, Object> variables);
}
