# 🏨 Booking Service — ReservaYa

Microservicio central (Core de Negocio) encargado de la gestión de reservas de hoteles y vuelos, control de solapamiento de fechas, orquestación de la **Saga Coreografiada** y caché de alto rendimiento con **Redis**.

---

## 🎯 Responsabilidades Principales

1. **Gestión de Reservas:**
   - Creación, consulta por ID, listado general y filtro por usuario autenticado.
   - Cancelación de reservas con emisión de eventos de compensación.
2. **Validación de Solapamiento Temporal:**
   - Valida que el recurso solicitado (`HOTEL` o `FLIGHT`) no posea reservas solapadas en el mismo rango de fechas para reservas en estado activo (`PENDING`, `CONFIRMED`).
3. **Inicio de Saga Coreografiada:**
   - Al crearse una reserva, se persiste con estado `PENDING` y se emite de inmediato el evento `BookingCreatedEvent` hacia Kafka en el tópico `booking.created`.
4. **Consumo de Eventos de Pago (Saga Callback):**
   - Escucha los tópicos `payment.approved` y `payment.failed`.
   - Si el pago es aprobado, actualiza el estado de la reserva a `CONFIRMED`.
   - Si el pago falla, actualiza el estado a `CANCELLED` y libera el recurso.
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

## 📡 Ejemplos de Peticiones y Respuestas

### 1. Crear Reserva (`POST /api/v1/bookings`)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/bookings" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "HOTEL",
    "resourceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "checkIn": "2026-12-01T14:00:00",
    "checkOut": "2026-12-05T10:00:00",
    "totalAmount": 320.00
  }'
```
**Respuesta:** `202 ACCEPTED`
```json
{
  "bookingId": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
  "status": "PENDING",
  "message": "Booking request received. Processing payment asynchronously."
}
```

### 2. Consultar Reservas del Usuario (`GET /api/v1/bookings/user`)
**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/bookings/user" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
**Respuesta:** `200 OK`
```json
[
  {
    "id": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
    "userId": 12,
    "resourceType": "HOTEL",
    "resourceId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "checkIn": "2026-12-01T14:00:00",
    "checkOut": "2026-12-05T10:00:00",
    "totalAmount": 320.00,
    "status": "CONFIRMED"
  }
]
```

### 3. Cancelar Reserva (`DELETE /api/v1/bookings/{id}`)
**Request:**
```bash
curl -X DELETE "http://localhost:8080/api/v1/bookings/c48f293b-18a7-4b68-98e6-d98c362a7b8e" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
**Respuesta:** `204 NO CONTENT`

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 3.4.x**
- **Spring Data JPA & PostgreSQL**
- **Spring Data Redis (Caché)**
- **Spring Kafka (Producer & Consumer)**
- **Netflix Eureka Client**
