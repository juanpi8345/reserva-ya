package com.reservaya.payment_service.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.reservaya.payment_service.event.model.BookingCreatedEvent;
import com.reservaya.payment_service.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

	private final PaymentService paymentService;

	@KafkaListener(
			topics = "${app.kafka.topics.booking-created.name}",
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void consumeBookingCreated(
			@Payload BookingCreatedEvent event,
			@Header(KafkaHeaders.RECEIVED_KEY) String key,
			@Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
			@Header(KafkaHeaders.OFFSET) long offset) {

		log.info("Evento BookingCreatedEvent recibido. Key={}, Partición={}, Offset={}, BookingId={}",
				key, partition, offset, event.getBookingId());

		paymentService.processBookingPayment(event);
	}
}
