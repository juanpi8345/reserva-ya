# 🏨 ReservaYa — Plataforma de Reservas Distribuida

Plataforma escalable de reservas de hoteles y vuelos basada en **arquitectura de microservicios orientada a eventos (Event-Driven)** con **Apache Kafka**, **Spring Cloud**, **PostgreSQL**, **Redis**, **AWS S3** y **Resilience4j**.

---

## 🏛️ Arquitectura Global del Sistema

![Arquitectura General](doc/General%20achitecture.png)

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
| **[`api-gateway`](api-gateway/README.md)** | `8080` | Memoria (Resilience4j) | Reverse Proxy, Rate Limiter, validación JWT e inyección de `X-User-Id` |
| **[`eureka-server`](eureka-server/README.md)** | `8761` | Memoria | Dashboard y Registro dinámico de microservicios |
| **[`auth-service`](auth-service/README.md)** | `8085` | PostgreSQL (`auth_db`) | Registro de usuarios, login, JWT y evento `user.created` |
| **[`booking-service`](booking-service/README.md)** | `8081` | PostgreSQL (`booking_db`) + Redis | Core de reservas, validación de fechas y Saga `booking.created` |
| **[`payment-service`](payment-service/README.md)** | `8082` | PostgreSQL (`payment_db`) | Cobro asíncrono, eventos `payment.approved` / `payment.failed` |
| **[`notification-service`](notification-service/README.md)** | `8083` | PostgreSQL (`notification_db`) + Redis + S3 | Patrón Strategy, templates S3 en caché y envio de emails |

---

## 🏗️ Topología y Arquitectura de Kafka

La plataforma incluye diagramas exhaustivos para entornos de desarrollo y producción:

### Desarrollo (single-broker)

![Kafka Arquitectura Desarrollo](doc/Kafka%20development%20architecture.png)

* **3 particiones por tópico**, productores y consumer groups.
* Archivo editable: [`doc/Kafka development architecture.drawio`](doc/Kafka%20development%20architecture.drawio)

### Producción (High Availability)

![Kafka Arquitectura Producción](doc/Kafka%20production%20architecture.png)

* **Cluster Multi-Broker:** 3 Brokers con **Replication Factor (RF) = 3**.
* **Balanceo Leader/ISR:** Cero concentración de carga; réplicas en sincronía distribuidas uniformemente.
* **Garantía Zero Data Loss:** `min.insync.replicas = 2` y productores con `acks = all` (`-1`).
* **Escalabilidad Horizontal:** 5 particiones con 5 consumidores concurrentes en tópicos críticos (`booking.created`, `payment.approved`, `payment.failed`).
* Archivo editable: [`doc/Kafka production architecture.drawio`](doc/Kafka%20production%20architecture%20.drawio)

**Especificación de la Saga:** [`doc/architecture.md`](doc/architecture.md).

---

## 📡 Referencia Rápida de Endpoints (vía API Gateway `:8080`)

> La documentación detallada de cada endpoint (request/response bodies, validaciones y ejemplos) está en el README de cada microservicio.

### 1. Autenticación (`auth-service`) — Sin JWT → [`auth-service/README.md`](auth-service/README.md)

| Método | Endpoint | Descripción | Response |
|---|---|---|---|
| `POST` | `/api/auth/register` | Registra nuevo usuario | `200 OK` (vacío) |
| `POST` | `/api/auth` | Login — devuelve JWT | `200 OK` + `{username, message, jwt}` |

**Usuarios pre-cargados:** `admin / 123456` (ADMIN), `usuario / 123456` (USER)

### 2. Reservas (`booking-service`) — Requiere JWT → [`booking-service/README.md`](booking-service/README.md)

| Método | Endpoint | Descripción | Response |
|---|---|---|---|
| `POST` | `/api/v1/bookings` | Crea reserva (asíncrono vía Kafka) | `202 ACCEPTED` + `{bookingId, status: PENDING}` |
| `GET` | `/api/v1/bookings` | Lista todas las reservas | `200 OK` + array |
| `GET` | `/api/v1/bookings/user` | Reservas del usuario autenticado | `200 OK` + array |
| `GET` | `/api/v1/bookings/{id}` | Detalle de una reserva | `200 OK` + objeto |
| `DELETE` | `/api/v1/bookings/{id}` | Cancela reserva (asíncrono vía Kafka) | `204 No Content` |

**Tipos:** `resourceType` = `HOTEL` \| `FLIGHT` · `status` = `PENDING` \| `CONFIRMED` \| `PAYMENT_FAILED` \| `CANCELLED`

### 3. Pagos (`payment-service`) — Requiere JWT → [`payment-service/README.md`](payment-service/README.md)

> Los pagos se crean automáticamente al consumir el evento `booking.created` de Kafka. No hay endpoint de creación manual.

| Método | Endpoint | Descripción | Response |
|---|---|---|---|
| `GET` | `/api/v1/payments` | Auditoría general de transacciones | `200 OK` + array |
| `GET` | `/api/v1/payments/{bookingId}` | Pago de una reserva puntual | `200 OK` + objeto |

**Tipos:** `paymentStatus` = `APPROVED` \| `FAILED`

### 4. Notificaciones (`notification-service`) — Requiere JWT → [`notification-service/README.md`](notification-service/README.md)

> Las notificaciones se generan automáticamente por Kafka. Solo lectura (auditoría).

| Método | Endpoint | Descripción | Response |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Notificaciones del usuario autenticado | `200 OK` + array |

**Tipos:** `type` = `BOOKING_CREATED` \| `PAYMENT_APPROVED` \| `PAYMENT_FAILED` \| `BOOKING_CANCELLED` · `status` = `PENDING` \| `SENT` \| `FAILED`

---

## 🐳 Despliegue Local con Docker Compose (Entorno de Pruebas y Desarrollo)

> 💡 **Nota de Arquitectura:** Esta configuración de Docker Compose está diseñada exclusivamente para **desarrollo local, pruebas e integración rápida** en una sola máquina (utiliza un contenedor PostgreSQL unificado con bases lógicas y un Kafka Single-Broker con factor de replicación 1).  
> 
> Para un entorno de **Producción (Cloud / AWS)** se recomienda migrar la infraestructura hacia servicios administrados de Alta Disponibilidad:
> - **Apache Kafka:** **AWS MSK** (Managed Streaming for Apache Kafka) con cluster multi-broker y Replication Factor = 3 (ver topología en [doc/Kafka production architecture.png](doc/Kafka%20production%20architecture.png)).
> - **Bases de Datos:** **AWS RDS para PostgreSQL** (instancias Multi-AZ independientes por microservicio con backups automatizados).
> - **Caché Distribuida:** **AWS ElastiCache para Redis** (con cluster mode / réplicas de lectura).
> - **Orquestación:** Despliegue en **Amazon EKS** o **ECS (Fargate)**.

### Prerequisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y corriendo.
- Maven instalado (o usar el `mvnw` de cada módulo) para compilar los `.jar` antes de construir las imágenes.

---

### Paso 1 — Crear el archivo `.env`

En la raíz del proyecto (`reservaYa/`) creá un archivo llamado `.env` con tus valores reales:

```env
# ─── Base de Datos ─────────────────────────────────────────────
BD_USER=postgres
BD_PASSWORD=tu_password_segura

# ─── JWT ───────────────────────────────────────────────────────
# Generá una clave segura con: openssl rand -base64 32
JWT_KEY=uPr4dQ7B/RFRS9L3V0t20rIIPF8SiVm0r0BIwwvcOIQ=
USER_GENERATOR=auth-service

# ─── Mail (SMTP para Notifications) ───────────────────────────
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=tu-app-password-de-gmail
```

> 💡 **Cómo generar la contraseña de Gmail para SMTP:**  
> Gmail no permite usar tu contraseña habitual. Debes generar una **Contraseña de Aplicación (App Password)** de 16 caracteres:  
> 1. En tu cuenta de Google, ve a *Seguridad* y asegúrate de tener activada la **Verificación en 2 pasos**.  
> 2. Busca **"Contraseñas de aplicaciones"** en el buscador de la cuenta.  
> 3. Escribe un nombre descriptivo (ej: `ReservaYa`) y pulsa **Crear**.  
> 4. Pega la clave de 16 caracteres generada en `MAIL_PASSWORD`.

```env
# ─── AWS S3 (Templates de notificaciones) ─────────────────────
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_S3_BUCKET_NAME=tu_bucket_s3
AWS_REGION=us-east-1
```

> **Nota sobre los templates en S3:** Antes o después de levantar, recordá crear el bucket y cargar las plantillas HTML que utiliza `notification-service` (ubicadas en `notification-service/src/main/resources/email-templates/`):
> ```bash
> aws s3 mb s3://tu_bucket_s3 --region us-east-1
> aws s3 cp notification-service/src/main/resources/email-templates/ s3://tu_bucket_s3/ --recursive
> ```

> **Nunca commitees el `.env` al repositorio.** Ya está en el `.gitignore`.

---

### Paso 2 — Compilar todos los microservicios

Desde la raíz del proyecto, compilá los `.jar` de cada microservicio para que Docker pueda construir las imágenes. En PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-27.0.1.0.3"   # Ajustá a tu JDK

foreach ($service in @("eureka-server", "api-gateway", "auth-service", "booking-service", "payment-service", "notification-service")) {
    Write-Host "Compilando $service..."
    Set-Location $service
    .\mvnw.cmd clean package -DskipTests
    Set-Location ..
}
```

O si preferís hacerlo uno por uno:

```powershell
cd eureka-server      && .\mvnw.cmd clean package -DskipTests && cd ..
cd api-gateway        && .\mvnw.cmd clean package -DskipTests && cd ..
cd auth-service       && .\mvnw.cmd clean package -DskipTests && cd ..
cd booking-service    && .\mvnw.cmd clean package -DskipTests && cd ..
cd payment-service    && .\mvnw.cmd clean package -DskipTests && cd ..
cd notification-service && .\mvnw.cmd clean package -DskipTests && cd ..
```

---

### Paso 3 — Construir las imágenes y levantar

```bash
# Desde la raíz del proyecto (donde está el docker-compose.yml)
docker compose up --build
```

La primera vez Docker va a:
1. Descargar las imágenes base de `postgres:16`, `apache/kafka:4.3.1` y `redis:7`.
2. Construir las imágenes de los 6 microservicios.
3. Crear el volumen `postgres-data` y ejecutar **automáticamente** todos los scripts de `scripts/db/` para inicializar las 4 bases de datos.

Para correr en segundo plano (modo detached):

```bash
docker compose up --build -d
```

---

### Paso 4 — Verificar que todos los servicios levantaron

```bash
docker compose ps
```

Deberías ver algo como:

| NAME | STATUS | PORTS |
|---|---|---|
| `reservaya-postgres-1` | healthy | `5433:5432` |
| `reservaya-kafka-1` | healthy | `9092:9092` |
| `reservaya-redis-1` | healthy | `6379:6379` |
| `reservaya-eureka-server-1` | running | `8761:8761` |
| `reservaya-api-gateway-1` | running | `8080:8080` |
| `reservaya-auth-service-1` | running | `8085:8085` |
| `reservaya-booking-service-1` | running | `8081:8081` |
| `reservaya-payment-service-1` | running | `8082:8082` |
| `reservaya-notification-service-1` | running | `8083:8083` |

Verificá que Eureka registró todos los servicios entrando a: **http://localhost:8761**

---

### Paso 5 — Verificar las bases de datos inicializadas

PostgreSQL está mapeado en el puerto `5433` del host (el `5432` del host puede estar ocupado por una instalación local).

```bash
# Listar las bases creadas
docker exec -it reservaya-postgres-1 psql -U postgres -c "\l"

# Ver los usuarios de auth_db
docker exec -it reservaya-postgres-1 psql -U postgres -d auth_db -c "SELECT id, username, enabled FROM users;"
```

---

### Paso 6 — Probar el sistema con un login

```bash
curl -s -X POST http://localhost:8080/api/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

Respuesta esperada:

```json
{
  "username": "admin",
  "message": "Login successful",
  "jwt": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJhdXRoLXNlcnZpY2UiLCJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwiaWF0IjoxNzAwMDAwMDAwfQ...."
}
```

---

### Comandos útiles del día a día

```bash
# Ver logs de un servicio específico en tiempo real
docker compose logs -f auth-service

# Ver logs de todos los servicios
docker compose logs -f

# Reiniciar un servicio sin reconstruir
docker compose restart auth-service

# Parar todo sin borrar volúmenes (los datos se conservan)
docker compose stop

# Parar y borrar los contenedores (los datos en volúmenes se conservan)
docker compose down

# Parar y borrar TODO incluyendo volúmenes (reset total de base de datos)
docker compose down -v

# Reconstruir una imagen sin tocar el resto
docker compose up --build auth-service
```

---

### Resumen de Puertos en Docker

| Servicio | Puerto Host | Puerto Contenedor |
|---|---|---|
| PostgreSQL | `5433` | `5432` |
| Kafka | `9092` | `9092` |
| Redis | `6379` | `6379` |
| Eureka Dashboard | `8761` | `8761` |
| API Gateway | `8080` | `8080` |
| auth-service | `8085` | `8085` |
| booking-service | `8081` | `8081` |
| payment-service | `8082` | `8082` |
| notification-service | `8083` | `8083` |

> **Nota:** PostgreSQL en Docker usa el puerto `5433` en el host para no colisionar con una instalación local de PostgreSQL que ya use el `5432`.

---

## 🗄️ Scripts de Base de Datos

Los scripts de inicialización están en `scripts/db/` y se ejecutan **automáticamente** cuando el contenedor de Postgres arranca por primera vez (volumen vacío):

| Archivo | Base de datos | Contenido |
|---|---|---|
| `00_init_databases.sh` | — | Crea las 4 bases de datos |
| `01_auth_service.sql` | `auth_db` | Tablas de roles, permisos y usuarios iniciales |
| `02_booking_service.sql` | `booking_db` | Tabla de reservas e índices |
| `03_payment_service.sql` | `payment_db` | Tabla de pagos e índices |
| `04_notification_service.sql` | `notification_db` | Tablas de contactos y notificaciones |

> Si necesitás **resetear las bases de datos**, ejecutá `docker compose down -v` y volvé a levantar con `docker compose up`.

---

### Ejecutar los scripts manualmente (PostgreSQL local)

Si tenés PostgreSQL instalado en tu máquina en lugar de usar Docker:

```powershell
$env:PGPASSWORD = "tu_password"
$psql = "psql" # o la ruta completa si no está en PATH
$scripts = "scripts\db"

# 1. Crear las bases de datos
"auth_db", "booking_db", "payment_db", "notification_db" | ForEach-Object {
    & $psql -U postgres -p 5432 -d postgres -c "CREATE DATABASE $_;"
}

# 2. Ejecutar los scripts de inicialización
& $psql -U postgres -p 5432 -d auth_db         -f "$scripts\01_auth_service.sql"
& $psql -U postgres -p 5432 -d booking_db      -f "$scripts\02_booking_service.sql"
& $psql -U postgres -p 5432 -d payment_db      -f "$scripts\03_payment_service.sql"
& $psql -U postgres -p 5432 -d notification_db -f "$scripts\04_notification_service.sql"
```

---

# reserva-ya
