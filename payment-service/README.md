# 💳 Payment Service — ReservaYa

Microservicio encargado del cobro y simulación de pagos asíncronos en la arquitectura orientada a eventos de **ReservaYa**.

---

## 🎯 Responsabilidades Principales

1. **Consumo de Solicitudes de Cobro:**
   - Escucha el tópico `booking.created` mediante el grupo consumidor `payment-group`.
2. **Procesamiento Asíncrono de Pagos:**
   - Evalúa el monto y procesa el cobro simulado.
   - Aplica validaciones de negocio: importes válidos resultan en `APPROVED`, mientras que casos borde o rechazos de pasarela derivan en `FAILED`.
3. **Publicación Dinámica de Resultados (Saga Coreografiada):**
   - Si el pago es exitoso, emite `PaymentEvent` (con status `APPROVED`) en el tópico `payment.approved`.
   - Si el pago es rechazado, emite `PaymentEvent` (con status `FAILED`) en el tópico `payment.failed`.
4. **Idempotencia y Auditoría:**
   - Persiste cada transacción en la tabla `payments` asociada al `booking_id` para garantizar no duplicidad de cobros.
5. **Consulta de Pagos:**
   - Expone endpoints REST para auditoría y consulta de estados de cobro.

---

## ⚙️ Configuración (`application.yml`)

* **Puerto:** `8082`
* **Base de Datos:** PostgreSQL (`payment_db` en puerto `5432`)
* **Kafka Topics:**
  - Consume: `booking.created`
  - Emite dinámicamente a: `payment.approved` y `payment.failed`
* **Consumer Group:** `payment-group`
* **Eureka URL:** `http://localhost:8761/eureka`

---

## 🔀 Endpoints Expuestos

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/v1/payments` | Protegido (Bearer JWT) | Listado completo de auditoría de pagos procesados |
| `GET` | `/v1/payments/{bookingId}` | Protegido (Bearer JWT) | Consulta del pago asociado a una reserva específica |

---

## 📡 Ejemplos de Peticiones y Respuestas

### 1. Listar Todos los Pagos (`GET /api/v1/payments`)
**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/payments" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
**Respuesta:** `200 OK`
```json
[
  {
    "id": "e7b1a2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
    "bookingId": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
    "amount": 320.00,
    "status": "APPROVED",
    "createdAt": "2026-11-14T15:15:00"
  }
]
```

### 2. Consultar Pago por Booking ID (`GET /api/v1/payments/{bookingId}`)
**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/payments/c48f293b-18a7-4b68-98e6-d98c362a7b8e" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
**Respuesta:** `200 OK`
```json
{
  "id": "e7b1a2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "bookingId": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
  "amount": 320.00,
  "status": "APPROVED",
  "createdAt": "2026-11-14T15:15:00"
}
```

---

## 📨 Modelo de Evento (`PaymentEvent`)

```json
{
  "bookingId": "c48f293b-18a7-4b68-98e6-d98c362a7b8e",
  "paymentStatus": "APPROVED",
  "userId": 12
}
```

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 3.4.x**
- **Spring Data JPA & PostgreSQL**
- **Spring Kafka (Producer & Consumer)**
- **Netflix Eureka Client**
