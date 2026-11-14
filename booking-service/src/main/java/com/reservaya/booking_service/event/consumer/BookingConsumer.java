package com.reservaya.booking_service.event.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.reservaya.booking_service.event.model.PaymentEvent;
import com.reservaya.booking_service.service.BookingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConsumer {
	
	private final BookingService bookingService;
	
	@KafkaListener(
			topics = {
					 "${app.kafka.topics.payment-approved.name}",
			       	 "${app.kafka.topics.payment-failed.name}"
					 },
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void consumePaymentEvent(
			@Payload PaymentEvent event,
			@Header(KafkaHeaders.RECEIVED_KEY) String key,
			@Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
			@Header(KafkaHeaders.OFFSET) long offset) {

		log.info("Evento PaymentEvent recibido. Key={}, Partición={}, Offset={}, BookingId={}",
				key, partition, offset, event.getBookingId());
		bookingService.processBookingPayment(event);
	}
}
