package com.reservaya.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.reservaya.auth.model.EventType;
import com.reservaya.auth.model.OutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_events")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,name="event_type")
    private EventType eventType;
    
    @Column(nullable = false,name="partition_key")
    private Long partitionKey;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status; 
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(nullable = false,name = "retry_count")
    private int retryCount;
    
	@CreationTimestamp
	@Column(name = "created_at", updatable = false, nullable=false)
	private LocalDateTime createdAt;
	
	@Column(name = "processed_at")
    private LocalDateTime processedAt;
	
	@PrePersist
	private void prePersist() {
		retryCount = 0;
	}
}