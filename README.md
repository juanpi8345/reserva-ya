# 🏨 ReservaYa — Plataforma de Reservas Distribuida

Plataforma escalable de reservas de hoteles y vuelos basada en **arquitectura de microservicios orientada a eventos (Event-Driven)** con **Apache Kafka**, **Spring Cloud**, **PostgreSQL**, **Redis**, **AWS S3** y **Resilience4j**.

---

## 🏛️ Arquitectura Global del Sistema

El ecosistema implementa una **Saga Coreografiada** asíncrona sobre Apache Kafka para garantizar coherencia eventual entre servicios sin acoplamiento punto a punto.

```text
                                [ CLIENTES / FRONTEND ]
                                           │
                                           ▼ (HTTP / REST)
                        ┌──────────────────────────────────────┐
                        │          API GATEWAY (:8080)         │
                        │  - JWT Fail-Fast Validator           │
                        │  - Header Injector (X-User-Id)       │
                        │  - Resilience4j (Rate Limit / CB)    │
                        └──────────────────┬───────────────────┘
                                           │ (Eureka Service Discovery :8761)
          ┌────────────────────────────────┼────────────────────────────────┐
          ▼                                ▼                                ▼
┌──────────────────┐             ┌──────────────────┐             ┌──────────────────┐
│   AUTH-SERVICE   │             │ BOOKING-SERVICE  │             │ PAYMENT-SERVICE  │
│      (:8085)     │             │      (:8081)     │             │      (:8082)     │
└─────────┬────────┘             └────────┬─────────┘             └────────▲─────────┘
          │ (user.created)                │ (booking.created)              │ (Consumes)
          ▼                               ▼                                │
  ┌────────────────────────────────────────────────────────────────────────┴─────────┐
  │                              APACHE KAFKA CLUSTER                                │
  │  Topics: user.created | booking.created | booking.cancelled                      │
  │          payment.approved | payment.failed                                       │
  └───────────────────────────────────────┬──────────────────────────────────────────┘
                                          │
                                          ▼ (Consumes all domain events)
                               ┌──────────────────────┐
                               │ NOTIFICATION-SERVICE │
                               │       (:8083)        │
                               │  - Strategy Pattern  │
                               │  - Templates en S3   │
                               │  - Caché Redis       │
                               └──────────────────────┘
```

---

## ⚡ Guía de Inicio Rápido (Puertos y Servicios)

| Servicio | Puerto | Base de Datos / Storage | Descripción |
|---|---|---|---|
| **[`api-gateway`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/api-gateway/README.md)** | `8080` | Memoria (Resilience4j) | Reverse Proxy, Rate Limiter, validación JWT e inyección de `X-User-Id` |
| **[`eureka-server`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/eureka-server/README.md)** | `8761` | Memoria | Dashboard y Registro dinámico de microservicios |
| **[`auth-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/auth-service/README.md)** | `8085` | PostgreSQL (`auth_db`) | Registro de usuarios, login, JWT y evento `user.created` |
| **[`booking-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/booking-service/README.md)** | `8081` | PostgreSQL (`booking_db`) + Redis | Core de reservas, validación de fechas y Saga `booking.created` |
| **[`payment-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/payment-service/README.md)** | `8082` | PostgreSQL (`payment_db`) | Cobro asíncrono, eventos `payment.approved` / `payment.failed` |
| **[`notification-service`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/notification-service/README.md)** | `8083` | PostgreSQL (`notification_db`) + Redis + S3 | Patrón Strategy, templates S3 en caché y auditoría de emails |

---

## 🏗️ Topología y Arquitectura de Kafka

La plataforma incluye diagramas Draw.io exhaustivos para entornos de desarrollo y producción:

* **Desarrollo:** [`doc/Kafka development architecture.drawio`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/doc/Kafka%20development%20architecture.drawio) (3 particiones por tópico, productores y consumer groups).
* **Producción (High Availability):** [`doc/Kafka production architecture .drawio`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/doc/Kafka%20production%20architecture%20.drawio):
  - **Cluster Multi-Broker:** 3 Brokers con **Replication Factor (RF) = 3**.
  - **Balanceo Leader/ISR:** Cero concentración de carga; réplicas en sincronía distribuidas uniformemente.
  - **Garantía Zero Data Loss:** `min.insync.replicas = 2` y productores con `acks = all` (`-1`).
  - **Escalabilidad Horizontal:** 5 particiones con 5 consumidores concurrentes en tópicos críticos (`booking.created`, `payment.approved`, `payment.failed`).
* **Arquitectura de Alto Nivel:** [`doc/Diagrama alto nivel.drawio`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/doc/Diagrama%20alto%20nivel.drawio).
* **Especificación de la Saga:** [`doc/arquitectura.md`](file:///c:/Users/SistemaxPC/Desktop/Proyectos/reservaYa/doc/arquitectura.md).

---

## 📡 Referencia Rápida de Endpoints (vía API Gateway `:8080`)

### 1. Autenticación (`auth-service`)
* `POST http://localhost:8080/api/auth/register` — Registro de usuario
* `POST http://localhost:8080/api/auth` — Login y obtención de token JWT

### 2. Reservas (`booking-service` - Requiere `Bearer <token>`)
* `POST http://localhost:8080/api/v1/bookings` — Inicia creación de reserva (`202 ACCEPTED`)
* `GET http://localhost:8080/api/v1/bookings` — Lista todas las reservas
* `GET http://localhost:8080/api/v1/bookings/user` — Reservas del usuario autenticado
* `GET http://localhost:8080/api/v1/bookings/{id}` — Detalle de una reserva
* `DELETE http://localhost:8080/api/v1/bookings/{id}` — Cancela reserva y dispara compensación

### 3. Pagos (`payment-service` - Requiere `Bearer <token>`)
* `GET http://localhost:8080/api/v1/payments` — Auditoría general de transacciones
* `GET http://localhost:8080/api/v1/payments/{bookingId}` — Pago de una reserva puntual

### 4. Notificaciones (`notification-service` - Requiere `Bearer <token>`)
* `GET http://localhost:8080/api/v1/notifications` — Auditoría de notificaciones del usuario
# reserva-ya
