package com.reservaya.auth.service;

import java.util.List;

import com.reservaya.auth.entity.OutboxEvent;

public interface OutboxEventService {
	
	List<OutboxEvent> getPendingEvents();
	void publishPendingEvents(List<OutboxEvent> pendingEvents);
}
