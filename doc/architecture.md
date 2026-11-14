# 🏨✈️ ReservaYa — Plataforma de Reservas Event-Driven

## Descripción General

**ReservaYa** es una plataforma de reservas de hoteles y vuelos que procesa reservas de forma asíncrona y orientada a eventos usando **Apache Kafka**.
El sistema está diseñado bajo principios de resiliencia, alta disponibilidad, consistencia eventual (Patrón **Saga Coreografiada**) y desacoplamiento absoluto mediante **Proyecciones Locales de Datos**.

---

## Stack Tecnológico Global

| Tecnología | Rol |
|---|---|
| **Java 27 / Spring Boot 3.x / 4.x** | Framework base de cada microservicio |
| **Spring Cloud Eureka** | Service Discovery (Registro y descubrimiento de servicios) |
| **Spring Cloud Gateway (MVC)** | API Gateway (Perímetro de seguridad, Fail-Fast JWT, inyección de `X-User-Id`) |
| **Spring Security + JWT** | Autenticación y Autorización stateless |
| **Apache Kafka** | Backbone de mensajería asíncrona orientada a eventos |
| **PostgreSQL** | Persistencia relacional (Database-per-Service) |
| **Redis** | Caché distribuido (Estrategia Cache-Aside con TTL y desalojo atómico) |
| **AWS S3 + Thymeleaf + Spring Mail** | Almacenamiento, renderizado y despacho de emails |
| **Docker & Docker Compose** | Contenerización y orquestación del ecosistema |

---

## Catálogo de Microservicios

```
                                 [ Cliente HTTP ]
                                        │
                                        ▼
                            [ api-gateway (8080) ]
                                 │            │
            ┌────────────────────┘            └────────────────────┐
            ▼                                                      ▼
   [ auth-service (8085) ]                               [ booking-service (8081) ]
            │                                                      │
            │ publica: user.created                                │ publica: booking.created / booking.cancelled
            ▼                                                      ▼
  ═══════════════════════════════ [ APACHE KAFKA ] ══════════════════════════════════
            │                                                      │
            │ consume: user.created, payment.*, booking.*          │ consume: booking.created
            ▼                                                      ▼
[ notification-service (8083) ]                           [ payment-service (8082) ]
                                                                   │
                                                                   │ publica: payment.approved / payment.failed
                                                                   └───────────────────────► (Kafka)
```

| Microservicio | Puerto | Base de Datos | Documentación Detallada |
|---|---|---|---|
| [`api-gateway`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/api-gateway/README.md) | `8080` | N/A | Validador JWT Fail-Fast, inyector de `X-User-Id`, enrutador Eureka |
| [`eureka-server`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/eureka-server/README.md) | `8761` | N/A | Servidor de registro y descubrimiento de instancias |
| [`auth-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/auth-service/README.md) | `8085` | `auth_db` | Registro, login, roles/permisos, JWT con claims de usuario |
| [`booking-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/booking-service/README.md) | `8081` | `booking_db` | Creación y ciclo de vida de reservas, validación de fechas, caché Redis |
| [`payment-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/payment-service/README.md) | `8082` | `payment_db` | Cobro asíncrono, simulación de transacciones, idempotencia |
| [`notification-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/notification-service/README.md) | `8083` | `notification_db` | Strategy Pattern, S3 templates, outbox de pendientes, envío de emails |

---

## Patrones de Arquitectura Clave

### 1. Patrón Saga Coreografiada (Transacciones Distribuidas)
No existe un orquestador centralizado. Los servicios avanzan el flujo de negocio reaccionando autónomamente a los eventos de dominio:

```
[Cliente] 
    │ POST /v1/bookings
    ▼
[booking-service] ──(guarda PENDING)
    │
    │ publica: booking.created
    ▼
[Kafka: booking.created]
    ├──► [payment-service] ──(procesa pago)
    │         ├── Éxito: publica 'payment.approved'
    │         └── Falla:  publica 'payment.failed'
    │
    └──► [notification-service] ──(envía email: booking_created)

[Kafka: payment.approved]
    ├──► [booking-service] ──(actualiza a CONFIRMED + desaloja caché)
    └──► [notification-service] ──(envía email: payment_approved)

[Kafka: payment.failed]
    ├──► [booking-service] ──(actualiza a PAYMENT_FAILED + desaloja caché)
    └──► [notification-service] ──(envía email: payment_failed)
```

### 2. Proyección Local de Datos (Local Data Projection)
Para evitar que `notification-service` consulte por HTTP a `auth-service` cada vez que necesita enviar un correo:
- Al registrarse un usuario, `auth-service` publica `user.created`.
- `notification-service` consume el evento y almacena localmente una copia en su tabla `user_contacts` y en Redis.
- **Ventaja:** Si `auth-service` está caído, las notificaciones se siguen enviando sin interrupciones.

### 3. Notificaciones Diferidas (Outbox / Deferred Pattern)
- Si un evento de pago o reserva llega antes que el evento de usuario nuevo (`user.created`), el sistema detecta que el contacto no existe aún.
- Guarda la notificación con estado `PENDING` y el JSON del evento en `payload`.
- Al llegar el evento `user.created`, recupera las pendientes y las despacha automáticamente (`SENT`).

### 4. Caché Distribuido (Cache-Aside con Redis)
- Implementado en `booking-service` y `notification-service`.
- Lectura: consulta primero Redis; ante un miss, consulta PostgreSQL y actualiza la caché con TTL.
- Escritura / Compensación: desaloja atómicamente las claves de caché (`@CacheEvict`) para evitar inconsistencias de datos.
