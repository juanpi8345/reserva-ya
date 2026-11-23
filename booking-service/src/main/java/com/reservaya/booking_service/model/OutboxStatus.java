package com.reservaya.booking_service.model;

public enum OutboxStatus {
	PENDING,
	PUBLISHED,
	PROCESSING,
	FAILED
}
