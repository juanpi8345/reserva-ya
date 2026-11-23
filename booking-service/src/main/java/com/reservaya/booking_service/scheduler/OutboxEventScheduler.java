
package com.reservaya.booking_service.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.reservaya.booking_service.entity.OutboxEvent;
import com.reservaya.booking_service.service.OutboxEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

	private final OutboxEventService outboxEventService;

	@Scheduled(fixedDelayString = "${app.kafka.outbox.scheduler.fixed-delay-ms}")
	public void processOutbox() {
		List<OutboxEvent> pendingEvents = outboxEventService.getPendingEvents();

		if (pendingEvents == null || pendingEvents.isEmpty())
			return;

		outboxEventService.publishPendingEvents(pendingEvents);
	}
}