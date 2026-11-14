package com.reservaya.notification_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reservaya.notification_service.model.GetNotificationResponse;
import com.reservaya.notification_service.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController {
	
	private final NotificationService notificationService;
	
	@GetMapping
	public ResponseEntity<List<GetNotificationResponse>> getNotifications(@RequestHeader("X-User-Id") Long userId){
		return ResponseEntity.ok(notificationService.getNotifications(userId));
	}

}
