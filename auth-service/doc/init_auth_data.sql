-- ====================================================================
-- Script DDL & DML: Creación de Tablas e Inicialización de Datos
-- Microservicio: auth-service
-- Base de datos: PostgreSQL
-- ====================================================================

-- --------------------------------------------------------------------
-- 1. CREACIÓN DE TABLAS (DDL)
-- --------------------------------------------------------------------

-- Tabla de Permisos
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_name VARCHAR(255) NOT NULL UNIQUE
);

-- Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(255) NOT NULL UNIQUE
);

-- Tabla Intermedia: Roles <-> Permisos (Many-to-Many)
CREATE TABLE IF NOT EXISTS roles_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_roles_permissions_role 
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_roles_permissions_permission 
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- Tabla de Usuarios
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

-- --------------------------------------------------------------------
-- 2. CARGA DE DATOS INICIALES (DML)
-- --------------------------------------------------------------------

-- Permisos (READ, SAVE, UPDATE, DELETE)
INSERT INTO permissions (permission_name) 
VALUES 
    ('READ'),
    ('SAVE'),
    ('UPDATE'),
    ('DELETE')
ON CONFLICT (permission_name) DO NOTHING;

-- Roles (ADMIN, USER)
INSERT INTO roles (role) 
VALUES 
    ('ADMIN'),
    ('USER')
ON CONFLICT (role) DO NOTHING;

-- Asignación: ADMIN tiene TODOS los permisos
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.role = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Asignación: USER tiene permisos de lectura y creación (READ, SAVE)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.role = 'USER' AND p.permission_name IN ('READ', 'SAVE')
ON CONFLICT DO NOTHING;
