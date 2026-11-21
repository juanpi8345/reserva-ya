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

## 📡 Endpoints y Ejemplos

### `POST /api/auth/register` — Registrar usuario

**No requiere JWT.**

**Request Body:**

| Campo | Tipo | Validaciones |
|---|---|---|
| `username` | `String` | Requerido, 3–30 caracteres |
| `password` | `String` | Requerido, 8–72 caracteres |
| `email` | `String` | Requerido, formato email válido |
| `phone` | `String` | Requerido, formato `^\\+?[0-9]{8,15}$` |

```bash
curl -s -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juan_perez",
    "password": "MiPassword1*",
    "email": "juan@gmail.com",
    "phone": "+5491112345678"
  }'
```

| Código | Body | Descripción |
|---|---|---|
| `200 OK` | *(vacío)* | Usuario registrado. Se publica `user.created` en Kafka |
| `400 Bad Request` | `{"status":"BAD_REQUEST","message":"..."}` | Validación fallida |
| `409 Conflict` | `{"status":"CONFLICT","message":"..."}` | Username ya registrado |

---

### `POST /api/auth` — Login (obtener JWT)

**No requiere JWT.**

```bash
curl -s -X POST "http://localhost:8080/api/auth" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

**Respuesta `200 OK`:**
```json
{
  "username": "admin",
  "message": "Login successful",
  "jwt": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJhdXRoLXNlcnZpY2UiLCJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwiaWF0IjoxNzAwMDAwMDAwfQ...."
}
```

| Campo | Descripción |
|---|---|
| `username` | Usuario autenticado |
| `message` | Siempre `"Login successful"` |
| `jwt` | Token RS256 firmado — usarlo como `Authorization: Bearer <jwt>` |

| Código | Body | Descripción |
|---|---|---|
| `200 OK` | Ver arriba | Login exitoso |
| `401 Unauthorized` | `{"status":"UNAUTHORIZED","message":"Invalid username or password"}` | Credenciales incorrectas |

#### Usuarios pre-cargados en la base de datos

| Username | Password | Rol |
|---|---|---|
| `admin` | `123456` | `ADMIN` |
| `usuario` | `123456` | `USER` |

---

## 📨 Eventos Kafka Producidos

* **Tópico:** `user.created`
* **Clave de particionado:** `userId`
* **Payload:**
```json
{
  "userId": 1,
  "phone": "+5491112345678",
  "email": "juan@gmail.com"
}
```

---

## 🛠️ Stack Tecnológico
- **Java 27 / Spring Boot 4.1.1**
- **Spring Data JPA & PostgreSQL Driver**
- **Spring Security 6.x (BCrypt, Stateless)**
- **Auth0 Java-JWT**
- **Spring Kafka (Producer)**
- **Netflix Eureka Client**
