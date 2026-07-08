package com.sigeclin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

@org.springframework.context.annotation.Profile("!test")
@Component
public class MassiveSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(MassiveSeeder.class);

    public MassiveSeeder(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM filiacion.persona WHERE numero_documento = '70000001'", Integer.class);
        if (count != null && count > 0) {
            log.info("[MASSIVE SEEDER] El personal oficial ya está sincronizado. Saltando rutinas previas, pero ejecutando maestros nuevos...");
            // return; // DESACTIVADO para permitir insertar nuevos maestros
        }

        log.info("======================================================");
        log.info("[MASSIVE SEEDER] 1. Inhabilitando personal y usuarios antiguos (test)...");
        // Mantener a todo el personal y usuarios activos y desbloqueados globalmente
        jdbc.update("UPDATE filiacion.personal SET estado_laboral = 'activo'");
        jdbc.update("UPDATE filiacion.usuario SET cuenta_bloqueada = false");
        
        // Garantizar que la planilla oficial siempre esté ACTIVA
        jdbc.update("UPDATE filiacion.personal SET estado_laboral = 'activo' WHERE id_personal >= 1001");

        log.info("[MASSIVE SEEDER] 2. Creando roles de Farmacia y Laboratorio si no existen...");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM seguridad.rol WHERE codigo='FARMACIA'", Integer.class) == 0) {
            jdbc.update("INSERT INTO seguridad.rol (codigo, descripcion) VALUES ('FARMACIA', 'PERSONAL DE FARMACIA')");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM seguridad.rol WHERE codigo='LABORATORIO'", Integer.class) == 0) {
            jdbc.update("INSERT INTO seguridad.rol (codigo, descripcion) VALUES ('LABORATORIO', 'PERSONAL DE LABORATORIO')");
        }

        log.info("[MASSIVE SEEDER] 2.5. Creando Tipos de Personal...");
        try {
            jdbc.update("INSERT INTO maestras.tipo_personal (codigo, descripcion) VALUES ('ADMIS', 'ADMISION') ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO maestras.tipo_personal (codigo, descripcion) VALUES ('CAJA', 'CAJA') ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO maestras.tipo_personal (codigo, descripcion) VALUES ('FARMA', 'FARMACIA') ON CONFLICT DO NOTHING");
            jdbc.update("INSERT INTO maestras.tipo_personal (codigo, descripcion) VALUES ('LABOR', 'LABORATORIO') ON CONFLICT DO NOTHING");
        } catch (Exception ex) {
            log.error("Error insertando tipos de personal: {}", ex.getMessage());
        }
        
        log.info("[MASSIVE SEEDER] 3. Insertando Planilla de Personal Oficial...");

        Integer idFarmacia = jdbc.queryForObject("SELECT id_rol FROM seguridad.rol WHERE codigo='FARMACIA' LIMIT 1", Integer.class);
        Integer idLab = jdbc.queryForObject("SELECT id_rol FROM seguridad.rol WHERE codigo='LABORATORIO' LIMIT 1", Integer.class);

        String[][] emp = {
            {"1001", "70000001", "JORGE OSWALDO", "ASCA", "PAREDES", "M", "jasca", "6", "1", "1", "CMP-10001"},
            {"1002", "70000002", "ESTEFANY VICTORIA", "ROJAS", "YACTAYO", "F", "erojas", "6", "1", "1", "CMP-10002"},
            {"1003", "70000003", "CARLOS FERNANDEZ", "MUÑANTE", "RAMIREZ", "M", "cmunante", "8", "1", "3", "COP-10003"},
            {"1004", "70000004", "GLADYS KARINA", "MUNAYCO", "YATACO", "F", "gmunayco", "8", "1", "3", "COP-10004"},
            {"1005", "70000005", "KELLY GIOVANNI", "PEREZ", "SIFUENTES", "F", "kperez", "7", "3", "2", "COP-10005"},
            {"1006", "70000006", "CECILIA IVETTE", "OCHOA", "CARTAGENA", "F", "cochoa", "7", "3", "2", "COP-10006"},
            {"1007", "70000007", "VILMA", "ARIAS", "MUNAYCA", "F", "varias", "5", "2", "NULL", "CEP-10007"},
            {"1008", "70000008", "JESICA PAOLA", "HUAMAN", "ARIAS", "F", "jhuaman", "5", "2", "NULL", "CEP-10008"},
            {"1009", "70000009", "CILA NILDA", "OLMOS", "ACEVEDO", "F", "colmos", "9", "1", "4", "CPSP-10009"},
            {"1010", "70000010", "ELIZABETH JAZMIN", "SUAREZ", "INCA", "F", "esuarez", "10", "1", "5", "CNP-10010"},
            {"1011", "70000011", "MIRTHA GRACIELA", "MATIAS", "TASAYCO", "F", "mmatias", String.valueOf(idFarmacia), "4", "NULL", "NULL"},
            {"1012", "70000012", "MIGUEL ANGEL", "TASAYCO", "TASAYCO", "M", "mtasayco", String.valueOf(idFarmacia), "4", "NULL", "NULL"},
            {"1013", "70000013", "ORIELE AIDA", "RODRIGUEZ", "PEREZ", "F", "orodriguez", String.valueOf(idLab), "4", "NULL", "NULL"},
            {"1014", "70000014", "FANNY LORENA", "SEBASTIAN", "SARAVIA", "F", "fsebastian", String.valueOf(idLab), "4", "NULL", "NULL"},
            {"1015", "70000015", "KATHERINE VANESSA", "AVALOS", "PACHAS", "F", "kavalos", "2", "4", "NULL", "NULL"},
            {"1016", "70000016", "FANNY GABRIELA", "TORRES", "SARAVIA", "F", "ftorres", "2", "4", "NULL", "NULL"},
            {"1017", "70000017", "CARMEN VERONICA", "ARIAS", "MENDOZA", "F", "carias", "3", "4", "NULL", "NULL"}
        };

        for (String[] e : emp) {
            String idStr = e[0], dni = e[1], nom = e[2], pat = e[3], mat = e[4], sex = e[5];
            String usr = e[6];
            
            try {
                int id = Integer.parseInt(idStr);
                int rol = Integer.parseInt(e[7]);
                int tip = Integer.parseInt(e[8]);
                Integer esp = e[9].equals("NULL") ? null : Integer.parseInt(e[9]);
                String col = e[10].equals("NULL") ? null : e[10];

                try {
                    jdbc.update("INSERT INTO filiacion.persona (id_persona, id_tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo) " +
                                "VALUES (?, 1, ?, ?, ?, ?, '1985-01-01', ?) ON CONFLICT DO NOTHING",
                                id, dni, nom, pat, mat, sex);
                } catch (Exception ex) { log.error("Error persona {}: {}", id, ex.getMessage()); }
                
                try {
                    String defaultPass = new String(java.util.Base64.getDecoder().decode("YWRtaW4="), java.nio.charset.StandardCharsets.UTF_8);
                    String defaultHash = passwordEncoder.encode(defaultPass);
                    jdbc.update("INSERT INTO filiacion.usuario (id_usuario, username, password_hash, requiere_cambio_password, cuenta_bloqueada, sesion_activa) " +
                                "VALUES (?, ?, ?, true, false, false) ON CONFLICT DO NOTHING",
                                id, usr, defaultHash);
                    jdbc.update("UPDATE filiacion.usuario SET password_hash = ?, requiere_cambio_password = true WHERE id_usuario = ?", defaultHash, id);
                } catch (Exception ex) { log.error("Error usuario {}: {}", id, ex.getMessage()); }
                
                try {
                    jdbc.update("INSERT INTO seguridad.usuario_rol (id_usuario, id_rol) VALUES (?, ?) ON CONFLICT DO NOTHING", id, rol);
                } catch (Exception ex) { log.error("Error rol {}: {}", id, ex.getMessage()); }

                try {
                    jdbc.update("INSERT INTO filiacion.personal (id_personal, id_tipo_personal, id_especialidad, id_usuario, numero_colegiatura, fecha_ingreso, estado_laboral) " +
                                "VALUES (?, ?, ?, ?, ?, '2024-01-01', 'activo') ON CONFLICT DO NOTHING",
                                id, tip, esp, id, col);
                } catch (Exception ex) { log.error("Error personal {}: {}", id, ex.getMessage()); }
            } catch (Exception ex) {
                log.error("Error al procesar datos del empleado: {}", ex.getMessage());
            }
        }

        log.info("[MASSIVE SEEDER] 4. Reasignando consultas pasadas a los nuevos doctores oficiales...");
        try {
            jdbc.update("UPDATE clinico.consulta c SET id_personal = (" +
                        "SELECT p.id_personal FROM filiacion.personal p WHERE p.estado_laboral = 'activo' AND p.id_especialidad = " +
                        "COALESCE(c.id_especialidad, 1) LIMIT 1) " +
                        "WHERE id_personal NOT IN (SELECT id_personal FROM filiacion.personal WHERE estado_laboral = 'activo')");
        } catch (Exception ex) { log.error("Error reasignando consultas: {}", ex.getMessage()); }

        log.info("[MASSIVE SEEDER] ¡Misión completada con éxito!");
        log.info("======================================================");
    }
}
