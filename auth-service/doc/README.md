# 🔐 Auth Service — Documentación y Scripts de Base de Datos

Microservicio encargado de la autenticación, registro de usuarios y generación de tokens JWT en el ecosistema **ReservaYa**.

---

## 🗄️ Modelo de Datos

El servicio utiliza **PostgreSQL** con las siguientes tablas:

* **`permissions` (`Permission`)**: Contiene las operaciones del sistema (`READ`, `SAVE`, `UPDATE`, `DELETE`).
* **`roles` (`Role`)**: Contiene los roles del sistema (`ADMIN`, `USER`).
* **`roles_permissions`**: Tabla intermedia que vincula los permisos de cada rol (Many-to-Many).
* **`users` (`UserSec`)**: Almacena credenciales, estado de la cuenta (`enabled`, `account_non_locked`, etc.) y referencia al rol (`role_id`).

---

## 📜 Script Completo (Creación de Tablas + Carga de Datos)

El archivo SQL completo se encuentra en:  
[`auth-service/doc/init_auth_data.sql`](init_auth_data.sql)

```sql
-- ====================================================================
-- 1. CREACIÓN DE TABLAS (DDL)
-- ====================================================================

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS roles_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_roles_permissions_role 
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_permissions_permission 
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credential_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    role_id BIGINT,
    CONSTRAINT fk_users_role 
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE SET NULL
);

-- ====================================================================
-- 2. CARGA DE DATOS INICIALES (DML)
-- ====================================================================

-- 1. Permisos
INSERT INTO permissions (permission_name) 
VALUES 
    ('READ'),
    ('SAVE'),
    ('UPDATE'),
    ('DELETE')
ON CONFLICT (permission_name) DO NOTHING;

-- 2. Roles
INSERT INTO roles (role) 
VALUES 
    ('ADMIN'),
    ('USER')
ON CONFLICT (role) DO NOTHING;

-- 3. Asignación Roles <-> Permisos
-- ADMIN: Todos los permisos (READ, SAVE, UPDATE, DELETE)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.role = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER: Permisos de lectura y creación (READ, SAVE)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.role = 'USER' AND p.permission_name IN ('READ', 'SAVE')
ON CONFLICT DO NOTHING;
```

---

## 💡 ¿Cómo ejecutar los scripts?

1. **Vía DBeaver / pgAdmin:** Conectarse a la base de datos `auth_db` y ejecutar el archivo `init_auth_data.sql`.
2. **Vía terminal psql (Docker o local):**
   ```bash
   psql -U postgres -d auth_db -f doc/init_auth_data.sql
   ```
3. **Automático al iniciar Spring Boot:**  
   Podés copiar este contenido en `src/main/resources/schema.sql` y `src/main/resources/data.sql` si configurás `spring.sql.init.mode=always`.

---

## 🚀 Endpoints Principales

### `POST /auth/register`
Registra un nuevo usuario con rol por defecto `USER`.
* **Body:**
  ```json
  {
    "username": "juanperez",
    "password": "Password123!"
  }
  ```

### `POST /auth`
Inicia sesión y genera el token JWT.
* **Body:**
  ```json
  {
    "username": "juanperez",
    "password": "Password123!"
  }
  ```
* **Respuesta (`200 OK`):**
  ```json
  {
    "username": "juanperez",
    "message": "Login successful",
    "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```

---

## 🎟️ Claims del JWT generado

El token emitido contiene los siguientes claims utilizados por el **API Gateway**:
* `sub`: Nombre de usuario (`username`).
* `userId`: ID numérico del usuario (`Long`), inyectado luego como header `X-User-Id` a los demás microservicios.
* `authorities`: Roles y permisos combinados (ej: `ROLE_USER,READ,SAVE`).
