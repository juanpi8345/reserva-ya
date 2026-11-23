package com.reservaya.payment_service.model;

public enum OutboxStatus {
	PENDING,
	PUBLISHED,
	PROCESSING,
	FAILED
}
