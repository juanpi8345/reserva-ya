-- ====================================================================
-- DDL & DML: booking-service
-- Base de datos: PostgreSQL (booking_db)
-- ====================================================================

\c booking_db;

-- 1. EXTENSION PARA UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. TABLA BOOKINGS
CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id BIGINT NOT NULL,
    resource_id UUID NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    check_in TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    check_out TIMESTAMP WITHOUT TIME ZONE,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices para busquedas eficientes y validacion de solapamiento
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_resource_dates ON bookings(resource_id, check_in, check_out);

