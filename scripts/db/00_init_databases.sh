#!/bin/bash
# ========================================================
# Inicializa todas las bases de datos de ReservaYa en PostgreSQL
# ========================================================
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-postgres}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=========================================================="
echo " Inicializando Bases de Datos de ReservaYa en $DB_HOST:$DB_PORT"
echo "=========================================================="

DATABASES=("auth_db" "booking_db" "payment_db" "notification_db")
FILES=(
  "$SCRIPT_DIR/01_auth_service.sql"
  "$SCRIPT_DIR/02_booking_service.sql"
  "$SCRIPT_DIR/03_payment_service.sql"
  "$SCRIPT_DIR/04_notification_service.sql"
)

for i in "${!DATABASES[@]}"; do
  DB="${DATABASES[$i]}"
  FILE="${FILES[$i]}"
  echo -e "\nCreando base de datos si no existe: $DB..."
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "CREATE DATABASE $DB;" 2>/dev/null || true

  echo "Ejecutando $FILE en $DB..."
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB" -f "$FILE"
done

echo -e "\n=========================================================="
echo " Inicializacion de Base de Datos Completada Exitosamente!"
echo "=========================================================="
