# 🌐 API Gateway — ReservaYa

Punto de entrada único (Edge Service / Reverse Proxy) para el ecosistema **ReservaYa**, construido con **Spring Cloud Gateway MVC**, **Spring Security** y **Resilience4j**.

---

## 🎯 Responsabilidades Principales

1. **Perímetro de Seguridad (Fail-Fast JWT):**
   - Intercepta todas las peticiones externas dirigidas a rutas protegidas.
   - Valida la firma HMAC-SHA256 y vigencia del JWT emitido por `auth-service`.
   - Si el token es inválido o expiró, responde de inmediato con `401 Unauthorized` sin transferir carga a los servicios downstream.

2. **Propagación de Identidad (`X-User-Id`):**
   - Extrae el claim `userId` del JWT verificado.
   - Inyecta de forma transparente el encabezado interno `X-User-Id: <id>` hacia los microservicios downstream (`booking-service`, `notification-service`).
   - Desacopla la lógica de autenticación de los microservicios internos.

3. **Resiliencia y Rate Limiting (Resilience4j):**
   - **Rate Limiter por IP:** Restricción de 3 peticiones cada 10 segundos por dirección IP (`ip-<client_ip>`), respondiendo con `429 Too Many Requests` ante ráfagas abusivas.
   - **Circuit Breaker:** Monitorea la tasa de fallos hacia los microservicios con apertura de circuito automática (`failure-rate-threshold: 50%`, `wait-duration: 5s`).
   - **Time Limiter:** Timeout estricto de 5 segundos (`globalTimeLimiter`) para evitar saturación por servicios lentos.

4. **Service Discovery Dinámico:**
   - Resuelve las instancias activas mediante **Netflix Eureka** con balanceo de carga en cliente (`lb("AUTH-SERVICE")`, `lb("BOOKING-SERVICE")`, etc.).

---

## ⚙️ Configuración (`application.yml`)

Todas las propiedades de resiliencia y de red están parametrizadas con variables de entorno.

| Propiedad / Variable de Entorno | Valor por Defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto HTTP del API Gateway |
| `SPRING_THREADS_VIRTUAL_ENABLED` | `true` | Habilita Virtual Threads (Java 27 / Loom) |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka` | URL del registro Eureka |
| `RESILIENCE4J_RATELIMITER_LIMIT_FOR_PERIOD` | `3` | Peticiones permitidas en la ventana |
| `RESILIENCE4J_RATELIMITER_LIMIT_REFRESH_PERIOD` | `10s` | Ventana de refresco del Rate Limiter |
| `RESILIENCE4J_RATELIMITER_TIMEOUT_DURATION` | `0s` | Espera máxima para adquirir permiso |
| `RESILIENCE4J_TIMELIMITER_TIMEOUT_DURATION` | `5s` | Timeout de ejecución antes de cancelar |
| `RESILIENCE4J_TIMELIMITER_CANCEL_RUNNING_FUTURE` | `true` | Cancela el future en caso de timeout |
| `RESILIENCE4J_CIRCUITBREAKER_SLIDING_WINDOW_SIZE` | `10` | Ventana deslizante de peticiones a evaluar |
| `RESILIENCE4J_CIRCUITBREAKER_FAILURE_RATE_THRESHOLD` | `50` | Porcentaje de fallos (%) para abrir circuito |
| `RESILIENCE4J_CIRCUITBREAKER_WAIT_DURATION_IN_OPEN_STATE` | `5s` | Tiempo en estado abierto antes de half-open |
| `LOG_LEVEL_GATEWAY` | `TRACE` | Nivel de logs para enrutamiento Gateway |
| `LOG_LEVEL_WEB` | `INFO` | Nivel de logs para peticiones Web |

* **Puerto:** `8080`
* **Eureka Service URL:** `http://localhost:8761/eureka`
* **Prefijo Global de Acceso:** `/api` (removido hacia los microservicios mediante `stripPrefix(1)`)

---

## 🔀 Enrutamiento Declarativo en application.yml

`yaml
spring:
  cloud:
    gateway:
      mvc:
        routes:
          - id: auth-service
            uri: lb://AUTH-SERVICE
            predicates:
              - Path=/api/auth/**
            filters:
              - StripPrefix=1
              - CircuitBreaker=globalTimeLimiter

          - id: booking-service
            uri: lb://BOOKING-SERVICE
            predicates:
              - Path=/api/v1/bookings/**
            filters:
              - StripPrefix=1
              - CircuitBreaker=globalTimeLimiter

          - id: payment-service
            uri: lb://PAYMENT-SERVICE
            predicates:
              - Path=/api/v1/payments/**
            filters:
              - StripPrefix=1
              - CircuitBreaker=globalTimeLimiter

          - id: notification-service
            uri: lb://NOTIFICATION-SERVICE
            predicates:
              - Path=/api/v1/notifications/**
            filters:
              - StripPrefix=1
              - CircuitBreaker=globalTimeLimiter
`

## 🔀 Rutas y Reglas de Enrutamiento

| Ruta Externa | Microservicio Destino | Ruta Interna Reescrita | Autenticación | Header Inyectado | Filtros Resiliencia |
|---|---|---|---|---|---|
| `/api/auth/**` | `AUTH-SERVICE` | `/auth/**` | Pública | Ninguno | RateLimiter + CircuitBreaker |
| `/api/v1/bookings/**` | `BOOKING-SERVICE` | `/v1/bookings/**` | **Requerida (Bearer JWT)** | `X-User-Id` | RateLimiter + CircuitBreaker + Auth |
| `/api/v1/payments/**` | `PAYMENT-SERVICE` | `/v1/payments/**` | **Requerida (Bearer JWT)** | Ninguno | RateLimiter + CircuitBreaker |
| `/api/v1/notifications/**` | `NOTIFICATION-SERVICE` | `/v1/notifications/**` | **Requerida (Bearer JWT)** | `X-User-Id` | RateLimiter + CircuitBreaker + Auth |

---

## 📡 Ejemplos de Invocación con cURL

### 1. Registrar Usuario
```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "lucas_dev",
    "password": "123456*",
    "email": "lucas@example.com",
    "phone": "+5491123456789"
  }'
```

### 2. Iniciar Sesión (Obtener JWT)
```bash
curl -X POST "http://localhost:8080/api/auth" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "lucas_dev",
    "password": "123456*"
  }'
```
*Respuesta:*
```json
{
  "username": "lucas_dev",
  "message": "User logged successfully",
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "status": true
}
```

### 3. Crear Reserva (Requiere Bearer Token)
```bash
curl -X POST "http://localhost:8080/api/v1/bookings" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "HOTEL",
    "resourceId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "checkIn": "2026-12-01T14:00:00",
    "checkOut": "2026-12-05T10:00:00",
    "totalAmount": 450.00
  }'
```

### 4. Consultar Notificaciones del Usuario
```bash
curl -X GET "http://localhost:8080/api/v1/notifications" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 4.1.2**
- **Virtual Threads habilitados (spring.threads.virtual.enabled: true)**
- **Spring Cloud Gateway Server MVC**
- **Spring Cloud Netflix Eureka Client**
- **Spring Security 6.x & Auth0 Java-JWT**
- **Resilience4j (RateLimiter, CircuitBreaker, TimeLimiter)**
