package com.sigeclin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TipoPersonalFixer implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private static final Logger log = LoggerFactory.getLogger(TipoPersonalFixer.class);

    public TipoPersonalFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("======================================================");
        log.info("[TIPO PERSONAL FIXER] Sincronizando catálogo maestro de Tipo Personal...");

        // 1. Renombrar las categorías heredadas para que calcen con la lista final del usuario
        jdbc.update("UPDATE maestras.tipo_personal SET descripcion = 'MEDICINA GENERAL' WHERE codigo = 'MED' OR descripcion = 'MÉDICO'");
        jdbc.update("UPDATE maestras.tipo_personal SET descripcion = 'ENFERMERIA' WHERE codigo = 'ENF' OR descripcion = 'ENFERMERO'");
        jdbc.update("UPDATE maestras.tipo_personal SET descripcion = 'OBSTETRICIA' WHERE codigo = 'OBS' OR descripcion = 'OBSTETRA'");
        jdbc.update("UPDATE maestras.tipo_personal SET descripcion = 'ADMIN' WHERE codigo = 'ADM' OR descripcion = 'ADMINISTRATIVO'");

        // 2. Definir la lista de los nuevos perfiles
        String[] codigos = {"ADMIS", "CAJA", "FARMA", "LABOR", "TRIAJ", "ODONT", "PSICO", "NUTRI", "SADM"};
        String[] nombres = {"ADMISION", "CAJA", "FARMACIA", "LABORATORIO", "TRIAJE", "ODONTOLOGIA", "PSICOLOGIA", "NUTRICION", "SUPERADMIN"};

        // 3. Insertar solo los que no existan aún
        for (int i = 0; i < codigos.length; i++) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM maestras.tipo_personal WHERE descripcion = ?", Integer.class, nombres[i]);
            if (count == null || count == 0) {
                jdbc.update("INSERT INTO maestras.tipo_personal (codigo, descripcion) VALUES (?, ?)", codigos[i], nombres[i]);
            }
        }
        
        // 4. Migrar al personal actual para que adopten sus nuevos tipos de personal
        jdbc.update("UPDATE filiacion.personal SET id_tipo_personal = (SELECT id_tipo_personal FROM maestras.tipo_personal WHERE descripcion = 'ODONTOLOGIA' LIMIT 1) WHERE id_especialidad = 3");
        log.info("[TIPO PERSONAL FIXER] Catálogo depurado y estandarizado con la lista oficial de 13 cargos.");
        log.info("======================================================");
    }
}
