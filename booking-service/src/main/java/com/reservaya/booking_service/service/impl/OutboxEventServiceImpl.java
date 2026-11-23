package com.reservaya.booking_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.reservaya.booking_service.entity.OutboxEvent;
import com.reservaya.booking_service.event.model.BookingCancelledEvent;
import com.reservaya.booking_service.event.model.BookingCreatedEvent;
import com.reservaya.booking_service.event.producer.BookingProducer;
import com.reservaya.booking_service.model.EventType;
import com.reservaya.booking_service.model.OutboxStatus;
import com.reservaya.booking_service.repository.OutboxEventRepository;
import com.reservaya.booking_service.service.OutboxEventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxEventServiceImpl implements OutboxEventService {
	
	private final BookingProducer bookingProducer;
	private final OutboxEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.kafka.outbox.max-retries}")
    private int maxRetries;
	
	@Override
	public void publishPendingEvents(List<OutboxEvent> pendingEvents) {
		pendingEvents.forEach(event ->{
	        String payload = event.getPayload();
	        
	        if(EventType.BOOKING_CREATED.equals(event.getEventType())) {
	        	BookingCreatedEvent bookingCreatedEvent = parsePayload(payload, BookingCreatedEvent.class);
	        	CompletableFuture<SendResult<String, Object>> future = bookingProducer.sendBookingCreated(bookingCreatedEvent);
	        	handleCompletableFuture(future,event);
	        	
	        }else if(EventType.BOOKING_CANCELLED.equals(event.getEventType())) {
	        	BookingCancelledEvent bookingCancelledEvent = parsePayload(payload, BookingCancelledEvent.class);
	        	CompletableFuture<SendResult<String, Object>> future = bookingProducer.sendBookingCancelled(bookingCancelledEvent);
	        	handleCompletableFuture(future, event);
	        }
		});
	}
	
	
	private <T> T parsePayload(String payload, Class<T> eventClass) {
	   return objectMapper.readValue(payload, eventClass);
	}
	
	private void handleCompletableFuture(CompletableFuture<SendResult<String, Object>> future, OutboxEvent event) {
		future.whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("Error al publicar {} para bookingId={}: {}", event.getEventType() ,event.getPartitionKey(), ex.getMessage(), ex);
				event.setRetryCount(event.getRetryCount()+1);
				if(event.getRetryCount() >= maxRetries) {
					event.setStatus(OutboxStatus.FAILED);
					event.setProcessedAt(LocalDateTime.now());
				}else {
					event.setStatus(OutboxStatus.PENDING);
				}
			} else {
				log.info("{} publicado con éxito. Tópico={}, Partición={}, Offset={}",
						event.getEventType(),
						result.getRecordMetadata().topic(),
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
				event.setStatus(OutboxStatus.PUBLISHED);
				event.setProcessedAt(LocalDateTime.now());
			}
			eventRepository.save(event);
		});;
	}


	@Transactional
	@Override
	public List<OutboxEvent> getPendingEvents() {
        List<OutboxEvent> events = eventRepository.findPendingEventsToProcess();

        events.forEach(event ->
            event.setStatus(OutboxStatus.PROCESSING)
        );

        return events;
	}
}
