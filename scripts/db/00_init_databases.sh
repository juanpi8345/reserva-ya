
#!/bin/bash
# ========================================================
# Inicializa todas las bases de datos de ReservaYa en PostgreSQL
# ========================================================
BD_USER="${POSTGRES_USER:-${BD_USER:-postgres}}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=========================================================="
echo " Inicializando Bases de Datos de ReservaYa"
echo "=========================================================="

DATABASES=("auth_db" "booking_db" "payment_db" "notification_db")
FILES=(
  "$SCRIPT_DIR/01_auth_service.sql"
  "$SCRIPT_DIR/02_booking_service.sql"
  "$SCRIPT_DIR/03_payment_service.sql"
  "$SCRIPT_DIR/04_notification_service.sql"
)

for i in "${!DATABASES[@]}"; do
  BD="${DATABASES[$i]}"
  FILE="${FILES[$i]}"

  echo -e "\nCreando base de datos si no existe: $BD..."

  psql -U "$BD_USER" -d postgres -c "CREATE DATABASE $BD;" 2>/dev/null || true

  echo "Ejecutando $FILE en $BD..."
  psql -U "$BD_USER" -d "$BD" -f "$FILE"
done

echo -e "\n=========================================================="
echo " Inicializacion de Base de Datos Completada Exitosamente!"
echo "=========================================================="
