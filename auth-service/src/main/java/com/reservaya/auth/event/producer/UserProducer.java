package com.reservaya.auth.event.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.reservaya.auth.event.model.UserCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProducer {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${app.kafka.topics.user-created.name:user.created}")
	private String userCreatedTopicName;

	public CompletableFuture<SendResult<String, Object>> sendUserCreatedEvent(UserCreatedEvent event) {
		String key = event.userId().toString();
		
		log.info("Publicando evento UserCreatedEvent para userId={} en tópico={}", key, userCreatedTopicName);

		return kafkaTemplate.send(userCreatedTopicName, key, event);
	}

}
