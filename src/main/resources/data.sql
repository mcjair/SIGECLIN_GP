-- Semilla de datos para SIGECLIN

-- 1. Tipos de Documento
INSERT INTO filiacion.tipo_documento (codigo, descripcion, longitud_exacta, regex_validacion) VALUES
('DNI', 'DOCUMENTO NACIONAL DE IDENTIDAD', 8, '^\d{8}$'),
('CE', 'CARNÉ DE EXTRANJERÍA', 9, '^\d{9}$'),
('PAS', 'PASAPORTE', 12, '^[A-Z0-9]{5,12}$'),
('DIE', 'DOCUMENTO DE IDENTIDAD EXTRANJERO', 15, '^[A-Z0-9]{5,15}$'),
('S/DOC', 'SIN DOCUMENTO', 15, '^[A-Z0-9]{5,15}$'),
('CNV', 'CERTIFICADO DE NACIDO VIVO', 10, '^\d{10}$');

-- 2. Especialidades
INSERT INTO maestras.especialidad (codigo, descripcion, cupo_maximo_diario) VALUES
('MG', 'MEDICINA GENERAL', 24),
('OBST', 'OBSTETRICIA', 20),
('ODONT', 'ODONTOLOGÍA', 16),
('PSIC', 'PSICOLOGÍA', 12),
('NUTR', 'NUTRICIÓN', 12);

-- 3. Tipos de Personal
INSERT INTO maestras.tipo_personal (codigo, descripcion) VALUES
('MED', 'MÉDICO'),
('ENF', 'ENFERMERO'),
('OBS', 'OBSTETRA'),
('ADM', 'ADMINISTRATIVO');

-- 4. Persona base para el Admin
INSERT INTO filiacion.persona (id_tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo)
VALUES (1, '00000000', 'ADMINISTRADOR', 'SIGECLIN', 'SISTEMA', '1990-01-01', 'M');

-- 5. Usuario Admin (Password: admin)
INSERT INTO filiacion.usuario (id_usuario, username, password_hash, requiere_cambio_password)
VALUES (1, 'admin', '$2a$10$IyOrcQllIZvSP6OU7FWcLejKAm/n1dticcm0sFFtcmSgQJQzQ3rFG', false);

-- 6. Roles
INSERT INTO seguridad.rol (codigo, descripcion) VALUES 
('ADMIN', 'ADMINISTRADOR DEL SISTEMA'),
('ADMISION', 'PERSONAL DE ADMISIÓN'),
('CAJA', 'PERSONAL DE CAJA'),
('TRIAJE', 'PERSONAL DE TRIAJE'),
('ENFERMERIA', 'PERSONAL DE ENFERMERÍA'),
('MEDICO_GENERAL', 'MÉDICO GENERAL'),
('OBSTETRICIA', 'OBSTETRA'),
('ODONTOLOGIA', 'ODONTÓLOGO'),
('PSICOLOGIA', 'PSICÓLOGO'),
('NUTRICION', 'NUTRICIONISTA');

-- 7. Personas para Usuarios (ID 2 al 7)
INSERT INTO filiacion.persona (id_persona, id_tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo) VALUES
(2, 1, '11111111', 'JUAN', 'ADMISION', 'PERU', '1990-01-01', 'M'),
(3, 1, '22222222', 'MARIA', 'CAJA', 'PERU', '1992-05-05', 'F'),
(4, 1, '33333333', 'PEDRO', 'TRIAJE', 'PERU', '1985-10-10', 'M'),
(5, 1, '44444444', 'LUISA', 'MEDICO', 'PERU', '1980-03-03', 'F'),
(6, 1, '55555555', 'CARMEN', 'OBSTETRA', 'PERU', '1988-07-07', 'F'),
(7, 1, '66666666', 'ANA', 'ENFERMERA', 'PERU', '1995-12-12', 'F');

-- 8. Usuarios (Password: admin para todos - con hashes de sal única)
INSERT INTO filiacion.usuario (id_usuario, username, password_hash, requiere_cambio_password) VALUES
(2, 'admision', '$2a$10$.UCSYsMwPQw/SSI6bH3UYuWrheIXTv6FGVOGechrgyETWGsFDN22.', false),
(3, 'caja', '$2a$10$hXDz7mRW9Co/q1aZSecMTxOPpVkacDXGuD6Ji3NA7nt5Ve/aIdmdYm', false),
(4, 'triaje', '$2a$10$81UiC3iWTCZRW8gjez4ApePe/6IGD7kNQIhjhjL.dZg9xQe3d2fpO', false),
(5, 'medico', '$2a$10$/mo7Yq.wqj6GDtUUPdy3HeT8C/gLAAHRXLpSzrQ3mjRD8R.jEvHSu', false),
(6, 'obstetra', '$2a$10$bL4fBV2rTSKW4EkbSvwfyuuEFM0XSrdC3lAXjZOGHeGO.BSIILnIW', false),
(7, 'enfermera', '$2a$10$Pzo1jXbHqJ4HP5E/ccNI1O7.Zpp.k86PrlMVMBXFypsvv8FUpeJee', false);

-- 9. Asignar Roles
INSERT INTO seguridad.usuario_rol (id_usuario, id_rol) VALUES 
(1, 1), -- Admin
(2, 2), -- Admision
(3, 3), -- Caja
(4, 4), -- Triaje
(5, 6), -- Medico (MEDICO_GENERAL)
(6, 7), -- Obstetra
(7, 5); -- Enfermera (ENFERMERIA)

-- 10. Personal (Necesario para Consultas)
INSERT INTO filiacion.personal (id_personal, id_tipo_personal, id_especialidad, id_usuario, numero_colegiatura, fecha_ingreso) VALUES
(5, 1, 1, 5, 'CMP-12345', '2020-01-01'), -- Medico
(6, 3, 2, 6, 'COP-54321', '2020-01-01'); -- Obstetra
