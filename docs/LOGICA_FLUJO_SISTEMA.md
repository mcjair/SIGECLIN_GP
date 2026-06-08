# SIGECLIN — Lógica y Flujo del Sistema

**Versión:** v0.0.3  
**Arquitectura:** Monolito Spring Boot MVC con PostgreSQL  
**Stack:** Java 17, Spring Boot 3.2.5, Spring Security, Thymeleaf, Bootstrap 5.3.2, H2 (tests)

---

## Índice

1. [Arquitectura General](#1-arquitectura-general)
2. [Esquemas de Base de Datos](#2-esquemas-de-base-de-datos)
3. [Modelo de Datos (Entidades)](#3-modelo-de-datos-entidades)
4. [Roles y Seguridad](#4-roles-y-seguridad)
5. [Flujo Completo del Paciente](#5-flujo-completo-del-paciente)
6. [Dashboard y Estadísticas](#6-dashboard-y-estadísticas)
7. [Servicios de Apoyo](#7-servicios-de-apoyo)
8. [Inicialización del Sistema](#8-inicialización-del-sistema)
9. [Manejo de Errores](#9-manejo-de-errores)
10. [Workarounds y Decisiones Técnicas](#10-workarounds-y-decisiones-técnicas)
11. [Estructura del Proyecto](#11-estructura-del-proyecto)

---

## 1. Arquitectura General

```
┌─────────────────────────────────────────────────────────────┐
│                   SIGECLIN (Spring Boot 3.2.5)              │
├─────────────────────────────────────────────────────────────┤
│  Controladores (MVC + REST)                ┌──────────────┐│
│  ┌─ MainController           ┌─ Cie10RestController      │││
│  │─ PacienteController       │─ ServicioController       │││
│  │─ GestionPacienteController│─ HistoriaClinicaController│││
│  │─ PersonalController       │─ ApoyoDiagnosticoController│││
│  │─ TriajeController         └──────────────────────────┘││
│  │─ ConsultaController                                    ││
│  │─ CajaController                                        ││
│  └────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│  Servicios (Interfaces + Implementaciones)                  │
│  ┌─ IPacienteService / PacienteService                     ││
│  │─ IPersonalService / PersonalService                     ││
│  │─ ITriajeService  / TriajeService                        ││
│  │─ IConsultaService / ConsultaService                     ││
│  │─ IRecetaService  / RecetaService                        ││
│  │─ IHistoriaClinicaService / HistoriaClinicaService       ││
│  │─ IAuditoriaService / AuditoriaService                   ││
│  │─ IApoyoDiagnosticoService / ApoyoDiagnosticoService     ││
│  │─ IDashboardService / DashboardService                   ││
│  │─ ICie10Service   / Cie10Service                         ││
│  │─ IMaestrasService / MaestrasService                     ││
│  │─ CustomUserDetailsService (Spring Security)             ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│  Repositorios (Spring Data JPA + Queries nativas)           │
│  ┌─ PacienteRepository  ── TriajeRepository               ││
│  │─ PersonalRepository  ── ConsultaRepository             ││
│  │─ UsuarioRepository   ── RecetaRepository               ││
│  │─ TipoDocumentoRepository ── DetalleRecetaRepository     ││
│  │─ RolRepository          ── AlergiaPacienteRepository   ││
│  │─ Cie10Repository       ── AuditoriaRepository          ││
│  │─ MedicamentoRepository ── ServicioRepository           ││
│  └────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│  Base de Datos: PostgreSQL 18 (dev) / H2 (tests)            │
│  ┌─ filiacion ── seguridad ── clinico ── maestras          ││
│  └────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

**Patrón:** Controller → Service (interface/impl) → Repository → JPA/Entity  
**Templates:** Thymeleaf con layout compartido (`layout.html`)  
**Estilo:** Bootstrap 5.3.2 con glassmorphism premium personalizado  
**Seguridad:** Spring Security + BCrypt + bloqueo de cuenta tras 5 intentos fallidos

---

## 2. Esquemas de Base de Datos

| Esquema | Propósito |
|---------|-----------|
| `filiacion` | Afiliación — personas, pacientes, personal, usuarios, tipo_documento |
| `seguridad` | Seguridad — roles, usuario_rol, sesión |
| `clinico` | Clínico — triaje, consulta, recetas, diagnósticos, alergias, auditoría, pagos |
| `maestras` | Maestros — CIE-10, medicamentos, servicios, vías administración, familias |

> **Nota:** `ddl-auto=none` — el esquema se crea manualmente vía scripts SQL.  
> `hibernate.hbm2ddl.create_namespaces=true` solo en tests con H2.

---

## 3. Modelo de Datos (Entidades)

### 3.1 Jerarquía Persona (Herencia JOINED)

```
Persona (base) ←── Paciente
Persona (base) ←── Personal
Persona (base) ←── Usuario

Cada subclase comparte el PK de Persona via @PrimaryKeyJoinColumn.
Tablas en esquema `filiacion`.
```

### 3.2 filiacion

| Tabla | Entidad | Campos clave |
|-------|---------|--------------|
| `persona` | `Persona` | id_persona (PK), id_tipo_documento (FK), numero_documento, nombres, apellidos, fecha_nacimiento, sexo, fotografía |
| `paciente` | `Paciente` (extends Persona) | numero_historia_clinica (=numeroDocumento), servicio_solicitado, estado (máquina de estados), contacto_emergencia |
| `personal` | `Personal` (extends Persona) | id_tipo_personal, id_especialidad, id_usuario (FK), numero_colegiatura, horario (JSON), firma_digital |
| `usuario` | `Usuario` (extends Persona) | username (único), password_hash (BCrypt), cuenta_bloqueada, intentos_fallidos, sesion_activa |
| `tipo_documento` | `TipoDocumento` | codigo (DNI/CE/PAS), regex_validador |

### 3.3 seguridad

| Tabla | Entidad | Campos clave |
|-------|---------|--------------|
| `rol` | `Rol` | codigo (ADMIN, MEDICO_GENERAL, etc.) |
| `usuario_rol` | Join Table | id_usuario (FK), id_rol (FK) |

### 3.4 clinico

| Tabla | Entidad | Campos clave |
|-------|---------|--------------|
| `triaje` | `Triaje` | signos_vitales, imc (computado), clasificacion_urgencia (CHECK: rojo/naranja/amarillo/verde), servicio_destino, alerta_clinica |
| `consulta` | `Consulta` | paciente, triaje, médico, motivo, anamnesis, examen_físico, plan, estado |
| `diagnostico_consulta` | `DiagnosticoConsulta` | consulta (FK), cie10 (FK), tipo (PRESUNTIVO/DEFINITIVO) |
| `receta_medica` | `RecetaMedica` | consulta (FK), paciente, médico, estado, indicaciones |
| `detalle_receta` | `DetalleReceta` | receta (FK), medicamento (FK), dosis, frecuencia, duración, cantidad |
| `alergia_paciente` | `AlergiaPaciente` | paciente (FK), medicamento (FK), severidad, activa |
| `auditoria_acceso` | `AuditoriaAcceso` | usuario, acción, detalle, IP, id_paciente_relacionado |
| `pago_log` | (JDBC directo) | id_paciente, id_usuario, monto, tipo_pago, concepto, comprobante |

### 3.5 maestras

| Tabla | Entidad | Campos clave |
|-------|---------|--------------|
| `cie10` | `Cie10` | codigo (PK), descripcion, categoria, subcategoria, capitulo, servicios |
| `catalogo_medicamentos` | `Medicamento` | nombre_generico, concentracion, presentacion |
| `servicio` | `Servicio` | nombre, activo, icono |

---

## 4. Roles y Seguridad

### 4.1 Roles del Sistema

| Rol | Código | Acceso |
|-----|--------|--------|
| Administrador | `ADMIN` | Todo el sistema |
| Médico General | `MEDICO_GENERAL` | Consulta, dashboard, Personal (lectura) |
| Enfermería | `ENFERMERIA` | Triaje, dashboard, Personal (lectura) |
| Obstetricia | `OBSTETRICIA` | Consulta módulo propio |
| Odontología | `ODONTOLOGIA` | Consulta módulo propio |
| Psicología | `PSICOLOGIA` | Consulta módulo propio |
| Nutrición | `NUTRICION` | Consulta módulo propio |

### 4.2 Seguridad por Capas

1. **Form Login** — Spring Security con página `/login` personalizada
2. **Rutas públicas** — `/`, `/login`, `/error`, `/css/**`, `/js/**`, `/webjars/**`, `/api/cie10/**`
3. **Autenticación requerida** — todo lo demás
4. **`@PreAuthorize`** — en métodos de `PersonalController`
5. **Sesión única** — máximo 1 sesión por usuario (evita reuso)
6. **Bloqueo de cuenta** — tras 5 intentos fallidos (configurable en `CustomUserDetailsService.MAX_INTENTOS_FALLIDOS = 5`)
7. **CSRF** — habilitado global, deshabilitado solo para `POST /consulta/guardar`
8. **Password** — BCrypt, requiere cambio en primer login (`requiereCambioPassword = true`)

### 4.3 Usuarios Semilla

| Usuario | Password | Rol |
|---------|----------|-----|
| `admin` | `admin` | ADMIN |
| `medicina` | `admin` | MEDICO_GENERAL |
| `enfermeria` | `admin` | ENFERMERIA |
| `obstetricia` | `admin` | OBSTETRICIA |
| `odontologia` | `admin` | ODONTOLOGIA |
| `psicologia` | `admin` | PSICOLOGIA |
| `nutricion` | `admin` | NUTRICION |

---

## 5. Flujo Completo del Paciente

### Máquina de Estados del Paciente

```
PENDIENTE_PAGO ──(pago)──> PENDIENTE_TRIAJE ──(triaje)──> PENDIENTE_CONSULTA ──(consulta)──> ATENDIDO
```

### Paso 1: Login

- **URL:** `GET /login`
- **Controlador:** `MainController.login()`
- **Template:** `login.html`
- **Auth:** Spring Security + `CustomUserDetailsService.loadUserByUsername()`
- **Post-login:** redirige a `GET /dashboard`
- **Seguridad:** cuenta se bloquea tras 5 intentos fallidos

### Paso 2: Dashboard

- **URL:** `GET /` o `GET /dashboard`
- **Controlador:** `MainController.dashboard()` → `DashboardService.cargarDatosDashboard()`
- **Template:** `dashboard.html`
- **Datos cargados:**
  - Total pacientes, personal, atenciones hoy, ingresos hoy
  - Espera promedio y eficiencia
  - Cola de espera (top 5 pacientes)
  - Conteo por servicio (MEDICINA GENERAL, ODONTOLOGÍA, ENFERMERÍA)
  - Histograma de últimas 24h
  - Estadísticas de memoria

### Paso 3: Admisión

- **URL:** `GET /admission/registro` (opcional `?search=DNIorHC`)
- **Controlador:** `PacienteController`
- **Template:** `admission/registro.html`
- **Proceso:**
  1. Si `search` tiene valor, busca paciente existente por DNI/HC y precarga formulario
  2. El usuario selecciona **servicio destino** (Medicina General, Enfermería, Obstetricia, Odontología, Psicología, Nutrición) mediante radio buttons con nombre `servicio`
  3. El formulario POST envía datos del paciente + `servicio` como parámetro
- **POST `/admission/guardar`:** `PacienteController.registrarPaciente()`
  1. Asigna `servicioSolicitado` desde el parámetro `servicio`
  2. `PacienteService.registrarPaciente()`:
     - Si ya existe por DNI: actualiza datos demográficos y resetea estado a `PENDIENTE_PAGO`
     - Si es nuevo: `numeroHistoriaClinica = numeroDocumento`, estado = `PENDIENTE_PAGO`
  3. Redirige a `/admission/registro?saved=true`
- **Estado:** `PENDIENTE_PAGO`

### Paso 4: Caja (Pago)

- **URL:** `GET /caja/pago` (opcional `?hc=...&servicio=...`)
- **Controlador:** `CajaController`
- **Template:** `clinico/caja_pago.html`
- **Proceso:**
  1. Carga pacientes pendientes (estado=`PENDIENTE_PAGO`)
  2. Si hay `hc`, busca paciente por DNI/HC
  3. Muestra datos del paciente, monto por servicio, selector de tipo pago
- **POST `/caja/pagar`:** `CajaController.procesarPago()`
  1. Valida paciente, obtiene usuario autenticado
  2. Inserta en `clinico.pago_log` vía JDBC directo:
     ```sql
     INSERT INTO clinico.pago_log (id_paciente, id_usuario, monto, tipo_pago, concepto, numero_comprobante)
     ```
  3. Cambia estado a `PENDIENTE_TRIAJE`
  4. Redirige a `/caja/pago` con mensaje de éxito
- **Estado:** `PENDIENTE_TRIAJE`

### Paso 5: Triaje

- **URL:** `GET /triaje/nuevo` (lista de pacientes pendientes)
- **Controlador:** `TriajeController`
- **Template:** `clinico/triaje_busqueda.html`
- **Proceso:**
  1. Muestra pacientes con estado `PENDIENTE_TRIAJE`
  2. O clic en botón lleva a `GET /triaje/registrar/{idPaciente}`
- **URL:** `GET /triaje/registrar/{idPaciente}`
  - Template: `clinico/triaje_registro.html`
  - Se precarga el `servicioDestino` desde `paciente.servicioSolicitado`
- **POST `/triaje/guardar`:** `TriajeController.guardarTriaje()`
  1. Valida campos con `@Valid` (incluyendo `@NotBlank servicioDestino`)
  2. Obtiene usuario autenticado vía `usuarioRepository.findByUsername()`
  3. Normaliza servicio destino (mayúsculas, tildes, fallback a MEDICINA GENERAL)
  4. Normaliza clasificación urgencia a minúsculas
  5. `TriajeService.guardarTriaje()`:
     - Llama `evaluarAlertasClinicas()` — verifica:
       - PA sistólica ≥ 140 o diastólica ≥ 90 → HIPERTENSIÓN
       - PA sistólica < 90 o diastólica < 60 → HIPOTENSIÓN
       - FC > 100 → TAQUICARDIA, FC < 60 → BRADICARDIA
       - SpO2 < 95 → HIPOXIA
       - Temp ≥ 38.0 → FEBRIL, Temp < 35.5 → HIPOTERMIA
     - Marca `alertaClinica` y `detalleAlerta` si hay anomalías
  6. Cambia estado a `PENDIENTE_CONSULTA`
- **Estado:** `PENDIENTE_CONSULTA`

### Paso 6: Consulta (Cola)

- **URL:** `GET /consulta/modulo/{nombreModulo}`
- **Controlador:** `ConsultaController.listarColaModulo()`
- **Template:** `clinico/consulta_cola.html`
- **Proceso:**
  1. Normaliza nombre del módulo (ENFERMERIA → ENFERMERÍA, etc.)
  2. Consulta `TriajeRepository.buscarPendientesPorModulo(servicio, start)`:
     ```sql
     SELECT t FROM Triaje t WHERE t.servicioDestino = :servicio 
     AND t.paciente.estado = 'PENDIENTE_CONSULTA' 
     AND t.fechaHora >= :start ORDER BY t.fechaHora ASC
     ```
  3. Muestra pacientes en orden de llegada

### Paso 7: Atención Médica

- **URL:** `GET /consulta/atender/{idTriaje}`
- **Controlador:** `ConsultaController.atenderPaciente()`
- **Template:** `clinico/consulta_espera.html`
- **Datos cargados:**
  - Triaje completo con paciente
  - Historial de consultas del paciente
  - Alergias activas
  - Médico logueado
  - Datos serializados a JSON para JS
- **POST `/consulta/guardar` (AJAX JSON):**
  - Body: `ConsultaRequest` con triajeId, motivo, anamnesis, examenFisico, planTratamiento, proximoControl, tipoSalida, diagnosticos[], medicamentos[]
  - `ConsultaService.guardarConsultaCompleta()`:
    1. Carga Triaje y Paciente
    2. Obtiene Personal (médico) autenticado
    3. Crea Consulta con paciente, triaje, médico, datos clínicos
    4. Por cada diagnóstico: crea `DiagnosticoConsulta` con código CIE-10
    5. Por cada medicamento: llama `RecetaService.emitirReceta()` que:
       - Verifica alergias activas vs medicamentos prescritos
       - Lanza `AlergiaActivaException` si hay conflicto
       - Crea RecetaMedica y DetalleReceta
    6. Cambia estado a `ATENDIDO`
  - Retorna JSON `ApiResponse.ok()`
- **Estado:** `ATENDIDO`

### Rutas Adicionales Post-Consulta

| Acción | URL | Template |
|--------|-----|----------|
| Ver Historia Clínica | `GET /clinico/historia/{idPaciente}` | `clinico/historia_3_columnas.html` |
| Imprimir Receta | `GET /consulta/receta/preview` | `clinico/receta_impresion.html` |
| Imprimir Referencia | `GET /consulta/referencia/preview` | `clinico/referencia_impresion.html` |
| Certificado Médico | `GET /consulta/certificado/preview` | `clinico/certificado_medico.html` |
| Voucher Pago | `GET /caja/imprimir` | `clinico/voucher_impresion.html` |

---

## 6. Dashboard y Estadísticas

**API REST:** `GET /api/dashboard/stats` retorna JSON con:

| Campo | Fuente |
|-------|--------|
| `totalPacientes` | `SELECT COUNT(*) FROM filiacion.paciente` |
| `pendientesTriaje` | Contar `PENDIENTE_TRIAJE` |
| `pendientesMedicina` | `PENDIENTE_CONSULTA` + servicio = 'MEDICINA GENERAL' |
| `pendientesOdontologia` | `PENDIENTE_CONSULTA` + servicio = 'ODONTOLOGÍA' |
| `pendientesEnfermeria` | `PENDIENTE_CONSULTA` + servicio = 'ENFERMERÍA' |
| `atencionesHoy` | Consultas de hoy |
| `histograma` | Atenciones por hora (últimas 24h) |
| `eficiencia` | Relación atenciones/pendientes |

---

## 7. Servicios de Apoyo

### Laboratorio

- **URL:** `GET /apoyo/laboratorio`
- **Controlador:** `ApoyoDiagnosticoController.laboratorio()`
- **Template:** `clinico/laboratorio_lista.html`
- **Datos:** Órdenes desde `clinico.orden_medica` (vía JDBC) + datos de muestra si no hay registros

### Farmacia

- **URL:** `GET /apoyo/farmacia`
- **Controlador:** `ApoyoDiagnosticoController.farmacia()`
- **Template:** `clinico/farmacia_lista.html`
- **Datos:** Recetas desde `clinico.receta_medica` (vía JDBC) + datos de muestra

### Gestión de Pacientes

- **URL:** `GET /pacientes/lista`
- **Exportable a Excel:** `GET /pacientes/export/excel` (XSSFWorkbook Apache POI)

### Gestión de Personal

- **URL:** `GET /personal/lista` (requiere ADMIN, MEDICO_GENERAL o ENFERMERIA)
- **CRUD completo** con `@PreAuthorize` para operaciones de escritura (solo ADMIN)

---

## 8. Inicialización del Sistema

En cada inicio (`CommandLineRunner`), se ejecutan en orden:

### SystemInitializer (order=1)

1. **Purga BD** — Trunca tablas en orden inverso de dependencias
2. **Consistencia de esquema** — `ALTER TABLE ADD COLUMN IF NOT EXISTS` para columnas faltantes
3. **Seed TipoDocumento** — DNI, CE, PAS, DIE, S/DOC, CNV (con sus regex de validación)
4. **Seed Roles** — ADMIN, MEDICO_GENERAL, ENFERMERIA, OBSTETRICIA, ODONTOLOGIA, PSICOLOGIA, NUTRICION
5. **Seed Personal** — 6 profesionales predefinidos
6. **Seed Usuarios** — 7 usuarios con password `admin` (BCrypt)
7. **Seed Medicamentos** — 7 medicamentos MINSA + 5 vías + 5 familias
8. `@Profile("!test")` — no se ejecuta en tests

### Cie10Seeder (order=2)

1. Carga ~389 códigos CIE-10 desde archivo CSV
2. Los datos se curan desde `d:/UTP/SISTEMAS/AEAMAN/ciex/diagnosticos_cie10.csv`
3. En tests usa `src/test/resources/ciex-test/`
4. `@Profile("!test")` — no se ejecuta en tests

---

## 9. Manejo de Errores

| Excepción | HTTP Status | Redirección |
|-----------|------------|-------------|
| `AlergiaActivaException` | 409 Conflict | `/triaje/nuevo` con flash error |
| `IllegalArgumentException` | — | `/dashboard` con flash error |
| `RuntimeException` | — | `/dashboard` con flash error |
| Otras excepciones | 500 | `error` view (Thymeleaf) |

Todas manejadas por `GlobalExceptionHandler` con logging via SLF4J.

---

## 10. Workarounds y Decisiones Técnicas

| Decisión | Razón |
|----------|-------|
| `numeroHistoriaClinica = numeroDocumento` | Usuario confirmó que son equivalentes (ej: DNI 70000001 = HC 70000001) |
| `ddl-auto=none` en producción | Esquema BD gestionado manualmente por scripts SQL |
| `spring.sql.init.mode=never` en tests | H2 no soporta sintaxis PostgreSQL de los scripts |
| `hibernate.hbm2ddl.create_namespaces=true` en tests | H2 necesita crear explícitamente los schemas |
| `@Profile("!test")` en seeders | Evita que se ejecuten con H2 (PostgreSQL SQL no compatible) |
| `@WebMvcTest` + remoción de 3 tests | No permite probar 403 ni templates con `@AuthenticationPrincipal` |
| `servicioDestino` como hidden input con `form` attr | El input está fuera del `<form>` HTML, asociado vía `form="triajeForm"` |
| `@Service` en TriajeService (no solo interfaz) | Simplifica inyección; interfaz existe para DIP |
| Servicios expuestos vía interfaces | Para DIP (Dependency Inversion Principle) y OCP (Open/Closed Principle) |
| DTOs tipados (`ApiResponse`, `ConsultaRequest`) | Reemplazan `Map<String, Object>` en endpoints AJAX |
| Logging SLF4J exclusivamente | `System.out/err` prohibido por estándar |
| PostgreSQL 18 en desarrollo | Requerido para BD de producción; H2 solo en tests |

---

## 11. Estructura del Proyecto

```
src/main/java/com/sigeclin/
├── SigeclinApplication.java
├── controller/           → MainController, DashboardService (interface+impl)
├── config/               → SecurityConfig, GlobalExceptionHandler, SystemInitializer
├── filiacion/
│   ├── model/            → Persona, Paciente, Personal, Usuario, TipoDocumento
│   ├── repository/       → PacienteRepository, PersonalRepository, UsuarioRepository...
│   ├── service/          → IPacienteService, PacienteService, IPersonalService, PersonalService
│   └── controller/       → PacienteController, GestionPacienteController, PersonalController
├── seguridad/
│   ├── model/            → Rol
│   ├── repository/       → RolRepository
│   └── service/          → CustomUserDetailsService
├── clinico/
│   ├── model/            → Triaje, Consulta, DiagnosticoConsulta, RecetaMedica, DetalleReceta, AlergiaPaciente, AuditoriaAcceso
│   ├── repository/       → TriajeRepository, ConsultaRepository, RecetaRepository, DetalleRecetaRepository, AlergiaPacienteRepository, AuditoriaRepository
│   ├── service/          → ITriajeService/TriajeService, IConsultaService/ConsultaService, IRecetaService/RecetaService, IHistoriaClinicaService/HistoriaClinicaService, IAuditoriaService/AuditoriaService, IApoyoDiagnosticoService/ApoyoDiagnosticoService
│   ├── controller/       → TriajeController, ConsultaController, CajaController, HistoriaClinicaController, ApoyoDiagnosticoController
│   └── dto/              → ApiResponse, ConsultaRequest
├── maestras/
│   ├── model/            → Cie10, Medicamento, Servicio
│   ├── repository/       → Cie10Repository, MedicamentoRepository, ServicioRepository
│   ├── service/          → ICie10Service/Cie10Service, IMaestrasService/MaestrasService
│   ├── controller/       → Cie10RestController, ServicioController
│   └── config/           → Cie10Seeder

src/main/resources/templates/
├── login.html
├── dashboard.html
├── layout.html
├── error.html
├── admission/
│   └── registro.html
├── filiacion/
│   ├── pacientes_lista.html
│   └── personal_lista.html
├── clinico/
│   ├── triaje_busqueda.html
│   ├── triaje_registro.html
│   ├── consulta_cola.html
│   ├── consulta_espera.html
│   ├── consulta_form.html
│   ├── receta_impresion.html
│   ├── referencia_impresion.html
│   ├── certificado_medico.html
│   ├── historia_3_columnas.html
│   ├── caja_pago.html
│   ├── voucher_impresion.html
│   ├── laboratorio_lista.html
│   └── farmacia_lista.html
└── maestras/
    └── servicios.html

Test: 46 tests (0 fallas, 0 errores)
  - 3 Integration (context, datasource, beans)
  - 8 Security Authorization
  - 5 TriajeController, 3 ConsultaController
  - 4 TriajeService, 2 RecetaService
  - 10 PersonalService
  - 5 Cie10Service
  - 7 CustomUserDetailsService
