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
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Configuration
@Profile("!test")
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
            long userCount = usuarioRepository.count();
            if (userCount > 0) {
                log.info(">>> [SIGECLIN] Base de datos ya inicializada (usuarios encontrados: {}). Omitiendo purga y sembrado para proteger datos.", userCount);
                ensureSchemaConsistency();
                return;
            }
            
            purgeDatabase();
            ensureSchemaConsistency();
            seedTipoDocumento();
            createRolesIfMissing();
            seedStaff();
            seedUsers();
            seedMedicamentos();
            seedExamenes();
            seedCatalogoCiex();
            seedLotes();
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

            // Columna servicios para filtrado por módulo en catálogo CIE-10 curado
            jdbcTemplate.execute("ALTER TABLE maestras.cie10 ADD COLUMN IF NOT EXISTS servicios VARCHAR(255)");

            // Crear tabla maestras.examen si no existe (para Laboratorio)
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS maestras.examen (" +
                "id_examen SERIAL PRIMARY KEY, " +
                "codigo VARCHAR(20) UNIQUE NOT NULL, " +
                "nombre VARCHAR(200) NOT NULL, " +
                "area VARCHAR(30) NOT NULL CHECK (area IN ('HEMATOLOGIA','BIOQUIMICA','MICROBIOLOGIA','INMUNOLOGIA','UROANALISIS','COPROLOGIA')), " +
                "unidad VARCHAR(30), " +
                "rango_minimo NUMERIC(10,2), " +
                "rango_maximo NUMERIC(10,2), " +
                "rango_texto VARCHAR(100), " +
                "tiempo_proceso_min INT DEFAULT 60, " +
                "activo BOOLEAN DEFAULT true)");
            try {
                jdbcTemplate.execute("SELECT setval('maestras.examen_id_examen_seq', COALESCE((SELECT MAX(id_examen) FROM maestras.examen), 1))");
            } catch (Exception e) {
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS maestras.examen_id_examen_seq");
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
        insertPersonal(1, "70000001", "TERESA VIOLETA", "PEJERREY", "BRAVO", 1, "CMP-12345", "F");

        insertPersonal(2, "70000017", "VILMA", "ARIAS", "VIVANCO", 4, "CEP-20202", "F");

        insertPersonal(3, "70000009", "MARTHA SUSANA", "HUAYAYO", "", 3, "COP-33333", "F");

        insertPersonal(4, "70000007", "CARLOS FERNANDO", "VILVANTI", "RAMIREZ", 2, "COP-11111", "M");

        insertPersonal(5, "70000027", "CLAUDIA", "QUIROS", "ACEVEDO", 5, "CPP-34343", "F");

        insertPersonal(6, "70000028", "ELIZABETH JAZMIN", "SUAREZ", "INCA", 6, "CNP-45454", "F");
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

        log.info(">>> [SIGECLIN] Sembrando Familias Farmacológicas (PNUME I-3 - RM 220-2024/MINSA)...");
        // Las 13 familias existentes
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (1, 'N02B', 'Analgésicos y Antipiréticos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (2, 'J01C', 'Antibióticos Beta-lactámicos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (3, 'A02B', 'Medicamentos para Úlcera Péptica y ERGE', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (4, 'R03A', 'Adrenérgicos Inhalatorios (Asma/EPOC)', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (5, 'M01A', 'Antiinflamatorios no Esteroideos (AINEs)', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (6, 'C09A', 'Antihipertensivos (IECA/ARAII)', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (7, 'A10B', 'Antidiabéticos Orales', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (8, 'R06A', 'Antihistamínicos de Uso Sistémico', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (9, 'C10A', 'Hipolipemiantes (Estatinas)', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (10, 'P02C', 'Antiparasitarios', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (11, 'J05A', 'Antivirales', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (12, 'D01A', 'Antifúngicos Tópicos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (13, 'B03A', 'Antianémicos', true) ON CONFLICT DO NOTHING");
        // Nuevas familias para alcanzar 50 medicamentos reales
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (14, 'J01F', 'Macrólidos y Lincosamidas', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (15, 'J01M', 'Quinolonas Antibacterianas', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (16, 'J01X', 'Otros Antibacterianos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (17, 'H02A', 'Corticoides Sistémicos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (18, 'J02A', 'Antimicóticos Sistémicos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (19, 'C08C', 'Calcioantagonistas', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (20, 'C07A', 'Betabloqueantes', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (21, 'C03A', 'Diuréticos de Bajo Techo', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (22, 'N03A', 'Anticonvulsivantes', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (23, 'N05B', 'Ansiolíticos (Benzodiacepinas)', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (24, 'N06A', 'Antidepresivos', true) ON CONFLICT DO NOTHING");
        jdbcTemplate.execute("INSERT INTO maestras.familia_farmacologica (id_familia, codigo_atc, descripcion, activo) VALUES (25, 'A03A', 'Antiespasmódicos', true) ON CONFLICT DO NOTHING");

        log.info(">>> [SIGECLIN] Sembrando Catálogo de Medicamentos (50 medicamentos PNUME I-3 / RM 220-2024-MINSA)...");
        String[][] meds = {
            // 1-20: Mantenemos los existentes con algunos ajustes de presentación
            {"1","MED-001","PARACETAMOL","Paracetamol 500mg","1","Tableta","500mg","10","true","true"},
            {"2","MED-002","IBUPROFENO","Ibuprofeno 400mg","5","Tableta","400mg","10","true","true"},
            {"3","MED-003","AMOXICILINA","Amoxicilina 500mg","2","Cápsula","500mg","10","true","true"},
            {"4","MED-004","OMEPRAZOL","Omeprazol 20mg","3","Cápsula","20mg","10","true","true"},
            {"5","MED-005","SALBUTAMOL","Salbutamol Inhalador","4","Aerosol","100mcg/dosis","5","true","true"},
            {"6","MED-006","DICLOFENACO","Diclofenaco 75mg","5","Ampolla","75mg/3ml","10","true","true"},
            {"7","MED-007","LORATADINA","Loratadina 10mg","8","Tableta","10mg","10","true","true"},
            {"8","MED-008","NAPROXENO","Naproxeno 500mg","5","Tableta","500mg","10","true","true"},
            {"9","MED-009","ENALAPRIL","Enalapril 10mg","6","Tableta","10mg","10","true","true"},
            {"10","MED-010","METFORMINA","Metformina 850mg","7","Tableta","850mg","10","true","true"},
            {"11","MED-011","CETIRIZINA","Cetirizina 10mg","8","Tableta","10mg","10","true","true"},
            {"12","MED-012","ATORVASTATINA","Atorvastatina 20mg","9","Tableta","20mg","10","true","true"},
            {"13","MED-013","ALBENDAZOL","Albendazol 200mg","10","Tableta","200mg","10","true","true"},
            {"14","MED-014","ACICLOVIR","Aciclovir 200mg","11","Tableta","200mg","10","true","true"},
            {"15","MED-015","CLOTRIMAZOL","Clotrimazol Crema 1%","12","Crema","1%","5","true","true"},
            {"16","MED-016","SULFATO FERROSO","Sulfato Ferroso 200mg","13","Tableta","200mg","10","true","true"},
            {"17","MED-017","ACIDO FOLICO","Ácido Fólico 5mg","13","Tableta","5mg","10","true","true"},
            {"18","MED-018","METRONIDAZOL","Metronidazol 500mg","16","Tableta","500mg","10","true","true"},
            {"19","MED-019","AMOXICILINA/CLAVULANICO","Amoxicilina/Ác. Clavulánico","2","Tableta","500/125mg","10","true","true"},
            {"20","MED-020","LOSARTAN","Losartán 50mg","6","Tableta","50mg","10","true","true"},
            // 21-50: Nuevos medicamentos de alta demanda en I-3 (MINSA/ESSALUD)
            {"21","MED-021","METAMIZOL","Metamizol (Dipirona) 500mg","1","Tableta","500mg","10","true","true"},
            {"22","MED-022","TRAMADOL","Tramadol 50mg","1","Cápsula","50mg","10","true","true"},
            {"23","MED-023","ACIDO ACETILSALICILICO","Ácido Acetilsalicílico 100mg","1","Tableta","100mg","10","true","true"},
            {"24","MED-024","IBUPROFENO 600","Ibuprofeno 600mg","5","Tableta","600mg","10","true","true"},
            {"25","MED-025","NAPROXENO 250","Naproxeno 250mg","5","Tableta","250mg","10","true","true"},
            {"26","MED-026","DICLOFENACO TAB","Diclofenaco 50mg Tableta","5","Tableta","50mg","10","true","true"},
            {"27","MED-027","MELOXICAM","Meloxicam 15mg","5","Tableta","15mg","10","true","true"},
            {"28","MED-028","CEFALEXINA","Cefalexina 500mg","2","Cápsula","500mg","10","true","true"},
            {"29","MED-029","AZITROMICINA","Azitromicina 500mg","14","Tableta","500mg","10","true","true"},
            {"30","MED-030","CIPROFLOXACINO","Ciprofloxacino 500mg","15","Tableta","500mg","10","true","true"},
            {"31","MED-031","SULFAMETOXAZOL/TMP","Sulfametoxazol/Trimetoprima","16","Tableta","800/160mg","10","true","true"},
            {"32","MED-032","CLINDAMICINA","Clindamicina 300mg","14","Cápsula","300mg","10","true","true"},
            {"33","MED-033","NITROFURANTOINA","Nitrofurantoína 100mg","16","Cápsula","100mg","10","true","true"},
            {"34","MED-034","PREDNISONA 20","Prednisona 20mg","17","Tableta","20mg","10","true","true"},
            {"35","MED-035","PREDNISONA 5","Prednisona 5mg","17","Tableta","5mg","10","true","true"},
            {"36","MED-036","DEXAMETASONA TAB","Dexametasona 4mg","17","Tableta","4mg","10","true","true"},
            {"37","MED-037","FLUCONAZOL","Fluconazol 150mg","18","Cápsula","150mg","10","true","true"},
            {"38","MED-038","KETOCONAZOL TAB","Ketoconazol 200mg","18","Tableta","200mg","10","true","true"},
            {"39","MED-039","AMLODIPINO","Amlodipino 5mg","19","Tableta","5mg","10","true","true"},
            {"40","MED-040","AMLODIPINO 10","Amlodipino 10mg","19","Tableta","10mg","10","true","true"},
            {"41","MED-041","ATENOLOL","Atenolol 50mg","20","Tableta","50mg","10","true","true"},
            {"42","MED-042","PROPRANOLOL","Propranolol 40mg","20","Tableta","40mg","10","true","true"},
            {"43","MED-043","HIDROCLOROTIAZIDA","Hidroclorotiazida 50mg","21","Tableta","50mg","10","true","true"},
            {"44","MED-044","FUROSEMIDA","Furosemida 40mg","21","Tableta","40mg","10","true","true"},
            {"45","MED-045","SIMVASTATINA","Simvastatina 20mg","9","Tableta","20mg","10","true","true"},
            {"46","MED-046","GLIBENCLAMIDA","Glibenclamida 5mg","7","Tableta","5mg","10","true","true"},
            {"47","MED-047","RANITIDINA","Ranitidina 150mg","3","Tableta","150mg","10","true","true"},
            {"48","MED-048","CARBAMAZEPINA","Carbamazepina 200mg","22","Tableta","200mg","10","true","true"},
            {"49","MED-049","DIAZEPAM","Diazepam 5mg","23","Tableta","5mg","10","true","true"},
            {"50","MED-050","AMITRIPTILINA","Amitriptilina 25mg","24","Tableta","25mg","10","true","true"}
        };
        for (String[] m : meds) {
            jdbcTemplate.execute("INSERT INTO maestras.catalogo_medicamentos (id_medicamento, codigo, nombre_generico, nombre_comercial, id_familia, presentacion, concentracion, stock_minimo, requiere_receta, activo) " +
                "VALUES (" + m[0] + ",'" + m[1] + "','" + m[2] + "','" + m[3].replace("'","''") + "'," + m[4] + ",'" + m[5] + "','" + m[6] + "'," + m[7] + "," + m[8] + "," + m[9] + ") ON CONFLICT DO NOTHING");
        }

        try {
            jdbcTemplate.execute("SELECT setval('maestras.via_administracion_id_via_administracion_seq', COALESCE((SELECT MAX(id_via_administracion) FROM maestras.via_administracion), 1))");
            jdbcTemplate.execute("SELECT setval('maestras.familia_farmacologica_id_familia_seq', COALESCE((SELECT MAX(id_familia) FROM maestras.familia_farmacologica), 1))");
            jdbcTemplate.execute("SELECT setval('maestras.catalogo_medicamentos_id_medicamento_seq', COALESCE((SELECT MAX(id_medicamento) FROM maestras.catalogo_medicamentos), 1))");
        } catch (Exception e) {
            log.warn("No se pudo sincronizar secuencias de tablas maestras: {}", e.getMessage());
        }
    }

    private void seedExamenes() {
        log.info(">>> [SIGECLIN] Sembrando Catálogo de Exámenes de Laboratorio (6 áreas I-3)...");
        String[][] examenes = {
            {"HEM-001","Hemoglobina","HEMATOLOGIA","g/dL","12","16",null,"30"},
            {"HEM-002","Hematocrito","HEMATOLOGIA","%","36","48",null,"30"},
            {"HEM-003","Leucocitos","HEMATOLOGIA","/mm³","5000","10000",null,"30"},
            {"HEM-004","Plaquetas","HEMATOLOGIA","/mm³","150000","450000",null,"30"},
            {"HEM-005","VSG","HEMATOLOGIA","mm/h","0","20",null,"45"},
            {"HEM-006","TP (Tiempo Protrombina)","HEMATOLOGIA","seg","11","14",null,"60"},
            {"HEM-007","TPT (Tiempo Tromboplastina)","HEMATOLOGIA","seg","25","35",null,"60"},
            {"BIO-001","Glucosa en ayunas","BIOQUIMICA","mg/dL","70","110",null,"45"},
            {"BIO-002","Urea","BIOQUIMICA","mg/dL","10","50",null,"45"},
            {"BIO-003","Creatinina","BIOQUIMICA","mg/dL","0.6","1.3",null,"45"},
            {"BIO-004","Colesterol Total","BIOQUIMICA","mg/dL","0","200",null,"45"},
            {"BIO-005","Triglicéridos","BIOQUIMICA","mg/dL","0","150",null,"45"},
            {"BIO-006","TGO (AST)","BIOQUIMICA","U/L","0","40",null,"45"},
            {"BIO-007","TGP (ALT)","BIOQUIMICA","U/L","0","41",null,"45"},
            {"BIO-008","Bilirrubina Total","BIOQUIMICA","mg/dL","0.3","1.2",null,"45"},
            {"BIO-009","Ácido Úrico","BIOQUIMICA","mg/dL","3.4","7.0",null,"45"},
            {"BIO-010","Proteínas Totales","BIOQUIMICA","g/dL","6.4","8.3",null,"45"},
            {"MIC-001","Examen de orina completo","MICROBIOLOGIA",null,null,null,"Físico-Químico y Sedimento","60"},
            {"MIC-002","Coproparasitológico","MICROBIOLOGIA",null,null,null,"Seriado 3 muestras","60"},
            {"MIC-003","Baciloscopía (BK)","MICROBIOLOGIA",null,null,null,"Tinción Ziehl-Neelsen","120"},
            {"MIC-004","Cultivo general","MICROBIOLOGIA",null,null,null,"Agar sangre/MacConkey","1440"},
            {"INM-001","Prueba rápida VIH","INMUNOLOGIA",null,null,null,"Reactivo/No Reactivo","30"},
            {"INM-002","VDRL (Sífilis)","INMUNOLOGIA",null,null,null,"Títulos","60"},
            {"INM-003","HBsAg (Hepatitis B)","INMUNOLOGIA",null,null,null,"Reactivo/No Reactivo","30"},
            {"INM-004","Anti-HCV (Hepatitis C)","INMUNOLOGIA",null,null,null,"Reactivo/No Reactivo","30"},
            {"INM-005","Prueba de embarazo","INMUNOLOGIA",null,null,null,"Reactivo/No Reactivo","15"},
            {"URO-001","Uroanálisis físico","UROANALISIS",null,null,null,"Color, Aspecto, Densidad","30"},
            {"URO-002","Uroanálisis químico","UROANALISIS",null,null,null,"pH, Glucosa, Proteínas, Cetonas","30"},
            {"URO-003","Sedimento urinario","UROANALISIS",null,null,null,"Células, Cilindros, Cristales","30"},
            {"COP-001","Coproparasitológico directo","COPROLOGIA",null,null,null,"Examen en fresco","45"},
            {"COP-002","Sangre oculta en heces","COPROLOGIA",null,null,null,"Método Guayaco","30"}
        };
        for (String[] e : examenes) {
            String codigo = e[0], nombre = e[1], area = e[2], unidad = e[3];
            String rMin = e[4], rMax = e[5], rTexto = e[6], tiempo = e[7];
            String sql = "INSERT INTO maestras.examen (codigo, nombre, area, unidad, rango_minimo, rango_maximo, rango_texto, tiempo_proceso_min, activo) " +
                "VALUES ('" + codigo + "','" + nombre.replace("'","''") + "','" + area + "'," +
                (unidad != null ? "'" + unidad + "'" : "NULL") + "," +
                (rMin != null ? rMin : "NULL") + "," +
                (rMax != null ? rMax : "NULL") + "," +
                (rTexto != null ? "'" + rTexto.replace("'","''") + "'" : "NULL") + "," +
                (tiempo != null ? tiempo : "60") + ",true) ON CONFLICT DO NOTHING";
            try { jdbcTemplate.execute(sql); } catch (Exception ex) { log.warn("Error insertando examen {}: {}", codigo, ex.getMessage()); }
        }
        try { jdbcTemplate.execute("SELECT setval('maestras.examen_id_examen_seq', COALESCE((SELECT MAX(id_examen) FROM maestras.examen), 1))"); }
        catch (Exception e) { log.warn("No se pudo sincronizar secuencia examen"); }
    }

    private void seedCatalogoCiex() {
        log.info(">>> [SIGECLIN] Sembrando Catálogo CIEx...");
        jdbcTemplate.execute("INSERT INTO maestras.catalogo_ciex (id_ciex, codigo, descripcion, tipo, activo) " +
            "VALUES (1, 'LAB-ORD', 'ORDEN DE LABORATORIO', 'LABORATORIO', true) ON CONFLICT (codigo) DO NOTHING");
    }

    private void seedLotes() {
        log.info(">>> [SIGECLIN] Sembrando Lotes de Medicamentos demo...");
        String[][] lotes = {
            {"1","LOTE-PCM-001","2027-12-31","500","500","100"},
            {"2","LOTE-IBU-001","2027-10-15","300","300","100"},
            {"3","LOTE-AMX-001","2027-08-20","200","200","100"},
            {"4","LOTE-OMP-001","2027-11-30","150","150","100"},
            {"5","LOTE-SLB-001","2027-09-10","100","100","100"},
            {"1","LOTE-PCM-002","2026-08-15","200","200","100"},
            {"5","LOTE-SLB-002","2026-07-01","50","5","100"}
        };
        for (String[] l : lotes) {
            String sql = "INSERT INTO clinico.lote_medicamento (id_medicamento, numero_lote, fecha_vencimiento, stock_inicial, stock_actual, id_usuario_registro) " +
                "VALUES (" + l[0] + ",'" + l[1] + "','" + l[2] + "'," + l[3] + "," + l[4] + "," + l[5] + ") ON CONFLICT DO NOTHING";
            try { jdbcTemplate.execute(sql); } catch (Exception ex) { log.warn("Error insertando lote {}: {}", l[1], ex.getMessage()); }
        }
        try { jdbcTemplate.execute("SELECT setval('clinico.lote_medicamento_id_lote_seq', COALESCE((SELECT MAX(id_lote) FROM clinico.lote_medicamento), 1))"); }
        catch (Exception e) { log.warn("No se pudo sincronizar secuencia lote"); }
    }
}
