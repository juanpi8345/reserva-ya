package com.reservaya.payment_service.service;

import java.util.List;

import com.reservaya.payment_service.entity.OutboxEvent;

public interface OutboxEventService {
	
	List<OutboxEvent> getPendingEvents();
	void publishPendingEvents(List<OutboxEvent> pendingEvents);
}
