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

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.url}")
    private String dbUrl;

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}")
    private String dbUser;

    @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}")
    private String dbPassword;

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
            String host = extractHost(dbUrl);
            String port = extractPort(dbUrl);
            String dbName = extractDbName(dbUrl);
            
            java.io.File backupDir = new java.io.File("./backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            String filename = "sigeclin_backup_" + System.currentTimeMillis() + ".sql";
            java.io.File backupFile = new java.io.File(backupDir, filename);

            ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", dbUser,
                "-F", "p",
                "-f", backupFile.getAbsolutePath(),
                dbName
            );
            
            pb.environment().put("PGPASSWORD", dbPassword);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("[CRON JOB] Backup generado exitosamente: {}", backupFile.getName());
            } else {
                log.error("[CRON JOB] pg_dump finalizó con código de error: {}", exitCode);
            }
            
            log.info("[CRON JOB] Limpiando archivos temporales y backups obsoletos...");
            limpiarBackupsAntiguos(backupDir);
            log.info("[CRON JOB] Tareas de mantenimiento finalizadas.");
        } catch (java.io.IOException e) {
            log.warn("[CRON JOB] No se pudo ejecutar pg_dump (¿Herramienta no instalada en PATH?): {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("[CRON JOB] Error durante el proceso de backup: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        log.info("======================================================");
    }

    private void limpiarBackupsAntiguos(java.io.File backupDir) {
        java.io.File[] files = backupDir.listFiles((dir, name) -> name.startsWith("sigeclin_backup_") && name.endsWith(".sql"));
        if (files != null) {
            long limitTime = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            for (java.io.File file : files) {
                if (file.lastModified() < limitTime) {
                    if (file.delete()) {
                        log.info("[CRON JOB] Backup antiguo eliminado: {}", file.getName());
                    }
                }
            }
        }
    }

    private String extractHost(String url) {
        try {
            String cleanUrl = url.substring("jdbc:postgresql://".length());
            int slashIndex = cleanUrl.indexOf('/');
            String hostPort = slashIndex != -1 ? cleanUrl.substring(0, slashIndex) : cleanUrl;
            if (hostPort.contains(":")) {
                return hostPort.split(":")[0];
            }
            return hostPort;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String extractPort(String url) {
        try {
            String cleanUrl = url.substring("jdbc:postgresql://".length());
            int slashIndex = cleanUrl.indexOf('/');
            String hostPort = slashIndex != -1 ? cleanUrl.substring(0, slashIndex) : cleanUrl;
            if (hostPort.contains(":")) {
                return hostPort.split(":")[1];
            }
            return "5432";
        } catch (Exception e) {
            return "5432";
        }
    }

    private String extractDbName(String url) {
        try {
            String cleanUrl = url.substring("jdbc:postgresql://".length());
            int slashIndex = cleanUrl.indexOf('/');
            if (slashIndex != -1) {
                String dbParams = cleanUrl.substring(slashIndex + 1);
                int questionIndex = dbParams.indexOf('?');
                return questionIndex != -1 ? dbParams.substring(0, questionIndex) : dbParams;
            }
            return "sigeclin";
        } catch (Exception e) {
            return "sigeclin";
        }
    }
}
