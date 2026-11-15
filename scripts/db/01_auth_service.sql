-- ====================================================================
-- DDL & DML: auth-service
-- Base de datos: PostgreSQL (auth_db)
-- ====================================================================

\c auth_db;

-- resto de tu script...
-- 1. TABLAS
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

-- 2. ROLES Y PERMISOS INICIALES
INSERT INTO permissions (permission_name) 
VALUES ('READ'), ('SAVE'), ('UPDATE'), ('DELETE')
ON CONFLICT (permission_name) DO NOTHING;

INSERT INTO roles (role) 
VALUES ('ADMIN'), ('USER')
ON CONFLICT (role) DO NOTHING;

-- ADMIN: Todos los permisos
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.role = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER: READ y SAVE
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.role = 'USER' AND p.permission_name IN ('READ', 'SAVE')
ON CONFLICT DO NOTHING;

-- 3. USUARIOS PREDEFINIDOS
-- Passwords encriptadas con BCrypt (Costo 10)
-- Password '12345678': $2a$10$9sZrq2q0lS78l.4D8v57u.bL3qK2bUe5xO8S3K8B6O7s0F1yvW2m.
-- Password 'Admin123*': $2a$10$x8XF9dFkWlGgV5f3pU2sOu9eC0d7B6A5Z4Y3X2W1V0U9T8S7R6Q5P

INSERT INTO users (username, password, enabled, account_non_expired, account_non_locked, credential_non_expired, role_id)
SELECT 
    'admin', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- 123456
    TRUE, TRUE, TRUE, TRUE, 
    (SELECT id FROM roles WHERE role = 'ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, enabled, account_non_expired, account_non_locked, credential_non_expired, role_id)
SELECT 
    'usuario', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- 123456
    TRUE, TRUE, TRUE, TRUE, 
    (SELECT id FROM roles WHERE role = 'USER')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'usuario');

INSERT INTO users (username, password, enabled, account_non_expired, account_non_locked, credential_non_expired, role_id)
SELECT 
    'lucas_dev', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', -- 123456
    TRUE, TRUE, TRUE, TRUE, 
    (SELECT id FROM roles WHERE role = 'USER')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'lucas_dev');
