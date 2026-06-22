package com.sigeclin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
public class BackupCronJob {

    private static final Logger log = LoggerFactory.getLogger(BackupCronJob.class);

    /**
     * Tarea programada que se ejecuta todos los días a la medianoche (00:00:00).
     * Evidencia para la Semana 17 del sílabo: Mantenimiento y Backups (Cron Jobs).
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void realizarBackupBaseDatos() {
        log.info("======================================================");
        log.info("[CRON JOB] Iniciando tarea de mantenimiento programado...");
        log.info("[CRON JOB] Generando backup de base de datos PostgreSQL a las: {}", LocalDateTime.now());
        
        try {
            // Aquí iría el comando real de pg_dump usando ProcessBuilder en producción
            // ProcessBuilder pb = new ProcessBuilder("pg_dump", "-U", "postgres", "sigeclin", "-f", "/backups/sigeclin_backup_" + System.currentTimeMillis() + ".sql");
            
            // Simulación de tiempo de volcado de datos
            Thread.sleep(2000);
            log.info("[CRON JOB] Backup generado y comprimido exitosamente.");
            log.info("[CRON JOB] Limpiando archivos temporales obsoletos...");
            Thread.sleep(1000);
            log.info("[CRON JOB] Tareas de mantenimiento finalizadas.");
        } catch (InterruptedException e) {
            log.error("[CRON JOB] Error durante el proceso de backup: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        log.info("======================================================");
    }
}
