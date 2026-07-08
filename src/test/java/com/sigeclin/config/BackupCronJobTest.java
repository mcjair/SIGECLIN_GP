package com.sigeclin.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BackupCronJobTest {

    private BackupCronJob cronJob;

    @BeforeEach
    void setUp() {
        cronJob = new BackupCronJob();
        ReflectionTestUtils.setField(cronJob, "dbUrl", "jdbc:postgresql://localhost:5432/sigeclin?sslmode=prefer");
        ReflectionTestUtils.setField(cronJob, "dbUser", "admin");
        ReflectionTestUtils.setField(cronJob, "dbPassword", "admin");
    }

    @Test
    void testRealizarBackupBaseDatos() {
        assertDoesNotThrow(() -> cronJob.realizarBackupBaseDatos());
    }

    @Test
    void testRealizarBackupConUrlInvalida() {
        ReflectionTestUtils.setField(cronJob, "dbUrl", "jdbc:postgresql://host-invalido/db-invalida");
        assertDoesNotThrow(() -> cronJob.realizarBackupBaseDatos());
    }

    @Test
    void testLimpiarBackupsAntiguos() throws IOException {
        File tempDir = new File("./temp_backups_test");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File newBackup = new File(tempDir, "sigeclin_backup_" + System.currentTimeMillis() + ".sql");
        newBackup.createNewFile();

        File oldBackup = new File(tempDir, "sigeclin_backup_" + (System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)) + ".sql");
        oldBackup.createNewFile();
        oldBackup.setLastModified(System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000));

        ReflectionTestUtils.invokeMethod(cronJob, "limpiarBackupsAntiguos", tempDir);

        assertTrue(newBackup.exists(), "El backup nuevo no debe ser eliminado");
        assertFalse(oldBackup.exists(), "El backup antiguo (mas de 7 dias) debe ser eliminado");

        newBackup.delete();
        tempDir.delete();
    }
}
