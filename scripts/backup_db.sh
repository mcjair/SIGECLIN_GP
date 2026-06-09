#!/bin/bash
# ==============================================================================
# SIGECLIN - Script de Respaldo Automatizado de Base de Datos (Linux Bash)
# ==============================================================================
# Este script realiza un volcado completo de la base de datos PostgreSQL 'sigeclin'.
# Se puede programar usando 'cron' (crontab -e).
#
# Ejemplo de configuración para ejecutarse todos los días a las 11:00 PM:
# 0 23 * * * /home/usuario/sigeclin/scripts/backup_db.sh > /dev/null 2>&1
# ==============================================================================

# --- CONFIGURACIÓN DE PARÁMETROS ---
DB_NAME="sigeclin"
DB_USER="admin"
DB_PASSWORD="admin"
DB_HOST="127.0.0.1"
DB_PORT="5432"

# Determinar directorio del script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BACKUP_DIR="$SCRIPT_DIR/../backups"
DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/sigeclin_backup_$DATE.sql"

# --- EJECUCIÓN DEL PROCESO ---

# Crear el directorio de backups si no existe
mkdir -p "$BACKUP_DIR"

echo "Iniciando respaldo de la base de datos '$DB_NAME'..."
export PGPASSWORD="$DB_PASSWORD"

# Ejecución de pg_dump
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -F p -v -f "$BACKUP_FILE" "$DB_NAME"

if [ $? -eq 0 ]; then
    echo "¡Respaldo completado con éxito!"
    echo "Archivo generado: $BACKUP_FILE"
    
    # Mantener solo los últimos 7 días de respaldos y eliminar los antiguos
    find "$BACKUP_DIR" -name "sigeclin_backup_*.sql" -mtime +7 -exec rm -f {} \;
    echo "Limpieza de backups antiguos finalizada."
else
    echo "ERROR: Falló el respaldo de la base de datos." >&2
    exit 1
fi

# Limpiar variable de entorno por seguridad
unset PGPASSWORD
