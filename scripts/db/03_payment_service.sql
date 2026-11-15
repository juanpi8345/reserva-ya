-- ====================================================================
-- DDL & DML: payment-service
-- Base de datos: PostgreSQL (payment_db)
-- ====================================================================

\c payment_db;

-- 1. EXTENSION PARA UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. TABLA PAYMENTS
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
