package com.reservaya.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Value("${app.kafka.topics.user-created.name:user.created}")
	private String userCreatedTopicName;

	@Value("${app.kafka.topics.user-created.partitions:3}")
	private int userCreatedTopicPartitions;

	@Value("${app.kafka.topics.user-created.replicas:1}")
	private int userCreatedTopicReplicas;

	@Bean
	public NewTopic paymentApprovedTopic() {
		return TopicBuilder.name(userCreatedTopicName)
				.partitions(userCreatedTopicPartitions)
				.replicas(userCreatedTopicReplicas)
				.build();
	}
}
