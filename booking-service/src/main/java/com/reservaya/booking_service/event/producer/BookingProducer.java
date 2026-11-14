package com.reservaya.booking_service.event.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.reservaya.booking_service.event.model.BookingCancelledEvent;
import com.reservaya.booking_service.event.model.BookingCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${app.kafka.topics.booking-created.name:booking.created}")
	private String bookingCreatedTopic;

	@Value("${app.kafka.topics.booking-cancelled.name:booking.cancelled}")
	private String bookingCancelledTopic;

	public void sendBookingCreated(BookingCreatedEvent event) {
		String key = event.getBookingId().toString();
		log.info("Publicando evento BookingCreatedEvent con key={} en tópico={}", key, bookingCreatedTopic);

		CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(bookingCreatedTopic, key, event);

		future.whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("Error al publicar BookingCreatedEvent para bookingId={}: {}", key, ex.getMessage(), ex);
			} else {
				log.info("BookingCreatedEvent publicado con éxito. Tópico={}, Partición={}, Offset={}",
						result.getRecordMetadata().topic(),
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			}
		});
	}

	public void sendBookingCancelled(BookingCancelledEvent event) {
		String key = event.getBookingId().toString();
		log.info("Publicando evento BookingCancelledEvent con key={} en tópico={}", key, bookingCancelledTopic);

		CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(bookingCancelledTopic, key, event);

		future.whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("Error al publicar BookingCancelledEvent para bookingId={}: {}", key, ex.getMessage(), ex);
			} else {
				log.info("BookingCancelledEvent publicado con éxito. Tópico={}, Partición={}, Offset={}",
						result.getRecordMetadata().topic(),
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			}
		});
	}
}
