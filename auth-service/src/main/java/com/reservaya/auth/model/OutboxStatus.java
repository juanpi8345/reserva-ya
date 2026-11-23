package com.reservaya.auth.model;

public enum OutboxStatus {
	PENDING,
	PUBLISHED,
	PROCESSING,
	FAILED
}
