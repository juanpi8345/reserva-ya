package com.reservaya.auth.event.model;

public record UserCreatedEvent(Long userId,
							   String phone,
							   String email) {}
