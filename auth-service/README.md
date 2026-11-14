# 🔐 Auth Service — ReservaYa

Microservicio responsable de la autenticación, autorización, gestión de usuarios, cifrado de credenciales y emisión de tokens **JSON Web Tokens (JWT)**.

---

## 🎯 Responsabilidades Principales

1. **Gestión de Identidades:** Registro seguro de usuarios con contraseñas encriptadas mediante `BCryptPasswordEncoder`.
2. **Generación de JWT:** Firma de tokens con claims personalizados:
   - `userId`: Identificador numérico único de la base de datos (clave para auditoría y propagación en microservicios).
   - `authorities`: Roles y permisos del usuario (`ROLE_USER`, `ROLE_ADMIN`).
3. **Integración Asíncrona con Kafka:** Al registrar un nuevo usuario, emite un evento `UserCreatedEvent` al tópico `user.created` para que `notification-service` guarde sus datos de contacto.
4. **Service Discovery:** Registro en **Netflix Eureka** como `AUTH-SERVICE`.

---

## ⚙️ Configuración (`application.yml`)

* **Puerto:** `8085`
* **Base de Datos:** PostgreSQL (`auth_db` en puerto `5432`)
* **Kafka Topic:** `user.created`
* **Eureka URL:** `http://localhost:8761/eureka`

---

## 🔀 Endpoints Expuestos

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/auth/register` | Público | Registra un nuevo usuario y emite `UserCreatedEvent` a Kafka |
| `POST` | `/auth` | Público | Autentica credenciales y retorna el JWT firmado |

---

## 📡 Ejemplos de Peticiones y Respuestas

### 1. Registrar Usuario (`POST /auth/register`)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "martin_perez",
    "password": "SecurePassword123*",
    "email": "martin@example.com",
    "phone": "+5491144445555"
  }'
```
**Respuesta:** `200 OK` (Cuerpo vacío)

### 2. Iniciar Sesión (`POST /auth`)
**Request:**
```bash
curl -X POST "http://localhost:8080/api/auth" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "martin_perez",
    "password": "SecurePassword123*"
  }'
```
**Respuesta:** `200 OK`
```json
{
  "username": "martin_perez",
  "message": "User logged successfully",
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3N1ZXIiOiJhdXRoLXNlcnZpY2UiLCJzdWIiOiJtYXJ0aW5fcGVyZXoiLCJ1c2VySWQiOjEyLCJhdXRob3JpdGllcyI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzg0ODk3NjAwLCJleHAiOjE3ODQ5MDEyMDB9.s8...",
  "status": true
}
```

---

## 📨 Eventos Kafka Producidos

* **Tópico:** `user.created`
* **Clave de particionado:** `userId`
* **Payload:**
```json
{
  "userId": 12,
  "username": "martin_perez",
  "email": "martin@example.com"
}
```

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 3.4.x**
- **Spring Data JPA & PostgreSQL Driver**
- **Spring Security 6.x (BCrypt, Stateless)**
- **Auth0 Java-JWT**
- **Spring Kafka (Producer)**
- **Netflix Eureka Client**
