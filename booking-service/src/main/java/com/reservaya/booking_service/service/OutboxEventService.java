package com.reservaya.booking_service.service;

import java.util.List;

import com.reservaya.booking_service.entity.OutboxEvent;

public interface OutboxEventService {
	
	List<OutboxEvent> getPendingEvents();
	void publishPendingEvents(List<OutboxEvent> pendingEvents);
}
