package com.reservaya.booking_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Value("${app.kafka.topics.booking-created.name}")
	private String bookingCreatedTopicName;

	@Value("${app.kafka.topics.booking-created.partitions}")
	private int bookingCreatedTopicPartitions;

	@Value("${app.kafka.topics.booking-created.replicas}")
	private int bookingCreatedTopicReplicas;

	@Value("${app.kafka.topics.booking-cancelled.name}")
	private String bookingCancelledTopicName;

	@Value("${app.kafka.topics.booking-cancelled.partitions}")
	private int bookingCancelledTopicPartitions;

	@Value("${app.kafka.topics.booking-cancelled.replicas}")
	private int bookingCancelledTopicReplicas;

	@Bean
	public NewTopic bookingCreatedTopic() {
		return TopicBuilder.name(bookingCreatedTopicName)
				.partitions(bookingCreatedTopicPartitions)
				.replicas(bookingCreatedTopicReplicas)
				.build();
	}

	@Bean
	public NewTopic bookingCancelledTopic() {
		return TopicBuilder.name(bookingCancelledTopicName)
				.partitions(bookingCancelledTopicPartitions)
				.replicas(bookingCancelledTopicReplicas)
				.build();
	}
}
