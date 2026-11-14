package com.reservaya.notification_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reservaya.notification_service.entity.Notification;
import com.reservaya.notification_service.model.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>{
	boolean existsByIdempotencyKey(String idempotencyKey);
	List<Notification> findAllByUserIdAndStatus(Long userId, NotificationStatus status);
	List<Notification> findAllByUserId(Long userId);
}

