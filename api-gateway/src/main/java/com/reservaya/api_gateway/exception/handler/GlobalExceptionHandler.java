package com.reservaya.api_gateway.exception.handler;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({TimeoutException.class, NoFallbackAvailableException.class})
    public ResponseEntity<String> handleTimeout(Exception e) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body("El servicio tardó demasiado en responder (504 Gateway Timeout).");
    }

    @ExceptionHandler({CallNotPermittedException.class, ConnectException.class, ResourceAccessException.class, UnknownHostException.class})
    public ResponseEntity<String> handleServiceDown(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("El servicio de destino no está disponible en este momento (503 Service Unavailable).");
    }
}
