package com.reservaya.payment_service.event.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.reservaya.payment_service.event.model.PaymentEvent;
import com.reservaya.payment_service.model.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${app.kafka.topics.payment-approved.name}")
	private String paymentApprovedTopic;

	@Value("${app.kafka.topics.payment-failed.name}")
	private String paymentFailedTopic;

	public CompletableFuture<SendResult<String, Object>> sendPaymentEvent(PaymentEvent event) {
		String key = event.getBookingId().toString();
		String paymentTopic = PaymentStatus.APPROVED.equals(event.getPaymentStatus()) ?
												paymentApprovedTopic : paymentFailedTopic;
		log.info("Publicando evento PaymentEvent para bookingId={} en tópico={}", key, paymentTopic);

		return kafkaTemplate.send(paymentTopic, key, event);
	}

}
