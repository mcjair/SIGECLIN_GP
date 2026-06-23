-- ============================================================
-- SISTEMA SIGECLIN - DDL COMPLETO
-- ============================================================

CREATE SCHEMA IF NOT EXISTS filiacion;
CREATE SCHEMA IF NOT EXISTS maestras;
CREATE SCHEMA IF NOT EXISTS clinico;
CREATE SCHEMA IF NOT EXISTS seguridad;

-- ============================================================
-- ESQUEMA: filiacion
-- ============================================================

CREATE TABLE filiacion.tipo_documento (
    id_tipo_documento SERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(100) NOT NULL,
    longitud_exacta INT,
    regex_validacion VARCHAR(255) NOT NULL,
    requiere_digito_verificacion BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE filiacion.persona (
    id_persona SERIAL PRIMARY KEY,
    id_tipo_documento INT NOT NULL REFERENCES filiacion.tipo_documento(id_tipo_documento),
    numero_documento VARCHAR(20) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50),
    fecha_nacimiento DATE NOT NULL,
    sexo CHAR(1) CHECK (sexo IN ('M', 'F')),
    telefono_principal VARCHAR(15),
    telefono_secundario VARCHAR(15),
    correo_electronico VARCHAR(100),
    id_ubigeo_nacimiento INT,
    id_ubigeo_residencia INT,
    direccion VARCHAR(255),
    fotografia BYTEA,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    UNIQUE (id_tipo_documento, numero_documento)
);

CREATE INDEX idx_persona_documento ON filiacion.persona(id_tipo_documento, numero_documento);
CREATE INDEX idx_persona_nombres ON filiacion.persona(apellido_paterno, apellido_materno, nombres);

CREATE TABLE filiacion.paciente (
    id_paciente INT PRIMARY KEY REFERENCES filiacion.persona(id_persona),
    numero_historia_clinica VARCHAR(20) UNIQUE NOT NULL,
    grupo_sanguineo VARCHAR(3),
    factor_rh VARCHAR(1),
    contacto_emergencia_nombre VARCHAR(100),
    contacto_emergencia_telefono VARCHAR(15),
    contacto_emergencia_parentesco VARCHAR(30),
    estado_civil VARCHAR(20),
    ocupacion VARCHAR(100),
    etnia VARCHAR(50),
    id_tipo_seguro INT,
    fecha_fallecimiento DATE,
    estado VARCHAR(20) DEFAULT 'activo',
    servicio_solicitado VARCHAR(50),
    referencia_direccion VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE filiacion.usuario (
    id_usuario INT PRIMARY KEY REFERENCES filiacion.persona(id_persona),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    cuenta_bloqueada BOOLEAN DEFAULT false,
    intentos_fallidos INT DEFAULT 0,
    fecha_ultimo_acceso TIMESTAMP,
    fecha_cambio_password TIMESTAMP,
    sesion_activa BOOLEAN DEFAULT false,
    requiere_cambio_password BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE filiacion.personal (
    id_personal INT PRIMARY KEY REFERENCES filiacion.persona(id_persona),
    id_tipo_personal INT NOT NULL,
    id_especialidad INT,
    id_usuario INT REFERENCES filiacion.usuario(id_usuario),
    numero_colegiatura VARCHAR(20),
    fecha_ingreso DATE NOT NULL,
    fecha_cese DATE,
    estado_laboral VARCHAR(20) DEFAULT 'activo',
    horario JSONB,
    firma_digital BYTEA,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ESQUEMA: maestras
-- ============================================================

CREATE TABLE maestras.tipo_personal (
    id_tipo_personal SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.especialidad (
    id_especialidad SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    cupo_maximo_diario INT NOT NULL DEFAULT 20,
    duracion_promedio_min INT DEFAULT 15,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.servicio (
    id_servicio SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) CHECK (tipo IN ('CONSULTA', 'APOYO_DIAGNOSTICO', 'ADMINISTRATIVO'))
);

CREATE TABLE maestras.cie10 (
    codigo VARCHAR(10) PRIMARY KEY,
    descripcion TEXT NOT NULL,
    categoria VARCHAR(50),
    subcategoria VARCHAR(50),
    capitulo VARCHAR(10),
    servicios VARCHAR(255),
    activo BOOLEAN DEFAULT true
);

CREATE INDEX idx_cie10_codigo ON maestras.cie10(codigo);
CREATE INDEX idx_cie10_descripcion ON maestras.cie10 USING gin(to_tsvector('spanish', descripcion));

CREATE TABLE maestras.catalogo_ciex (
    id_ciex SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion TEXT NOT NULL,
    tipo VARCHAR(30),
    id_especialidad INT REFERENCES maestras.especialidad(id_especialidad),
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.familia_farmacologica (
    id_familia SERIAL PRIMARY KEY,
    codigo_atc VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.catalogo_medicamentos (
    id_medicamento SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nombre_generico VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    id_familia INT NOT NULL REFERENCES maestras.familia_farmacologica(id_familia),
    presentacion VARCHAR(100),
    concentracion VARCHAR(100),
    id_unidad_medida INT,
    stock_minimo INT DEFAULT 10,
    requiere_receta BOOLEAN DEFAULT true,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.alergia_tipo (
    id_alergia_tipo SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    severidad_base VARCHAR(20)
);

CREATE TABLE maestras.via_administracion (
    id_via_administracion SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(50) NOT NULL
);

CREATE TABLE maestras.unidad_medida (
    id_unidad_medida SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(50) NOT NULL,
    abreviatura VARCHAR(10)
);

CREATE TABLE maestras.ubigeo (
    id_ubigeo SERIAL PRIMARY KEY,
    codigo VARCHAR(6) UNIQUE NOT NULL,
    departamento VARCHAR(100) NOT NULL,
    provincia VARCHAR(100) NOT NULL,
    distrito VARCHAR(100) NOT NULL
);

CREATE TABLE maestras.tipo_seguro (
    id_tipo_seguro SERIAL PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.examen (
    id_examen SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    area VARCHAR(30) NOT NULL CHECK (area IN ('HEMATOLOGIA','BIOQUIMICA','MICROBIOLOGIA','INMUNOLOGIA','UROANALISIS','COPROLOGIA')),
    unidad VARCHAR(30),
    rango_minimo NUMERIC(10,2),
    rango_maximo NUMERIC(10,2),
    rango_texto VARCHAR(100),
    tiempo_proceso_min INT DEFAULT 60,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE maestras.catalogo_vacunas (
    id_vacuna SERIAL PRIMARY KEY,
    codigo VARCHAR(20) UNIQUE NOT NULL,
    descripcion VARCHAR(200) NOT NULL,
    dosis_recomendada INT,
    edad_aplicacion_meses INT,
    activo BOOLEAN DEFAULT true
);

-- ============================================================
-- ESQUEMA: clinico
-- ============================================================

CREATE TABLE clinico.alergia_paciente (
    id_alergia_paciente SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_alergia_tipo INT NOT NULL REFERENCES maestras.alergia_tipo(id_alergia_tipo),
    id_familia INT REFERENCES maestras.familia_farmacologica(id_familia),
    id_medicamento INT REFERENCES maestras.catalogo_medicamentos(id_medicamento),
    descripcion VARCHAR(255),
    severidad VARCHAR(20),
    fecha_diagnostico DATE,
    activa BOOLEAN DEFAULT true,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alergia_paciente ON clinico.alergia_paciente(id_paciente, activa);

CREATE TABLE clinico.antecedente_paciente (
    id_antecedente SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    tipo VARCHAR(30) CHECK (tipo IN ('FAMILIAR', 'PERSONAL', 'QUIRURGICO', 'HOSPITALARIO')),
    descripcion TEXT NOT NULL,
    fecha_registro DATE DEFAULT CURRENT_DATE
);

CREATE TABLE clinico.cita (
    id_cita SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_especialidad INT NOT NULL REFERENCES maestras.especialidad(id_especialidad),
    id_personal INT REFERENCES filiacion.personal(id_personal),
    fecha_hora_programada TIMESTAMP NOT NULL,
    fecha_hora_llegada TIMESTAMP,
    fecha_hora_atencion TIMESTAMP,
    fecha_hora_fin TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'programada',
    motivo VARCHAR(255),
    origen VARCHAR(20) DEFAULT 'presencial',
    prioridad VARCHAR(10) DEFAULT 'normal',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_paciente, fecha_hora_programada, id_especialidad)
);

CREATE INDEX idx_cita_fecha ON clinico.cita(fecha_hora_programada, estado);

CREATE TABLE clinico.triaje (
    id_triaje SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_cita INT REFERENCES clinico.cita(id_cita),
    id_usuario INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    presion_arterial_sistolica INT,
    presion_arterial_diastolica INT,
    frecuencia_cardiaca INT,
    frecuencia_respiratoria INT,
    temperatura NUMERIC(4,1),
    saturacion_oxigeno INT,
    peso_kg NUMERIC(5,2),
    talla_cm NUMERIC(5,2),
    imc NUMERIC(5,2) GENERATED ALWAYS AS (peso_kg / POWER(talla_cm/100, 2)) STORED,
    clasificacion_nutricional VARCHAR(20) GENERATED ALWAYS AS (
        CASE
            WHEN (peso_kg / POWER(talla_cm/100, 2)) < 18.5 THEN 'bajo_peso'
            WHEN (peso_kg / POWER(talla_cm/100, 2)) BETWEEN 18.5 AND 24.99 THEN 'normal'
            WHEN (peso_kg / POWER(talla_cm/100, 2)) BETWEEN 25 AND 29.99 THEN 'sobrepeso'
            ELSE 'obesidad'
        END
    ) STORED,
    clasificacion_urgencia VARCHAR(10) CHECK (clasificacion_urgencia IN ('rojo', 'naranja', 'amarillo', 'verde')),
    servicio_destino VARCHAR(50),
    checklist_sintomas JSONB,
    observaciones TEXT
);

CREATE INDEX idx_triaje_paciente ON clinico.triaje(id_paciente, fecha_hora DESC);

CREATE TABLE clinico.consulta (
    id_consulta SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_cita INT REFERENCES clinico.cita(id_cita),
    id_triaje INT REFERENCES clinico.triaje(id_triaje),
    id_personal INT NOT NULL REFERENCES filiacion.personal(id_personal),
    id_especialidad INT NOT NULL REFERENCES maestras.especialidad(id_especialidad),
    fecha_hora_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_hora_fin TIMESTAMP,
    tipo_consulta VARCHAR(30) DEFAULT 'presencial',
    motivo_consulta TEXT NOT NULL,
    anamnesis TEXT,
    examen_fisico TEXT,
    plan_tratamiento TEXT,
    proximo_control DATE,
    estado VARCHAR(20) DEFAULT 'en_progreso'
);

CREATE INDEX idx_consulta_paciente ON clinico.consulta(id_paciente, fecha_hora_inicio DESC);

CREATE TABLE clinico.diagnostico_consulta (
    id_diagnostico SERIAL PRIMARY KEY,
    id_consulta INT NOT NULL REFERENCES clinico.consulta(id_consulta),
    codigo_cie10 VARCHAR(10) NOT NULL REFERENCES maestras.cie10(codigo),
    tipo VARCHAR(20) DEFAULT 'PRESUNTIVO',
    observaciones TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinico.receta_medica (
    id_receta SERIAL PRIMARY KEY,
    id_consulta INT NOT NULL REFERENCES clinico.consulta(id_consulta),
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_personal INT NOT NULL REFERENCES filiacion.personal(id_personal),
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'emitida',
    indicaciones_generales TEXT,
    fecha_proxima_revision DATE
);

CREATE TABLE clinico.detalle_receta (
    id_detalle SERIAL PRIMARY KEY,
    id_receta INT NOT NULL REFERENCES clinico.receta_medica(id_receta),
    id_medicamento INT NOT NULL REFERENCES maestras.catalogo_medicamentos(id_medicamento),
    dosis VARCHAR(50) NOT NULL,
    frecuencia VARCHAR(50) NOT NULL,
    duracion_dias INT NOT NULL,
    cantidad_total INT NOT NULL,
    id_via_administracion INT NOT NULL REFERENCES maestras.via_administracion(id_via_administracion),
    indicaciones_adicionales TEXT,
    estado_dispensacion VARCHAR(20) DEFAULT 'pendiente'
);

CREATE TABLE clinico.orden_medica (
    id_orden SERIAL PRIMARY KEY,
    id_consulta INT NOT NULL REFERENCES clinico.consulta(id_consulta),
    id_ciex INT NOT NULL REFERENCES maestras.catalogo_ciex(id_ciex),
    id_personal_solicitante INT NOT NULL REFERENCES filiacion.personal(id_personal),
    fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_resultado TIMESTAMP,
    id_personal_ejecutor INT REFERENCES filiacion.personal(id_personal),
    tipo VARCHAR(20) CHECK (tipo IN ('LABORATORIO', 'IMAGENES', 'PROCEDIMIENTO')),
    estado VARCHAR(20) DEFAULT 'solicitada',
    indicaciones TEXT,
    resultado_texto TEXT,
    resultado_archivo BYTEA,
    urgente BOOLEAN DEFAULT false
);

CREATE TABLE clinico.resultado_laboratorio (
    id_resultado SERIAL PRIMARY KEY,
    id_orden INT NOT NULL REFERENCES clinico.orden_medica(id_orden),
    codigo_examen VARCHAR(20) NOT NULL,
    valor_resultado VARCHAR(50),
    unidad VARCHAR(20),
    rango_minimo NUMERIC(10,2),
    rango_maximo NUMERIC(10,2),
    es_anormal BOOLEAN,
    fecha_procesamiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinico.lote_medicamento (
    id_lote SERIAL PRIMARY KEY,
    id_medicamento INT NOT NULL REFERENCES maestras.catalogo_medicamentos(id_medicamento),
    numero_lote VARCHAR(50) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    stock_inicial INT NOT NULL,
    stock_actual INT NOT NULL,
    fecha_ingreso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_usuario_registro INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    UNIQUE (id_medicamento, numero_lote)
);

CREATE INDEX idx_lote_vencimiento ON clinico.lote_medicamento(fecha_vencimiento);
CREATE INDEX idx_lote_stock ON clinico.lote_medicamento(stock_actual);

CREATE TABLE clinico.dispensacion (
    id_dispensacion SERIAL PRIMARY KEY,
    id_detalle_receta INT NOT NULL REFERENCES clinico.detalle_receta(id_detalle),
    id_lote INT NOT NULL REFERENCES clinico.lote_medicamento(id_lote),
    id_usuario INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    cantidad_entregada INT NOT NULL,
    fecha_dispensacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    observaciones TEXT
);

CREATE TABLE clinico.inmunizacion (
    id_inmunizacion SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_vacuna INT NOT NULL REFERENCES maestras.catalogo_vacunas(id_vacuna),
    id_lote INT REFERENCES clinico.lote_medicamento(id_lote),
    id_personal INT NOT NULL REFERENCES filiacion.personal(id_personal),
    fecha_aplicacion DATE NOT NULL,
    numero_dosis INT NOT NULL,
    lugar_aplicacion VARCHAR(50),
    fecha_proxima_dosis DATE,
    observaciones TEXT
);

CREATE TABLE clinico.atencion_preventiva (
    id_atencion_preventiva SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_personal INT NOT NULL REFERENCES filiacion.personal(id_personal),
    tipo_programa VARCHAR(30) CHECK (tipo_programa IN (
        'CRED', 'PLANIFICACION_FAMILIAR', 'MATERNO_PERINATAL',
        'TUBERCULOSIS', 'ITS_VIH', 'SALUD_MENTAL', 'NUTRICION',
        'SALUD_BUCAL', 'INMUNIZACIONES', 'CANCER'
    )),
    fecha_atencion DATE NOT NULL,
    descripcion_actividad TEXT,
    resultado VARCHAR(50),
    observaciones TEXT
);

CREATE TABLE clinico.pago_log (
    id_pago SERIAL PRIMARY KEY,
    id_cita INT REFERENCES clinico.cita(id_cita),
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_usuario INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    monto NUMERIC(10,2) NOT NULL,
    tipo_pago VARCHAR(20) CHECK (tipo_pago IN ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA', 'EXONERADO')),
    concepto VARCHAR(100),
    numero_comprobante VARCHAR(50),
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinico.auditoria_acceso (
    id_auditoria SERIAL PRIMARY KEY,
    usuario VARCHAR(50) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    detalle TEXT,
    ip_origen VARCHAR(45),
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_paciente_relacionado INT REFERENCES filiacion.paciente(id_paciente)
);

-- ============================================================
-- ESQUEMA: seguridad
-- ============================================================

CREATE TABLE seguridad.rol (
    id_rol SERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    descripcion VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT true
);

CREATE TABLE seguridad.usuario_rol (
    id_usuario INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    id_rol INT NOT NULL REFERENCES seguridad.rol(id_rol),
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_usuario_asignador INT REFERENCES filiacion.usuario(id_usuario),
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE TABLE seguridad.logs_auditoria (
    id_log BIGSERIAL,
    id_usuario INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    id_paciente_consultado INT REFERENCES filiacion.paciente(id_paciente),
    tabla_afectada VARCHAR(50) NOT NULL,
    id_registro_afectado INT,
    accion VARCHAR(20) CHECK (accion IN ('INSERT', 'UPDATE', 'DELETE', 'SELECT_HC')),
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    ip_origen VARCHAR(45),
    user_agent VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_log, timestamp)
) PARTITION BY RANGE (timestamp);

CREATE TABLE seguridad.sesion_log (
    id_sesion SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES filiacion.usuario(id_usuario),
    token_sesion VARCHAR(255) UNIQUE NOT NULL,
    ip_origen VARCHAR(45),
    fecha_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_ultima_actividad TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre TIMESTAMP,
    cerrada_por VARCHAR(30) CHECK (cerrada_por IN ('LOGOUT', 'TIMEOUT', 'SISTEMA')),
    duracion_segundos INT
);

CREATE TABLE seguridad.configuracion_sistema (
    id_config SERIAL PRIMARY KEY,
    clave VARCHAR(50) UNIQUE NOT NULL,
    valor TEXT NOT NULL,
    descripcion VARCHAR(255),
    modificable BOOLEAN DEFAULT true
);

-- ============================================================
-- VISTA MATERIALIZADA
-- ============================================================

CREATE MATERIALIZED VIEW clinico.vw_historia_clinica AS
SELECT
    p.id_paciente,
    per.nombres,
    per.apellido_paterno,
    per.apellido_materno,
    td.codigo AS tipo_documento,
    per.numero_documento,
    p.numero_historia_clinica,
    p.grupo_sanguineo,
    p.contacto_emergencia_nombre,
    p.contacto_emergencia_telefono,
    t.clasificacion_urgencia,
    t.presion_arterial_sistolica,
    t.presion_arterial_diastolica,
    t.frecuencia_cardiaca,
    t.temperatura,
    t.saturacion_oxigeno,
    t.imc,
    t.clasificacion_nutricional,
    EXISTS(SELECT 1 FROM clinico.alergia_paciente ap WHERE ap.id_paciente = p.id_paciente AND ap.activa = true) AS tiene_alergias_activas,
    (SELECT MAX(c.fecha_hora_inicio) FROM clinico.consulta c WHERE c.id_paciente = p.id_paciente) AS ultima_consulta
FROM filiacion.paciente p
JOIN filiacion.persona per ON p.id_paciente = per.id_persona
JOIN filiacion.tipo_documento td ON per.id_tipo_documento = td.id_tipo_documento
LEFT JOIN LATERAL (
    SELECT * FROM clinico.triaje t2
    WHERE t2.id_paciente = p.id_paciente
    ORDER BY t2.fecha_hora DESC LIMIT 1
) t ON true;

CREATE UNIQUE INDEX idx_vw_hc_paciente ON clinico.vw_historia_clinica(id_paciente);

-- ============================================================
-- TABLAS DE AUDITORIA FORENSE (HIBERNATE ENVERS)
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS public.revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS public.revinfo (
    rev INTEGER NOT NULL,
    revtstmp BIGINT,
    PRIMARY KEY (rev)
);

CREATE TABLE IF NOT EXISTS clinico.consulta_aud (
    id_consulta INT NOT NULL,
    rev INT NOT NULL REFERENCES public.revinfo(rev),
    revtype SMALLINT,
    id_paciente INT,
    id_cita INT,
    id_triaje INT,
    id_personal INT,
    id_especialidad INT,
    fecha_hora_inicio TIMESTAMP,
    fecha_hora_fin TIMESTAMP,
    tipo_consulta VARCHAR(30),
    motivo_consulta TEXT,
    anamnesis TEXT,
    examen_fisico TEXT,
    plan_tratamiento TEXT,
    proximo_control DATE,
    estado VARCHAR(20),
    PRIMARY KEY (id_consulta, rev)
);

CREATE TABLE IF NOT EXISTS clinico.triaje_aud (
    id_triaje INT NOT NULL,
    rev INT NOT NULL REFERENCES public.revinfo(rev),
    revtype SMALLINT,
    id_paciente INT,
    id_cita INT,
    id_usuario INT,
    fecha_hora TIMESTAMP,
    presion_arterial_sistolica INT,
    presion_arterial_diastolica INT,
    frecuencia_cardiaca INT,
    frecuencia_respiratoria INT,
    temperatura NUMERIC(4,1),
    saturacion_oxigeno INT,
    peso_kg NUMERIC(5,2),
    talla_cm NUMERIC(5,2),
    clasificacion_urgencia VARCHAR(10),
    servicio_destino VARCHAR(50),
    checklist_sintomas JSONB,
    observaciones TEXT,
    PRIMARY KEY (id_triaje, rev)
);

CREATE TABLE IF NOT EXISTS clinico.receta_medica_aud (
    id_receta INT NOT NULL,
    rev INT NOT NULL REFERENCES public.revinfo(rev),
    revtype SMALLINT,
    id_consulta INT,
    id_paciente INT,
    id_personal INT,
    fecha_emision TIMESTAMP,
    estado VARCHAR(20),
    indicaciones_generales TEXT,
    fecha_proxima_revision DATE,
    PRIMARY KEY (id_receta, rev)
);

CREATE TABLE IF NOT EXISTS clinico.detalle_receta_aud (
    id_detalle INT NOT NULL,
    rev INT NOT NULL REFERENCES public.revinfo(rev),
    revtype SMALLINT,
    id_receta INT,
    id_medicamento INT,
    dosis VARCHAR(50),
    frecuencia VARCHAR(50),
    duracion_dias INT,
    cantidad_total INT,
    id_via_administracion INT,
    indicaciones_adicionales TEXT,
    estado_dispensacion VARCHAR(20),
    PRIMARY KEY (id_detalle, rev)
);

CREATE TABLE IF NOT EXISTS clinico.diagnostico_consulta_aud (
    id_diagnostico INT NOT NULL,
    rev INT NOT NULL REFERENCES public.revinfo(rev),
    revtype SMALLINT,
    id_consulta INT,
    codigo_cie10 VARCHAR(10),
    tipo VARCHAR(20),
    observaciones TEXT,
    fecha_registro TIMESTAMP,
    PRIMARY KEY (id_diagnostico, rev)
);
