package com.reservaya.notification_service.event.model;

import java.util.UUID;

import com.reservaya.notification_service.model.NotificationContext;
import com.reservaya.notification_service.model.PaymentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentEvent extends NotificationContext{
    private UUID bookingId;
    private PaymentStatus paymentStatus;
}
