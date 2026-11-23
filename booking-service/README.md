# 🏨 Booking Service — ReservaYa

Microservicio central (Core de Negocio) encargado de la gestión de reservas de hoteles y vuelos, control de solapamiento de fechas, orquestación de la **Saga Coreografiada** y caché de alto rendimiento con **Redis**.

---

## 🎯 Responsabilidades Principales

1. **Gestión de Reservas:**
   - Creación, consulta por ID, listado general y filtro por usuario autenticado.
   - Cancelación de reservas con emisión de eventos de compensación.
2. **Validación de Solapamiento Temporal:**
   - Valida que el recurso solicitado (`HOTEL` o `FLIGHT`) no posea reservas solapadas en el mismo rango de fechas para reservas en estado activo (`PENDING`, `CONFIRMED`).
3. **Inicio de Saga Coreografiada con Transactional Outbox:**
   - Al crearse o cancelarse una reserva, el cambio de estado y el evento se guardan de forma atómica en la base de datos dentro de la tabla `outbox_events`.
   - Un scheduler desacoplado con concurrencia segura (`FOR UPDATE SKIP LOCKED`) procesa los eventos y los emite a Kafka (`booking.created`, `booking.cancelled`), garantizando entrega *at-least-once* incluso ante caídas del broker.
4. **Consumo de Eventos de Pago (Saga Callback):**
   - Escucha los tópicos `payment.approved` y `payment.failed`.
   - Si el pago es aprobado, actualiza el estado de la reserva a `CONFIRMED`.
   - Si el pago falla, actualiza el estado a `CANCELLED` y libera el recurso.

> 📊 **Diagramas de Arquitectura:** Consultá el flujo de Saga e integración con Kafka en [General Architecture Diagram](../doc/General%20achitecture.png) y [Kafka Development Architecture](../doc/Kafka%20development%20architecture.png).
5. **Caché Distribuida con Redis:**
   - Caché en lecturas frecuentes (`@Cacheable(value = "bookings")`) y desalojo automático ante cancelaciones y cambios de estado (`@CacheEvict`).

---

## ⚙️ Configuración (`application.yml`)

* **Puerto:** `8081`
* **Base de Datos:** PostgreSQL (`booking_db` en puerto `5432`)
* **Redis Host/Port:** `localhost:6379`
* **Kafka Topics:**
  - Emite: `booking.created`, `booking.cancelled`
  - Consume: `payment.approved`, `payment.failed`
* **Consumer Group:** `booking-group`
* **Eureka URL:** `http://localhost:8761/eureka`

---

## 🔀 Endpoints Expuestos

| Método | Endpoint | Headers Requeridos | Descripción |
|---|---|---|---|
| `POST` | `/v1/bookings` | `X-User-Id` *(inyectado por Gateway)* | Inicia la creación de reserva y emite evento a Kafka (`202 ACCEPTED`) |
| `GET` | `/v1/bookings` | Ninguno | Listado global de todas las reservas |
| `GET` | `/v1/bookings/{id}` | `X-User-Id` | Consulta detalle de reserva por UUID |
| `GET` | `/v1/bookings/user` | `X-User-Id` | Lista todas las reservas del usuario autenticado |
| `DELETE` | `/v1/bookings/{id}` | `X-User-Id` | Cancela la reserva y emite `BookingCancelledEvent` (`204 NO_CONTENT`) |

---

## 📡 Endpoints y Ejemplos

> Todos los endpoints requieren `Authorization: Bearer <jwt>`.
> El header `X-User-Id` es inyectado automáticamente por el API Gateway — no enviarlo manualmente.

---

### `POST /api/v1/bookings` — Crear reserva

Publica el evento `booking.created` en Kafka y persiste la reserva en estado `PENDING`. **Respuesta asíncrona: `202 ACCEPTED`.**

**Request Body:**

| Campo | Tipo | Validaciones |
|---|---|---|
| `resourceType` | `Enum` | Requerido. `HOTEL` \| `FLIGHT` |
| `resourceId` | `UUID` | Requerido. ID del hotel o vuelo |
| `checkIn` | `LocalDateTime` | Requerido. Formato `yyyy-MM-dd'T'HH:mm:ss`. Hoy o futuro |
| `checkOut` | `LocalDateTime` | Requerido. Formato `yyyy-MM-dd'T'HH:mm:ss`. Debe ser futuro |
| `totalAmount` | `BigDecimal` | Requerido. Mínimo `0.01`, máximo 10 enteros y 2 decimales |

```bash
curl -s -X POST "http://localhost:8080/api/v1/bookings" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "HOTEL",
    "resourceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "checkIn": "2026-12-01T14:00:00",
    "checkOut": "2026-12-05T10:00:00",
    "totalAmount": 320.00
  }'
```

**Respuesta `202 ACCEPTED`:**
```json
{
  "bookingId": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
  "status": "PENDING"
}
```

---

### `GET /api/v1/bookings` — Listar todas las reservas

```bash
curl -s "http://localhost:8080/api/v1/bookings" \
  -H "Authorization: Bearer <jwt>"
```

**Respuesta `200 OK`:**
```json
[
  {
    "id": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
    "userId": 1,
    "resourceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "resourceType": "HOTEL",
    "checkIn": "2026-12-01T14:00:00",
    "checkOut": "2026-12-05T10:00:00",
    "totalAmount": 320.00,
    "status": "CONFIRMED",
    "createdAt": "2026-11-20T09:30:00",
    "updatedAt": "2026-11-20T09:31:05"
  }
]
```

#### Estados posibles (`status`)

| Valor | Descripción |
|---|---|
| `PENDING` | Reserva publicada en Kafka, esperando resultado del pago |
| `CONFIRMED` | Pago aprobado (`payment.approved` consumido) |
| `PAYMENT_FAILED` | Pago rechazado (`payment.failed` consumido) |
| `CANCELLED` | Cancelada manualmente por el usuario |

---

### `GET /api/v1/bookings/user` — Reservas del usuario autenticado

Filtra por `X-User-Id` inyectado por el Gateway.

```bash
curl -s "http://localhost:8080/api/v1/bookings/user" \
  -H "Authorization: Bearer <jwt>"
```

Mismo formato de respuesta que `GET /api/v1/bookings`.

---

### `GET /api/v1/bookings/{id}` — Detalle de una reserva

```bash
curl -s "http://localhost:8080/api/v1/bookings/c48f293b-18a7-4b68-98e6-d98c362a7b8e" \
  -H "Authorization: Bearer <jwt>"
```

Mismo formato de objeto de respuesta que el array anterior.

---

### `DELETE /api/v1/bookings/{id}` — Cancelar reserva

Publica el evento `booking.cancelled` en Kafka. Respuesta `204 NO CONTENT`.

```bash
curl -s -X DELETE "http://localhost:8080/api/v1/bookings/c48f293b-18a7-4b68-98e6-d98c362a7b8e" \
  -H "Authorization: Bearer <jwt>"
```

| Código | Descripción |
|---|---|
| `204 No Content` | Evento de cancelación publicado — compensación asíncrona en curso |

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 4.1.2**
- **Spring Data JPA & PostgreSQL**
- **Spring Data Redis (Caché)**
- **Spring Kafka (Producer & Consumer)**
- **Netflix Eureka Client**
