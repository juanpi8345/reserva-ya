package com.reservaya.payment_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

public record GetPaymentResponse (UUID paymentId,
								  UUID bookingId,
								  Long userId,
								  BigDecimal amount,
								  PaymentStatus paymentStatus,
								  @JsonSerialize(using = LocalDateTimeSerializer.class)
								  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
								  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
								  LocalDateTime proccessedAt) {

	
}
