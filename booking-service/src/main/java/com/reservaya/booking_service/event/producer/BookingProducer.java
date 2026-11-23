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

	public CompletableFuture<SendResult<String, Object>> sendBookingCreated(BookingCreatedEvent event) {
		String key = event.getBookingId().toString();
		log.info("Publicando evento BookingCreatedEvent con key={} en tópico={}", key, bookingCreatedTopic);

		return kafkaTemplate.send(bookingCreatedTopic, key, event);
	}

	public CompletableFuture<SendResult<String, Object>> sendBookingCancelled(BookingCancelledEvent event) {
		String key = event.getBookingId().toString();
		log.info("Publicando evento BookingCancelledEvent con key={} en tópico={}", key, bookingCancelledTopic);

		return kafkaTemplate.send(bookingCancelledTopic, key, event);
	}
}
