package com.sigeclin.config;

import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.seguridad.model.Rol;
import com.sigeclin.filiacion.model.TipoDocumento;
import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.PersonalRepository;
import com.sigeclin.seguridad.repository.RolRepository;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.filiacion.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class SystemInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PersonalRepository personalRepository;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info(">>> [SIGECLIN] Iniciando Sincronización Total del Sistema...");
        
        try {
            purgeDatabase();
            ensureSchemaConsistency();
            seedTipoDocumento();
            createRolesIfMissing();
            seedStaff();
            seedUsers();
            seedMedicamentos();
            log.info(">>> [SIGECLIN] Sincronización Finalizada con Éxito.");
        } catch (Exception e) {
            log.error(">>> [SIGECLIN] ERROR CRITICO EN INICIALIZACION: {}", e.getMessage(), e);
        }
    }

    private void ensureSchemaConsistency() {
        log.info(">>> [SIGECLIN] Verificando consistencia de esquema...");
        try {
            jdbcTemplate.execute("ALTER TABLE clinico.triaje ADD COLUMN IF NOT EXISTS servicio_destino VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE clinico.triaje ADD COLUMN IF NOT EXISTS alerta_clinica BOOLEAN DEFAULT false");
            jdbcTemplate.execute("ALTER TABLE clinico.triaje ADD COLUMN IF NOT EXISTS detalle_alerta TEXT");
            
            // Actualizar restricción de urgencia para incluir 'naranja'
            try {
                jdbcTemplate.execute("ALTER TABLE clinico.triaje DROP CONSTRAINT IF EXISTS triaje_clasificacion_urgencia_check");
                jdbcTemplate.execute("ALTER TABLE clinico.triaje ADD CONSTRAINT triaje_clasificacion_urgencia_check CHECK (clasificacion_urgencia IN ('rojo', 'naranja', 'amarillo', 'verde'))");
            } catch (Exception e) {
                log.warn("La restricción de urgencia ya existe o no pudo actualizarse.");
            }

            jdbcTemplate.execute("ALTER TABLE filiacion.paciente ADD COLUMN IF NOT EXISTS servicio_solicitado VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE filiacion.paciente ADD COLUMN IF NOT EXISTS referencia_direccion VARCHAR(255)");
            
            // Resolver inconsistencias de columnas heredadas no utilizadas de esquemas antiguos
            try {
                jdbcTemplate.execute("ALTER TABLE clinico.detalle_receta ALTER COLUMN duracion DROP NOT NULL");
            } catch (Exception e) {
                log.warn("No se pudo alterar la columna duracion en detalle_receta: {}", e.getMessage());
            }
            try {
                jdbcTemplate.execute("ALTER TABLE clinico.detalle_receta ALTER COLUMN indicaciones DROP NOT NULL");
            } catch (Exception e) {
                log.warn("No se pudo alterar la columna indicaciones en detalle_receta: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("No se pudo completar la consistencia de esquema: {}", e.getMessage());
        }
    }

    private void purgeDatabase() {
        log.info(">>> [SIGECLIN] Purgando datos...");
        String[] tables = {
            "clinico.auditoria_acceso", "seguridad.sesion_log",
            "clinico.detalle_receta", "clinico.receta_medica",
            "clinico.dispensacion", "clinico.lote_medicamento",
            "clinico.diagnostico_consulta", "clinico.consulta",
            "clinico.triaje", "clinico.alergia_paciente", "clinico.antecedente_paciente",
            "filiacion.paciente", "filiacion.personal", "filiacion.tipo_documento", "filiacion.persona",
            "maestras.catalogo_medicamentos", "maestras.familia_farmacologica", "maestras.via_administracion"
        };
        for (String table : tables) {
            try { jdbcTemplate.execute("TRUNCATE " + table + " CASCADE"); } catch (Exception e) {
                log.debug("No se pudo truncar {}: {}", table, e.getMessage());
            }
        }
        jdbcTemplate.execute("ALTER SEQUENCE IF EXISTS clinico.consulta_id_consulta_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE IF EXISTS clinico.triaje_id_triaje_seq RESTART WITH 1");
    }

    private void seedTipoDocumento() {
        log.info(">>> [SIGECLIN] Sembrando TipoDocumento...");
        jdbcTemplate.execute("INSERT INTO filiacion.tipo_documento (id_tipo_documento, codigo, descripcion, regex_validacion, activo) " +
                             "VALUES (1, 'DNI', 'Documento Nacional de Identidad', '^[0-9]{8}$', true)");
        jdbcTemplate.execute("INSERT INTO filiacion.tipo_documento (id_tipo_documento, codigo, descripcion, regex_validacion, activo) " +
                             "VALUES (2, 'CE', 'Carnet de Extranjería', '^[A-Z0-9]{9,12}$', true)");
        jdbcTemplate.execute("INSERT INTO filiacion.tipo_documento (id_tipo_documento, codigo, descripcion, regex_validacion, activo) " +
                             "VALUES (3, 'PAS', 'Pasaporte', '^[A-Z0-9]{5,15}$', true)");
        jdbcTemplate.execute("INSERT INTO filiacion.tipo_documento (id_tipo_documento, codigo, descripcion, regex_validacion, activo) " +
                             "VALUES (4, 'DIE', 'Documento de Identidad Extranjero', '^[A-Z0-9]{5,15}$', true)");
        jdbcTemplate.execute("INSERT INTO filiacion.tipo_documento (id_tipo_documento, codigo, descripcion, regex_validacion, activo) " +
                             "VALUES (5, 'S/DOC', 'Sin Documento', '^[A-Z0-9]{5,15}$', true)");
        jdbcTemplate.execute("INSERT INTO filiacion.tipo_documento (id_tipo_documento, codigo, descripcion, regex_validacion, activo) " +
                             "VALUES (6, 'CNV', 'Certificado de Nacido Vivo', '^[0-9]{10}$', true)");
    }

    private void createRolesIfMissing() {
        String[] roles = {"MEDICO GENERAL", "ENFERMERIA", "OBSTETRICIA", "ODONTOLOGIA", "PSICOLOGIA", "NUTRICION", "ADMIN"};
        for (String r : roles) {
            String code = r.replace(" ", "_");
            jdbcTemplate.execute("INSERT INTO seguridad.rol (codigo, descripcion) VALUES ('" + code + "', '" + r + "') ON CONFLICT (codigo) DO NOTHING");
        }
    }

    private void seedStaff() {
        log.info(">>> [SIGECLIN] Cargando personal...");
        // Medicina (70000001)
        insertPersonal(1, "70000001", "TERESA VIOLETA", "PEJERREY", "BRAVO", 1, "CMP 12345", "F");
        // Enfermeria (70000017)
        insertPersonal(2, "70000017", "VILMA", "ARIAS", "VIVANCO", 4, "CEP 20202", "F");
        // Obstetricia (70000009)
        insertPersonal(3, "70000009", "MARTHA SUSANA", "HUAYAYO", "", 3, "COP 33333", "F");
        // Odontologia (70000007)
        insertPersonal(4, "70000007", "CARLOS FERNANDO", "VILVANTI", "RAMIREZ", 2, "COP 11111", "M");
        // Psicologia (70000027)
        insertPersonal(5, "70000027", "CLAUDIA", "QUIROS", "ACEVEDO", 5, "CPP 34343", "F");
        // Nutricion (70000028)
        insertPersonal(6, "70000028", "ELIZABETH JAZMIN", "SUAREZ", "INCA", 6, "CNP 45454", "F");
    }

    private void insertPersonal(int id, String dni, String nom, String apP, String apM, int type, String coleg, String sex) {
        jdbcTemplate.execute("INSERT INTO filiacion.persona (id_persona, id_tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno, sexo, fecha_nacimiento) " +
                             "VALUES (" + (200 + id) + ", 1, '" + dni + "', '" + nom + "', '" + apP + "', '" + apM + "', '" + sex + "', '1980-01-01')");
        jdbcTemplate.execute("INSERT INTO filiacion.personal (id_personal, id_tipo_personal, numero_colegiatura, fecha_ingreso, estado_laboral) " +
                             "VALUES (" + (200 + id) + ", " + type + ", '" + (coleg == null ? "" : coleg) + "', NOW(), 'activo')");
    }

    private void seedUsers() {
        log.info(">>> [SIGECLIN] Creando usuarios...");
        
        // Admin
        String pass = passwordEncoder.encode("admin");
        jdbcTemplate.execute("DELETE FROM filiacion.usuario WHERE username = 'admin'");
        jdbcTemplate.execute("INSERT INTO filiacion.persona (id_persona, id_tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno, fecha_nacimiento) " +
                             "VALUES (100, 1, '00000000', 'ADMINISTRADOR', 'SISTEMA', '', '1980-01-01')");
        jdbcTemplate.execute("INSERT INTO filiacion.usuario (id_usuario, username, password_hash) " +
                             "VALUES (100, 'admin', '" + pass + "')");
        
        // Roles for admin
        Integer adminRolId = jdbcTemplate.queryForObject("SELECT id_rol FROM seguridad.rol WHERE codigo = 'ADMIN'", Integer.class);
        jdbcTemplate.execute("INSERT INTO seguridad.usuario_rol (id_usuario, id_rol) VALUES (100, " + adminRolId + ")");

        // Service Users
        createServiceUser("medicina", pass, "MEDICO_GENERAL", 201);
        createServiceUser("enfermeria", pass, "ENFERMERIA", 202);
        createServiceUser("obstetricia", pass, "OBSTETRICIA", 203);
        createServiceUser("odontologia", pass, "ODONTOLOGIA", 204);
        createServiceUser("psicologia", pass, "PSICOLOGIA", 205);
        createServiceUser("nutricion", pass, "NUTRICION", 206);
    }

    private void createServiceUser(String user, String pass, String rolCode, int personId) {
        jdbcTemplate.execute("DELETE FROM filiacion.usuario WHERE username = '" + user + "'");
        jdbcTemplate.execute("INSERT INTO filiacion.usuario (id_usuario, username, password_hash) VALUES (" + personId + ", '" + user + "', '" + pass + "')");
        Integer rolId = jdbcTemplate.queryForObject("SELECT id_rol FROM seguridad.rol WHERE codigo = '" + rolCode + "'", Integer.class);
        jdbcTemplate.execute("INSERT INTO seguridad.usuario_rol (id_usuario, id_rol) VALUES (" + personId + ", " + rolId + ")");
        
        // Sincronizar secuencias para evitar errores de duplicados (id_persona = 205, etc.)
        syncSequences();
    }

    private void syncSequences() {
        log.info(">>> [SIGECLIN] Sincronizando secuencias de base de datos...");
        String[] sequences = {
            "filiacion.persona_id_persona_seq",
            "filiacion.tipo_documento_id_tipo_documento_seq",
            "seguridad.rol_id_rol_seq",
            "maestras.especialidad_id_especialidad_seq",
            "maestras.tipo_personal_id_tipo_personal_seq"
        };
        for (String seq : sequences) {
            try {
                // Ejemplo: filiacion.persona_id_persona_seq
                String[] parts = seq.replace("_seq", "").split("\\.");
                String schema = parts[0];
                String tableAndCol = parts[1]; // persona_id_persona
                
                // Dividir persona_id_persona -> tabla="persona", columna="id_persona"
                int lastIdIdx = tableAndCol.lastIndexOf("_id_");
                String table = tableAndCol.substring(0, lastIdIdx);
                String idCol = tableAndCol.substring(lastIdIdx + 1);
                
                String fullTable = schema + "." + table;
                jdbcTemplate.execute("SELECT setval('" + seq + "', COALESCE((SELECT MAX(" + idCol + ") FROM " + fullTable + "), 1))");
            } catch (Exception e) {
                log.warn("No se pudo sincronizar secuencia {}: {}", seq, e.getMessage());
            }
        }
    }

    private void seedMedicamentos() {
        log.info(">>> [SIGECLIN] Sembrando Vías de Administración...");
        jdbcTemplate.execute("INSERT INTO maestras.via_administracion (id_via_administracion, codigo, descripcion) VALUES (1, 'ORAL', 'Oral') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.via_administracion (id_via_administracion, codigo, descripcion) VALUES (2, 'EV', 'Endovenosa') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.via_administracion (id_via_administracion, codigo, descripcion) VALUES (3, 'IM', 'Intramuscular') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.via_administracion (id_via_administracion, codigo, descripcion) VALUES (4, 'TOP', 'Tópica') ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.via_administracion (id_via_administracion, codigo, descripcion) VALUES (5, 'INH', 'Inhalatoria') ON CONFLICT DO NOTHING");

        log.info(">>> [SIGECLIN] Sembrando Familias Farmacológicas...");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (1, 'N02B', 'Analgésicos y Antipiréticos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (2, 'J01C', 'Antibióticos Beta-lactámicos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (3, 'A02B', 'Medicamentos para Úlcera Péptica', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (4, 'R03A', 'Adrenérgicos Inhalatorios', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (5, 'M01A', 'Antiinflamatorios no Esteroideos (AINEs)', true) ON CONFLICT DO NOTHING");

        log.info(">>> [SIGECLIN] Sembrando Catálogo de Medicamentos (MINSA)...");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (1, 'MED-001', 'PARACETAMOL', 'Paracetamol 500mg', 1, 'Tableta', '500mg', 10, true, true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (2, 'MED-002', 'IBUPROFENO', 'Ibuprofeno 400mg', 5, 'Tableta', '400mg', 10, true, true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (3, 'MED-003', 'AMOXICILINA', 'Amoxicilina 500mg', 2, 'Cápsula', '500mg', 10, true, true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (4, 'MED-004', 'OMEPRAZOL', 'Omeprazol 20mg', 3, 'Cápsula', '20mg', 10, true, true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (5, 'MED-005', 'SALBUTAMOL', 'Salbutamol Inhalador', 4, 'Aerosol', '100mcg/dosis', 5, true, true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (6, 'MED-006', 'DICLOFENACO', 'Diclofenaco Sódico', 5, 'Ampolla', '75mg/3ml', 10, true, true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                             "VALUES (7, 'MED-007', 'LORATADINA', 'Loratadina 10mg', 1, 'Tableta', '10mg', 10, true, true) ON CONFLICT DO NOTHING");

        try {
            jdbcTemplate.execute("SELECT setval('maestras.via_administracion_id_via_administracion_seq', COALESCE((SELECT MAX(id_via_administracion) FROM maestras.via_administracion), 1))");
            jdbcTemplate.execute("SELECT setval('maestras.familia_farmacologica_id_familia_seq', COALESCE((SELECT MAX(id_familia) FROM maestras.familia_farmacologica), 1))");
            jdbcTemplate.execute("SELECT setval('maestras.catalogo_medicamentos_id_medicamento_seq', COALESCE((SELECT MAX(id_medicamento) FROM maestras.catalogo_medicamentos), 1))");
        } catch (Exception e) {
            log.warn("No se pudo sincronizar secuencias de tablas maestras: {}", e.getMessage());
        }
    }
}
