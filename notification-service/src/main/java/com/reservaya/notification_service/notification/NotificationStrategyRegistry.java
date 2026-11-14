package com.reservaya.notification_service.notification;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.reservaya.notification_service.model.NotificationType;
import com.reservaya.notification_service.notification.strategy.NotificationStrategy;

/**
 * Registry del patrón Strategy.
 * Spring inyecta automáticamente todas las implementaciones de NotificationStrategy.
 * El mapa se construye una sola vez al arrancar la aplicación.
 */
@Component
public class NotificationStrategyRegistry {

    private final Map<NotificationType, NotificationStrategy> strategies;

    public NotificationStrategyRegistry(List<NotificationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        NotificationStrategy::supports,
                        Function.identity()
                ));
    }

    public NotificationStrategy get(NotificationType type) {
        NotificationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No existe una estrategia de notificación para el evento: " + type);
        }
        return strategy;
    }
}
