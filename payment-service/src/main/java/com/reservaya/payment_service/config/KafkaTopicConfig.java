package com.reservaya.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Value("${app.kafka.topics.payment-approved.name:payment.approved}")
	private String paymentApprovedTopicName;

	@Value("${app.kafka.topics.payment-approved.partitions:3}")
	private int paymentApprovedTopicPartitions;

	@Value("${app.kafka.topics.payment-approved.replicas:1}")
	private int paymentApprovedTopicReplicas;

	@Value("${app.kafka.topics.payment-failed.name:payment.failed}")
	private String paymentFailedTopicName;

	@Value("${app.kafka.topics.payment-failed.partitions:3}")
	private int paymentFailedTopicPartitions;

	@Value("${app.kafka.topics.payment-failed.replicas:1}")
	private int paymentFailedTopicReplicas;

	@Bean
	public NewTopic paymentApprovedTopic() {
		return TopicBuilder.name(paymentApprovedTopicName)
				.partitions(paymentApprovedTopicPartitions)
				.replicas(paymentApprovedTopicReplicas)
				.build();
	}

	@Bean
	public NewTopic paymentFailedTopic() {
		return TopicBuilder.name(paymentFailedTopicName)
				.partitions(paymentFailedTopicPartitions)
				.replicas(paymentFailedTopicReplicas)
				.build();
	}
}
