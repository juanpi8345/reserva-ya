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

> 📊 **Diagramas de Arquitectura:** Consultá la interacción en el flujo asíncrono en [General Architecture Diagram](../doc/General%20achitecture.png) y [Kafka Development Architecture](../doc/Kafka%20development%20architecture.png).
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

## ☁️ Plantillas de Correo en AWS S3

El microservicio descarga dinámicamente los templates HTML desde el bucket configurado en `AWS_S3_BUCKET_NAME` (clave esperada en S3: `<templateName>.html`).

Las plantillas base se encuentran en el proyecto en:
📂 `notification-service/src/main/resources/email-templates/`

| Archivo Template | Clave S3 | Evento que lo dispara |
|---|---|---|
| `booking_created.html` | `booking_created.html` | Al recibir `booking.created` (esperando pago) |
| `payment_approved.html` | `payment_approved.html` | Al recibir `payment.approved` (reserva confirmada) |
| `payment_failed.html` | `payment_failed.html` | Al recibir `payment.failed` (pago rechazado) |
| `booking_cancelled.html` | `booking_cancelled.html` | Al recibir `booking.cancelled` (reserva cancelada) |

### 🚀 Cómo crear el Bucket y subir los templates con AWS CLI

Si estás configurando tu entorno o AWS por primera vez:

```bash
# 1. Crear el bucket en la región deseada (ejemplo: us-east-1)
aws s3 mb s3://tu_bucket_s3 --region us-east-1

# 2. Subir todos los templates HTML desde la carpeta de resources
cd notification-service/src/main/resources/email-templates
aws s3 cp . s3://tu_bucket_s3/ --recursive --exclude "*" --include "*.html"

# 3. Verificar que los archivos quedaron en la raíz del bucket
aws s3 ls s3://tu_bucket_s3/
```

> **En PowerShell:**
> ```powershell
> aws s3 mb s3://tu_bucket_s3 --region us-east-1
> Get-ChildItem .\src\main\resources\email-templates\*.html | ForEach-Object {
>     aws s3 cp $_.FullName "s3://tu_bucket_s3/$($_.Name)"
> }
> ```

---

## 🔀 Endpoints Expuestos

| Método | Endpoint | Headers Requeridos | Descripción |
|---|---|---|---|
| `GET` | `/v1/notifications` | `X-User-Id` *(inyectado por Gateway)* | Consulta el historial de notificaciones enviadas y pendientes del usuario |

---

## 📡 Endpoints y Ejemplos

> Requiere `Authorization: Bearer <jwt>`.
> Las notificaciones se generan automáticamente por Kafka — este endpoint es de **solo lectura** (auditoría).

---

### `GET /api/v1/notifications` — Notificaciones del usuario autenticado

Filtra por `X-User-Id` inyectado por el Gateway.

```bash
curl -s "http://localhost:8080/api/v1/notifications" \
  -H "Authorization: Bearer <jwt>"
```

**Respuesta `200 OK`:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "sentTo": "juan@gmail.com",
    "type": "BOOKING_CREATED",
    "status": "SENT",
    "createdAt": "2026-11-20T09:31:10"
  },
  {
    "id": 2,
    "userId": 1,
    "sentTo": "juan@gmail.com",
    "type": "PAYMENT_APPROVED",
    "status": "SENT",
    "createdAt": "2026-11-20T09:31:15"
  }
]
```

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | ID de la notificación |
| `userId` | `Long` | ID del usuario |
| `sentTo` | `String` | Email o teléfono destino |
| `type` | `Enum` | `BOOKING_CREATED` \| `PAYMENT_APPROVED` \| `PAYMENT_FAILED` \| `BOOKING_CANCELLED` |
| `status` | `Enum` | `PENDING` \| `SENT` \| `FAILED` |
| `createdAt` | `LocalDateTime` | Timestamp de creación |

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 4.1.2**
- **Spring Data JPA & PostgreSQL**
- **Spring Data Redis (Caché de templates S3)**
- **AWS SDK v2 (S3 Client)**
- **Thymeleaf Engine**
- **Spring Kafka (Consumer)**
- **JavaMailSender (SMTP / Mock)**
