# ==============================================================================
# SIGECLIN - Script de Respaldo Automatizado de Base de Datos (Windows PowerShell)
# ==============================================================================
# Este script realiza un volcado completo de la base de datos PostgreSQL 'sigeclin'.
# Se puede programar en el Programador de Tareas de Windows (Task Scheduler).
#
# Para ejecutarlo automáticamente, crea una tarea con la acción:
# Powershell.exe -ExecutionPolicy Bypass -File "C:\Ruta\Al\Proyecto\scripts\backup_db.ps1"
# ==============================================================================

# --- CONFIGURACIÓN DE PARÁMETROS ---
$DB_NAME = "sigeclin"
$DB_USER = "admin"
$DB_PASSWORD = "admin"
$DB_HOST = "127.0.0.1"
$DB_PORT = "5432"

# Ruta para almacenar los backups (dentro del proyecto)
$BACKUP_DIR = Join-Path $PSScriptRoot "..\backups"
$DATE = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILE = "$BACKUP_DIR\sigeclin_backup_$DATE.sql"

# Ruta de instalación de pg_dump (Ajustar si PostgreSQL se instaló en otra ruta)
$PG_DUMP_PATH = "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe"
if (-not (Test-Path $PG_DUMP_PATH)) {
    $PG_DUMP_PATH = "C:\Program Files\PostgreSQL\15\bin\pg_dump.exe"
}
if (-not (Test-Path $PG_DUMP_PATH)) {
    # Si no se encuentra en las rutas estándar, intentar buscar en el PATH del sistema
    $PG_DUMP_PATH = "pg_dump"
}

# --- EJECUCIÓN DEL PROCESO ---

# Crear el directorio de backups si no existe
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Force -Path $BACKUP_DIR | Out-Null
    Write-Host "Directorio de backups creado: $BACKUP_DIR" -ForegroundColor Cyan
}

Write-Host "Iniciando respaldo de la base de datos '$DB_NAME'..." -ForegroundColor Green
$env:PGPASSWORD = $DB_PASSWORD

try {
    # Ejecución de pg_dump
    & $PG_DUMP_PATH -h $DB_HOST -p $DB_PORT -U $DB_USER -F p -v -f $BACKUP_FILE $DB_NAME

    if ($LASTEXITCODE -eq 0) {
        Write-Host "¡Respaldo completado con éxito!" -ForegroundColor Green
        Write-Host "Archivo generado: $BACKUP_FILE" -ForegroundColor Yellow
        
        # Opcional: Mantener solo los últimos 7 días de respaldos y depurar los antiguos
        Get-ChildItem -Path $BACKUP_DIR -Filter "sigeclin_backup_*.sql" | 
            Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } | 
            Remove-Item -Force
            
        Write-Host "Limpieza de backups antiguos (mayores a 7 días) finalizada." -ForegroundColor Gray
    } else {
        Write-Error "Error en pg_dump. Código de salida: $LASTEXITCODE"
    }
} catch {
    Write-Error "Ocurrió una excepción al intentar realizar el respaldo: $_"
} finally {
    # Limpiar variable de entorno de contraseña por seguridad
    Remove-Item env:\PGPASSWORD -ErrorAction SilentlyContinue
}
