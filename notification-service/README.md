# 🔔 Notification Service — ReservaYa

Microservicio enfocado en el procesamiento de eventos del dominio, auditoría de notificaciones, almacenamiento y renderizado de plantillas HTML desde **AWS S3** con caché en **Redis**, y despacho de correos electrónicos.

---

## 🎯 Responsabilidades y Patrones de Diseño

1. **Patrón Strategy + Registry para Procesamiento de Eventos:**
   - Desacoplamiento total del procesamiento según el tipo de evento:
     - `BookingCreatedStrategy`: Notificación de reserva creada en espera de pago.
     - `PaymentApprovedStrategy`: Confirmación de pago y reserva asegurada.
     - `PaymentFailedStrategy`: Aviso de rechazo de pago y cancelación de reserva.
     - `BookingCancelledStrategy`: Notificación de cancelación voluntaria o por compensación.
2. **Manejo de Out-of-Order Events (Outbox / Deferred Notifications):**
   - Resuelve condiciones de carrera en Kafka cuando un evento de pago o cancelación llega antes de que el usuario o la reserva estén registrados localmente.
   - Persiste la notificación en estado `PENDING` con su `payload` JSON en la base de datos para procesamiento posterior sin pérdida de mensajes.
3. **Plantillas HTML en AWS S3 con Caché Redis:**
   - Descarga dinámica de plantillas (`welcome.html`, `booking_created.html`, `payment_approved.html`, `payment_failed.html`, `booking_cancelled.html`) desde bucket S3.
   - Caché de alto rendimiento con `@Cacheable(value = "emailTemplates", key = "#templateName")` implementado en `TemplateStorageService` para evitar I/O redundante hacia S3.
4. **Motor de Renderizado Thymeleaf:**
   - Reemplazo dinámico de variables en plantillas en memoria con `SpringTemplateEngine` y `StringTemplateResolver`.

---

## ⚙️ Configuración (`application.yml`)

* **Puerto:** `8083`
* **Base de Datos:** PostgreSQL (`notification_db` en puerto `5432`)
* **Redis:** Caché `emailTemplates` en `localhost:6379`
* **AWS S3:** Bucket `reservaya-email-templates`
* **Kafka Consumer Topics:**
  - `user.created`
  - `booking.created`
  - `booking.cancelled`
  - `payment.approved`
  - `payment.failed`
* **Consumer Group:** `notification-group`
* **Eureka URL:** `http://localhost:8761/eureka`

---

## 🔀 Endpoints Expuestos

| Método | Endpoint | Headers Requeridos | Descripción |
|---|---|---|---|
| `GET` | `/v1/notifications` | `X-User-Id` *(inyectado por Gateway)* | Consulta el historial de notificaciones enviadas y pendientes del usuario |

---

## 📡 Ejemplos de Peticiones y Respuestas

### 1. Consultar Notificaciones del Usuario (`GET /api/v1/notifications`)
**Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/notifications" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
**Respuesta:** `200 OK`
```json
[
  {
    "id": "f8c2b3a1-5d6e-7f8a-9b0c-1d2e3f4a5b6c",
    "recipient": "martin@example.com",
    "subject": "¡Tu pago ha sido aprobado! - ReservaYa",
    "status": "SENT",
    "createdAt": "2026-11-14T15:15:30"
  },
  {
    "id": "a9b1c2d3-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
    "recipient": "martin@example.com",
    "subject": "Solicitud de Reserva Recibida - ReservaYa",
    "status": "SENT",
    "createdAt": "2026-11-14T15:15:02"
  }
]
```

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 3.4.x**
- **Spring Data JPA & PostgreSQL**
- **Spring Data Redis (Caché de templates S3)**
- **AWS SDK v2 (S3 Client)**
- **Thymeleaf Engine**
- **Spring Kafka (Consumer)**
- **JavaMailSender (SMTP / Mock)**
