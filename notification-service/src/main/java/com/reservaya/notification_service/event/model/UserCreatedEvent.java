package com.reservaya.notification_service.event.model;

import lombok.Data;

@Data
public class UserCreatedEvent{
	private Long userId;
	private String phone;
	private String email;
}
