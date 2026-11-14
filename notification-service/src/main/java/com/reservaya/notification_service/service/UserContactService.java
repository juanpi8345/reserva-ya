package com.reservaya.notification_service.service;

import com.reservaya.notification_service.entity.UserContact;
import com.reservaya.notification_service.event.model.UserCreatedEvent;

public interface UserContactService {
	UserContact getUserContact(Long userId);
	UserContact saveUserContact(UserCreatedEvent event);
}
