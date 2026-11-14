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

	public void sendPaymentEvent(PaymentEvent event, PaymentStatus paymentStatus) {
		String key = event.getBookingId().toString();
		String paymentTopic = PaymentStatus.APPROVED.equals(paymentStatus) ?
												paymentApprovedTopic : paymentFailedTopic;
		log.info("Publicando evento PaymentEvent para bookingId={} en tópico={}", key, paymentTopic);

		CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(paymentTopic, key, event);

		future.whenComplete((result, ex) -> {
			if (ex != null) {
				log.error("Error al publicar PaymentEvent para bookingId={} en tópico={}: {}", key, paymentTopic, ex.getMessage(), ex);
			} else {
				log.info("PaymentEvent publicado con éxito. Tópico={}, Partición={}, Offset={}",
						result.getRecordMetadata().topic(),
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
			}
		});
	}

}
