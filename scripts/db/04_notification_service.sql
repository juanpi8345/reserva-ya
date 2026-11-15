-- ====================================================================
-- DDL & DML: notification-service
-- Base de datos: PostgreSQL (notification_db)
-- ====================================================================

\c notification_db;

-- 1. TABLA USER_CONTACTS (Mapeo de contacto para envios de notificaciones)
CREATE TABLE IF NOT EXISTS user_contacts (
    user_id BIGINT PRIMARY KEY,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL
);

-- 2. TABLA NOTIFICATIONS (Auditoria y cola de notificaciones)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sent_to VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idempotency_key VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_idempotency ON notifications(idempotency_key);

-- 3. DATOS DE EJEMPLO
INSERT INTO user_contacts (user_id, phone, email)
VALUES 
    (1, '+5491100000000', 'admin@reservaya.com'),
    (2, '+5491112345678', 'usuario@example.com'),
    (3, '+5491123456789', 'lucas@example.com')
ON CONFLICT (user_id) DO NOTHING;

