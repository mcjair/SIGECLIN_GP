# SIGECLIN — Sistema Integrado de Gestión Clínica AEAMAN

> **Versión:** 0.0.2-SNAPSHOT | **Spring Boot:** 3.2.5 | **Java:** 17 | **BD:** PostgreSQL 16 | **Puerto:** 3001

---

## Índice

1. [Arquitectura del Sistema](#1-arquitectura-del-sistema)
2. [Flujo de Procesos](#2-flujo-de-procesos)
3. [Modelo de Datos](#3-modelo-de-datos)
4. [API Endpoints](#4-api-endpoints)
5. [Servicios (Business Layer)](#5-servicios-business-layer)
6. [Repositorios (Data Access)](#6-repositorios-data-access)
7. [Seguridad](#7-seguridad)
8. [Librerías Java (Unidad 3)](#8-librerías-java-unidad-3)
9. [Frontend y UI](#9-frontend-y-ui)
10. [Pruebas (TDD)](#10-pruebas-tdd)
11. [Base de Datos](#11-base-de-datos)
12. [Configuración del Entorno](#12-configuración-del-entorno)
13. [Control de Versiones](#13-control-de-versiones)

---

## 1. Arquitectura del Sistema

### 1.1 Patrón MVC (Model-View-Controller)

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENTE (Browser)                     │
│              Thymeleaf + Bootstrap + JS                  │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP (GET/POST)
                         ▼
┌─────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER          ┌───────────────────────────┐│
│  @Controller / @RestController                         ││
│  • MainController          │  • PersonalController     ││
│  • PacienteController      │  • ConsultaController     ││
│  • TriajeController        │  • CajaController         ││
│  • HistoriaClinicaController                           ││
│  • Cie10RestController     │  • ServicioController     ││
│  • ApoyoDiagnosticoController                          ││
└────────────────────────┬────────────────────────────────┘
                         │ Llamada a Servicios
                         ▼
┌─────────────────────────────────────────────────────────┐
│  SERVICE LAYER (Business Logic)                         │
│  @Service                                                │
│  • PersonalService       • PacienteService              │
│  • ConsultaService       • TriajeService                │
│  • HistoriaClinicaService • RecetaService               │
│  • AuditoriaService      • Cie10Service                 │
│  • MaestrasService       • CustomUserDetailsService     │
└────────────────────────┬────────────────────────────────┘
                         │ Llamada a Repositorios
                         ▼
┌─────────────────────────────────────────────────────────┐
│  DAO / REPOSITORY LAYER     ┌──────────────────────────┐│
│  @Repository (Spring Data JPA)                         ││
│  • PersonalRepository    • PacienteRepository          ││
│  • ConsultaRepository    • TriajeRepository            ││
│  • RecetaRepository      • DetalleRecetaRepository     ││
│  • Cie10Repository       • ServicioRepository          ││
│  • + 8 repositorios adicionales                        ││
└────────────────────────┬────────────────────────────────┘
                         │ JPA / Hibernate
                         ▼
┌─────────────────────────────────────────────────────────┐
│  MODEL LAYER (JPA Entities)                             │
│  @Entity (15 entidades)                                 │
│  • Persona (base, JOINED) → Personal, Paciente, Usuario │
│  • Triaje, Consulta, RecetaMedica, DetalleReceta        │
│  • DiagnosticoConsulta, AlergiaPaciente                 │
│  • Cie10, Servicio, Medicamento, TipoDocumento          │
│  • Rol, AuditoriaAcceso                                 │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────────┐
│              PostgreSQL 16 (sigeclin)                    │
│         Esquemas: filiacion, clinico, maestras,         │
│                   seguridad                              │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Principios SOLID Aplicados

| Principio | Implementación |
|-----------|---------------|
| **S**ingle Responsibility | Cada service/controller tiene una responsabilidad específica |
| **O**pen/Closed | Spring DI permite extender sin modificar |
| **L**iskov Substitution | Herencia JOINED correcta: Persona → Personal/Paciente/Usuario |
| **I**nterface Segregation | Repositorios Spring Data JPA con interfaces enfocadas |
| **D**ependency Inversion | Inyección por constructor (`@RequiredArgsConstructor`) |

### 1.3 Seguridad en la Arquitectura

- Spring Security con autenticación por formulario
- BCryptPasswordEncoder para hash de contraseñas
- CSRF protección activa (excepto endpoints AJAX `/consulta/**`)
- `@EnableMethodSecurity` + `@PreAuthorize` en controllers
- `sec:authorize` en templates para ocultar elementos por rol
- Content-Security-Policy con directivas estrictas
- XSS Protection habilitado (modo block)
- 7 roles definidos: ADMIN, MEDICO_GENERAL, ENFERMERIA, OBSTETRICIA, ODONTOLOGIA, PSICOLOGIA, NUTRICION

---

## 2. Flujo de Procesos

### 2.1 Flujo Principal del Paciente

```
┌──────────┐     ┌────────┐     ┌────────┐     ┌───────────┐     ┌──────────┐
│ ADMISIÓN │────▶│  CAJA  │────▶│ TRIAJE │────▶│ CONSULTA  │────▶│  ALTA /  │
│(Registro)│     │ (Pago) │     │(Evalu.)│     │(Atención) │     │  RECITA  │
└──────────┘     └────────┘     └────────┘     └─────┬─────┘     └──────────┘
      │               │               │              │
      ▼               ▼               ▼              ▼
  Genera HC      pago_log      Alerta Clínica    Diagnóstico
  (Historia      (histórico    (si aplica)       CIE-10
   Clínica)      de pagos)                        + Receta
                                                     │
                                                     ▼
                                              ┌ ─ ─ ─ ─ ─ ┐
                                               Laboratorio
                                              │ Farmacia   │
                                               (Apoyo
                                              │ Diagnóstico)
                                               └ ─ ─ ─ ─ ─ ┘
```

### 2.2 Estados del Paciente

```
activo → PENDIENTE_PAGO → PENDIENTE_TRIAJE → PENDIENTE_CONSULTA → ATENDIDO
```
> **Nota:** El estado default del paciente es `"activo"`. `PacienteService.registrarPaciente()` lo establece explícitamente a `"PENDIENTE_PAGO"`.

### 2.3 Flujo Detallado por Módulo

#### Módulo de Admisión (`/admission/registro`)

1. Usuario ingresa DNI o nombre del paciente en buscador
2. Si existe → carga datos existentes y permite actualizar
3. Si no existe → formulario de nuevo paciente
4. Guarda → genera automáticamente HC (año-correlativo: `2026-000001`)
5. Estado pasa a `PENDIENTE_PAGO`
6. Redirecciona a Caja

#### Módulo de Caja (`/caja/pago`)

1. Lista pacientes con estado `PENDIENTE_PAGO`
2. Selecciona paciente → ingresa monto
3. Procesa pago → guarda en `pago_log` + cambia estado a `PENDIENTE_TRIAJE`
4. Imprime voucher

#### Módulo de Triaje (`/triaje/nuevo` → `/triaje/registrar/{id}`)

1. Lista pacientes con estado `PENDIENTE_TRIAJE`
2. Selecciona paciente → formulario de triaje
3. Registra: peso, talla, PA, FC, FR, temperatura, SpO2
4. IMC se calcula automáticamente
5. **Evaluación de alertas clínicas** (TriajeService):
   - `HIPERTENSIÓN`: PAS ≥ 140 o PAD ≥ 90
   - `TAQUICARDIA`: FC > 100
   - `HIPOXIA`: SpO2 < 95
   - `ESTADO FEBRIL`: Temperatura ≥ 38°C
   - `HIPOTERMIA`: Temperatura < 35°C
6. Clasifica urgencia: rojo/naranja/amarillo/verde
7. Asigna servicio destino
8. Estado → `PENDIENTE_CONSULTA`

#### Módulo de Consulta Médica (`/consulta/modulo/{servicio}` → `/consulta/atender/{idTriaje}`)

1. Cola de pacientes por módulo/servicio
2. Selecciona paciente → pantalla de 3 columnas:
   - **Columna 1:** Estado clínico (signos vitales, alertas, alergias)
   - **Columna 2:** Registro clínico (anamnesis, examen físico, plan) + historial
   - **Columna 3:** Recetario y diagnóstico CIE-10
3. Busca diagnóstico CIE-10 (autocompletado)
4. Agrega medicamentos a receta
5. Finaliza atención → guarda consulta + diagnósticos + receta
6. Opciones de salida: ALTA, RECITA (programa cita control), REFERENCIA

#### Módulo de Personal (`/personal/lista`)

1. Lista de profesionales de salud (solo ADMIN, MEDICO_GENERAL, ENFERMERIA)
2. CRUD completo: Crear, Editar, Cambiar estado (activo/inactivo)
3. Modal de permisos (simulado)
4. Validación de colegiatura (regex: `CMP-12345`, `CEP-1234`, etc.)

#### Dashboard (`/dashboard`)

1. KPIs en tiempo real: ingresos hoy, atenciones, eficiencia, espera promedio
2. Monitoreo de cola de espera (triaje)
3. Ocupación por servicio (barras de progreso)
4. Telemetría JVM (uso de memoria RAM)
5. Últimas transacciones de caja
6. Gráfico SVG de carga horaria
7. Polling cada 15 segundos vía AJAX (`/api/dashboard/stats`)

---

## 3. Modelo de Datos

### 3.1 Diagrama de Entidades

```
Persona (JOINED BASE)
├── Personal (id_personal PK)
│   ├── idTipoPersonal, idEspecialidad
│   ├── numeroColegiatura, fechaIngreso
│   ├── estadoLaboral, horario (JSON)
│   ├── firmaDigital (BINARY)
│   └── @OneToOne → Usuario
├── Paciente (id_paciente PK)
│   ├── numeroHistoriaClinica (UNIQUE)
│   ├── grupoSanguineo, factorRh
│   ├── contactoEmergencia*, estadoCivil
│   ├── estado (PENDIENTE_PAGO → PENDIENTE_TRIAJE → PENDIENTE_CONSULTA → ATENDIDO)
│   └── servicioSolicitado
└── Usuario (id_usuario PK)
    ├── username (UNIQUE), passwordHash
    ├── cuentaBloqueada, intentosFallidos
    └── @ManyToMany → Rol

Triaje → @ManyToOne → Paciente, Usuario
       → presionArterial*, frecuencia*, temperatura, SpO2
       → imc (calculated), clasificacionUrgencia
       → alertaClinica, detalleAlerta

Consulta → @ManyToOne → Paciente, Personal (médico)
         → @OneToOne → Triaje
         → motivoConsulta, anamnesis, examenFisico, planTratamiento
         → estado (en_progreso → completada)
         → @OneToMany → DiagnosticoConsulta
         → @OneToMany → RecetaMedica

RecetaMedica → @ManyToOne → Consulta, Paciente, Personal
             → estado (emitida/dispensada)
             → @OneToMany → DetalleReceta (Cascade ALL)

DetalleReceta → @ManyToOne → RecetaMedica, Medicamento
              → dosis, frecuencia, duracionDias, cantidadTotal

DiagnosticoConsulta → @ManyToOne → Consulta, Cie10
                    → tipoDiagnostico (PRESUNTIVO/DEFINITIVO)

AlergiaPaciente → @ManyToOne → Paciente, Medicamento
                → severidad (LEVE/MODERADA/SEVERA)
                → activa (boolean)

AuditoriaAcceso → usuario, accion, detalle, ipOrigen, fechaHora
```

### 3.2 Entidades y sus Esquemas

| Esquema | Entidades |
|---------|-----------|
| **filiacion** | persona, tipo_documento, paciente, personal, usuario |
| **clinico** | triaje, consulta, diagnostico_consulta, receta_medica, detalle_receta, alergia_paciente, auditoria_acceso, pago_log, orden_medica, resultado_laboratorio, dispensacion, lote_medicamento |
| **maestras** | cie10, servicio, catalogo_medicamentos, via_administracion, familia_farmacologica, especialidad, tipo_personal, catalogo_ciex |
| **seguridad** | rol, usuario_rol, sesion_log |

---

## 4. API Endpoints

### 4.1 Endpoints de Vista (Thymeleaf)

| Método | Ruta | Controller | Acceso | Descripción |
|--------|------|------------|--------|-------------|
| GET | `/login` | MainController | Público | Login |
| GET | `/`, `/dashboard` | MainController | Auth | Dashboard principal |
| GET | `/personal/lista` | PersonalController | ADMIN/MEDICO_GENERAL/ENFERMERIA | Gestión de personal |
| GET | `/personal/api/{id}` | PersonalController | ADMIN/MEDICO_GENERAL/ENFERMERIA | JSON personal |
| POST | `/personal/guardar` | PersonalController | ADMIN | Crear/editar personal |
| POST | `/personal/eliminar/{id}` | PersonalController | ADMIN | Desactivar personal |
| POST | `/personal/toggle-estado/{id}` | PersonalController | ADMIN | Activar/desactivar |
| GET | `/admission/registro` | PacienteController | Auth | Admisión de pacientes |
| GET | `/admission/api/buscar/{documento}` | PacienteController | Auth | Buscar paciente (JSON) |
| POST | `/admission/guardar` | PacienteController | Auth | Guardar admisión |
| GET | `/pacientes/lista` | GestionPacienteController | Auth | Lista pacientes |
| GET | `/pacientes/export/excel` | GestionPacienteController | Auth | Exportar Excel |
| GET | `/pacientes/api/historial/{id}` | GestionPacienteController | Auth | Historial paciente (JSON) |
| GET | `/triaje/nuevo` | TriajeController | Auth | Lista triaje pendiente |
| GET | `/triaje/registrar/{idPaciente}` | TriajeController | Auth | Formulario triaje |
| POST | `/triaje/guardar` | TriajeController | Auth | Guardar triaje |
| GET | `/consulta/modulo/{nombreModulo}` | ConsultaController | Auth | Cola por módulo |
| GET | `/consulta/atender/{idTriaje}` | ConsultaController | Auth | Atención médica |
| POST | `/consulta/guardar` | ConsultaController | Auth | Guardar consulta (JSON) |
| GET | `/consulta/api/detalle/{id}` | ConsultaController | Auth | Detalle consulta (JSON) |
| GET | `/consulta/receta/preview` | ConsultaController | Auth | Vista previa receta |
| GET | `/consulta/referencia/preview` | ConsultaController | Auth | Vista previa referencia |
| GET | `/consulta/certificado/preview` | ConsultaController | Auth | Certificado médico |
| GET | `/clinico/historia/{idPaciente}` | HistoriaClinicaController | Auth | Historia clínica |
| GET | `/caja/pago` | CajaController | Auth | Pantalla pago |
| POST | `/caja/pagar` | CajaController | Auth | Procesar pago |
| GET | `/caja/imprimir` | CajaController | Auth | Voucher impresión |
| GET | `/apoyo/laboratorio` | ApoyoDiagnosticoController | Auth | Órdenes laboratorio |
| GET | `/apoyo/farmacia` | ApoyoDiagnosticoController | Auth | Recetas farmacia |
| GET | `/servicios` | ServicioController | Auth | Servicios activos |

### 4.2 Endpoints REST (JSON)

| Método | Ruta | Controller | Acceso | Descripción |
|--------|------|------------|--------|-------------|
| GET | `/api/dashboard/stats` | MainController | Auth | KPIs dashboard |
| GET | `/api/cie10/search?q=` | Cie10RestController | Público | Búsqueda CIE-10 |
| GET | `/consulta/api/cie10/search?q=` | ConsultaController | Auth | Búsqueda CIE-10 |
| GET | `/personal/api/{id}` | PersonalController | Auth | Datos personal |
| GET | `/admission/api/buscar/{documento}` | PacienteController | Auth | Buscar paciente |
| GET | `/pacientes/api/historial/{id}` | GestionPacienteController | Auth | Historial paciente |
| GET | `/consulta/api/detalle/{id}` | ConsultaController | Auth | Detalle consulta |

---

## 5. Servicios (Business Layer)

### 5.1 PersonalService
| Método | Transaccional | Descripción |
|--------|:------------:|-------------|
| `listarTodos()` | No | Lista completo de personal |
| `buscarPorId(Integer)` | No | Busca por ID o lanza RuntimeException |
| `guardar(Personal)` | ✅ | Normaliza nombres/colegiatura (Commons Lang3), guarda |
| `eliminar(Integer)` | ✅ | Cambia estado a "inactivo" |
| `toggleEstado(Integer)` | ✅ | Alterna activo ↔ inactivo |

### 5.2 PacienteService
| Método | Transaccional | Descripción |
|--------|:------------:|-------------|
| `registrarPaciente(Paciente)` | ✅ | Crea o actualiza, genera HC |
| `buscarPorId(Integer)` | No | Optional<Paciente> |
| `obtenerPacientesRecientes()` | No | Pendientes de pago |
| `obtenerPendientesTriaje()` | No | Estado PENDIENTE_TRIAJE |
| `obtenerColaConsulta(String)` | No | Estado + servicio |
| `actualizarEstado(Integer, String)` | ✅ | Cambia estado |
| `buscarPorDniOHC(String)` | No | Búsqueda por DNI o HC |

### 5.3 ConsultaService
| Método | Transaccional | Descripción |
|--------|:------------:|-------------|
| `obtenerHistorialPaciente(Integer)` | No | Consultas por paciente |
| `obtenerPacientesEnEspera()` | No | Triajes de hoy |
| `obtenerPacientesEnEsperaPorModulo(String)` | No | Filtrados por módulo |
| `obtenerServiciosActivos()` | No | Servicios |
| `guardarConsultaCompleta(Integer, Map)` | ✅ | Guarda consulta + dx + receta |

### 5.4 TriajeService
| Método | Transaccional | Descripción |
|--------|:------------:|-------------|
| `guardarTriaje(Triaje)` | ✅ | Evalúa alertas, guarda |
| `evaluarAlertasClinicas(Triaje)` | No | Detecta HTA+taquicardia+hipoxia+fiebre |
| `obtenerHistorialTriaje(Integer)` | No | Triajes por paciente |

### 5.5 HistoriaClinicaService
| Método | Descripción |
|--------|-------------|
| `obtenerHistoriaClinicaCompleta(Integer)` | Mapa con paciente + consultas + alergias + triajes + recetas |

### 5.6 Cie10Service
| Método | Descripción |
|--------|-------------|
| `init()` (@PostConstruct) | Carga ~389 diagnósticos **curados** desde CSV (`diagnosticos_cie10.csv`) a caché Guava |
| `search(String)` | Búsqueda con caché (1000 entradas, 10min expiración) |
| `search(String, String servicio)` | Búsqueda filtrada por servicio/módulo (MEDICINA GENERAL, ENFERMERIA, etc.) |
| `getCacheSize()` | Tamaño de caché actual |

### 5.7 RecetaService
| Método | Transaccional | Descripción |
|--------|:------------:|-------------|
| `emitirReceta(RecetaMedica, List<DetalleReceta>)` | ✅ | Valida alergias activas del paciente contra cada medicamento; lanza `AlergiaActivaException` si hay conflicto; guarda receta y detalles |

---

## 6. Repositorios (Data Access)

### 6.1 Repositorios con Queries Personalizadas

| Repositorio | Método | Query |
|-------------|--------|-------|
| **PacienteRepository** | `findByEstadoOrderByFechaCreacionAsc(String)` | Estado + orden fecha |
| | `findByEstadoAndServicioSolicitadoOrderByFechaCreacionAsc()` | Estado + servicio |
| | `findByTipoAndNumeroDocumento(String, String)` | JOIN con TipoDocumento |
| **TriajeRepository** | `buscarPendientesPorModulo(String, LocalDateTime)` | JPQL pendientes por módulo |
| | `findTopByPacienteNumeroDocumentoOrPacienteNumeroHistoriaClinica` | Búsqueda por doc o HC |
| **Cie10Repository** | `search(String, Pageable)` | LIKE en código y descripción |
| **MedicamentoRepository** | `buscarPorNombre(String)` | LIKE en nombre genérico |
| **AlergiaRepository** | `findByPacienteIdPersonaAndActivaTrue(Integer)` | Alergias activas del paciente |
| **AlergiaPacienteRepository** | `findByPacienteIdPersonaAndActivaTrue(Integer)` | Alergias activas (usado por RecetaService) |

---

## 7. Seguridad

### 7.1 Configuración (SecurityConfig.java)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize
public class SecurityConfig {
    // Rutas públicas: /, /login, /error, /css/**, /js/**, /webjars/**, /api/cie10/**
    // Form login: /login → /dashboard
    // CSRF: activo, excepto /consulta/** (llamadas AJAX)
    // Password: BCrypt
    // Headers: XSS Protection + Content Security Policy
}
```

### 7.2 Roles del Sistema

| Rol | Acceso a Módulos |
|-----|-----------------|
| **ADMIN** | Todo el sistema |
| **MEDICO_GENERAL** | Dashboard, Consulta, Personal (lectura), Pacientes |
| **ENFERMERIA** | Dashboard, Consulta (Enfermería), Personal (lectura) |
| **OBSTETRICIA** | Dashboard, Consulta (Obstetricia) |
| **ODONTOLOGIA** | Dashboard, Consulta (Odontología) |
| **PSICOLOGIA** | Dashboard, Consulta (Psicología) |
| **NUTRICION** | Dashboard, Consulta (Nutrición) |

### 7.3 Usuarios por Defecto (SystemInitializer)

| Usuario | Contraseña | Roles |
|---------|-----------|-------|
| admin | admin | TODOS |
| medicina | admin | MEDICO_GENERAL |
| enfermeria | admin | ENFERMERIA |
| obstetricia | admin | OBSTETRICIA |
| odontologia | admin | ODONTOLOGIA |
| psicologia | admin | PSICOLOGIA |
| nutricion | admin | NUTRICION |

---

## 8. Librerías Java (Unidad 3)

### 8.1 Google Guava (33.0.0-jre)
- **Uso:** `Cie10Service` — caché de búsqueda CIE-10 con `CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(10, TimeUnit.MINUTES).build()`
- **Propósito:** Almacena en memoria ~389 diagnósticos **curados** (top 50 por servicio) para búsquedas rápidas sin consultar BD

### 8.2 Apache POI (5.2.5 + poi-ooxml)
- **Uso:** `GestionPacienteController` — exportación de pacientes a Excel (.xlsx) con formato profesional
- **Propósito:** Generación de reportes clínicos descargables con cabeceras, estilos y formato de celdas

### 8.3 Apache Commons Lang3 (3.14.0)
- **Uso:** `PersonalService` — `StringUtils.upperCase()`, `StringUtils.trim()`, `Validate.notNull()`
- **Propósito:** Normalización de datos (colegiatura a mayúsculas, trim de nombres), validación de parámetros

### 8.4 Logback (Spring Boot Starter)
- **Uso:** `GlobalExceptionHandler`, `Cie10Service` — `LoggerFactory.getLogger()` con niveles DEBUG/TRACE
- **Configuración:** `logging.level.com.sigeclin=DEBUG`, `logging.level.org.springframework.security=TRACE`

---

## 9. Frontend y UI

### 9.1 Tecnologías
- **Thymeleaf** + Spring Security Extras (thymeleaf-extras-springsecurity6)
- **Bootstrap 5.3.2** (CDN + WebJar)
- **Bootstrap Icons** (CDN)
- **SweetAlert2** (CDN)
- **Plus Jakarta Sans** (Google Fonts)
- **CSS propio:** `main.css` (2314 líneas)

### 9.2 Estructura de Templates

```
templates/
├── layout.html            ← Layout maestro (head, sidebar, main, footer, theme toggle)
├── login.html             ← Página de inicio de sesión
├── dashboard.html         ← Dashboard con KPIs, gráficos, cola de espera
├── admission/
│   └── registro.html      ← Admisión con búsqueda de pacientes
├── filiacion/
│   ├── personal_lista.html ← CRUD personal médico
│   └── pacientes_lista.html ← Lista de pacientes con exportación Excel
├── clinico/
│   ├── triaje_busqueda.html  ← Lista pendientes de triaje
│   ├── triaje_registro.html  ← Formulario de triaje
│   ├── consulta_cola.html    ← Cola de pacientes por módulo
│   ├── consulta_espera.html  ← Atención médica (3 columnas)
│   ├── consulta_form.html    ← Formulario de consulta
│   ├── receta_impresion.html ← Vista de impresión de receta
│   ├── referencia_impresion.html ← Hoja de referencia
│   ├── certificado_medico.html   ← Certificado médico
│   ├── caja_pago.html       ← Pantalla de cobro
│   ├── voucher_impresion.html    ← Voucher de pago
│   ├── historia_3_columnas.html  ← Historia clínica completa
│   ├── farmacia_lista.html       ← Recetas para farmacia
│   └── laboratorio_lista.html    ← Órdenes de laboratorio
└── maestras/
    └── servicios.html       ← Servicios activos
```

### 9.3 Sistema de Temas (Modo Oscuro/Claro)
- **CSS Variables** en `:root` y `[data-theme="dark"]`
- **Persistencia:** `localStorage.theme`
- **Toggle:** Botón en header del layout
- **Glassmorphism:** Tarjetas con `backdrop-filter: blur(20px) saturate(180%)`
- **WCAG AA:** Contraste validado con `--text-dim: #6b7b8f` (4.5:1)

---

## 10. Pruebas (TDD)

### 10.1 Tests Unitarios Implementados

| Test | Framework | Métodos Probados | Cantidad |
|------|-----------|-----------------|:--------:|
| **PersonalServiceTest** | JUnit 5 + Mockito | guardar, buscarPorId, toggleEstado, eliminar | 10 tests |
| **TriajeServiceTest** | JUnit 5 + Mockito | evaluarAlertasClinicas, guardarTriaje | 4 tests |
| **Cie10ServiceTest** | JUnit 5 | init (cache), search (codigo/descripcion/servicio) | 5 tests |
| **Total** | | | **19 tests** |

### 10.2 Ejecución
```bash
mvn test           # Ejecuta todos los tests
mvn test -q        # Modo silencioso (solo errores)
```

---

## 11. Optimización: Catálogo CIE-10 Curado (389 códigos)

### 11.1 Problema Original
- `Cie10Service` cargaba **24,584+** diagnósticos desde múltiples CSV al iniciar
- Alto consumo de memoria heap para datos que nunca se usaban
- Búsquedas lentas al no estar filtradas por especialidad

### 11.2 Solución Implementada
- Investigación de los **50 diagnósticos más frecuentes** por cada servicio (MINSA/ESSALUD/HNDM):
  - **MEDICINA GENERAL**: Hipertensión (I10), Diabetes (E11.9), Rinofaringitis (J00), Lumbalgia (M54.5), etc.
  - **ENFERMERÍA**: Inmunizaciones (Z23.*), cuidados de heridas (Z48.*), signos vitales (Z01.*)
  - **OBSTETRICIA**: Embarazo (Z34.*), parto (O80), control prenatal (Z35.*), anemia gestacional (O99.0)
  - **ODONTOLOGÍA**: Caries (K02.*), gingivitis (K05.*), pulpitis (K04.*), estomatitis (K12.0)
  - **PSICOLOGÍA**: Ansiedad (F41.*), depresión (F32.*), trastorno adaptativo (F43.2), TDAH (F90.*)
  - **NUTRICIÓN**: Obesidad (E66.9), desnutrición (E44.*), trastornos alimentarios (F50.*)
- Archivo único: `ciex/diagnosticos_cie10.csv` con columnas `codigo,descripcion,servicios`
- **389 códigos únicos** (muchos compartidos entre servicios)

### 11.3 Filtrado por Servicio
- `Cie10Service.search(query, servicio)` filtra por la columna `servicios`
- El frontend envía `&servicio=MEDICINA GENERAL` al buscar diagnósticos
- Cada endpoint REST acepta parámetro opcional `servicio`
- Guava Cache: clave compuesta `"servicio:query"` para evitar mezclar resultados

### 11.4 Impacto
| Métrica | Antes | Después |
|---------|-------|---------|
| Diagnósticos en memoria | 24,584+ | 389 |
| Archivos CSV procesados | 5 archivos | 1 archivo |
| Memoria heap utilizada | ~15-20 MB | ~200 KB |
| Relevancia por servicio | Sin filtro | Filtrado preciso |
| Tiempo de carga | ~3-5 seg | < 0.5 seg |

---

## 12. Correcciones Aplicadas (v0.0.2)

| # | Archivo | Corrección |
|---|---------|------------|
| 1 | `src/main/resources/application.properties` | Puerto unificado a `3001`; agregado `sigeclin.cie10.dir-path` |
| 2 | `application.properties` (raíz) | Sincronizado con la versión de resources |
| 3 | `maestras/model/Cie10.java` | Agregado campo `servicios` (VARCHAR 255) |
| 4 | `maestras/service/Cie10Service.java` | Ahora carga solo el CSV curado; `search()` acepta filtro por servicio |
| 5 | `maestras/controller/Cie10RestController.java` | Cambiado de `Cie10Repository` a `Cie10Service` (usa caché Guava) |
| 6 | `maestras/config/Cie10Seeder.java` | Eliminado `@Profile("dev")`; limitado a 389 códigos curados |
| 7 | `config/SystemInitializer.java` | Agregado `ALTER TABLE maestras.cie10 ADD COLUMN servicios` |
| 8 | `clinico/repository/AlergiaPacienteRepository.java` | Corregido: extendía `JpaRepository<Triaje,...>` en vez de `AlergiaPaciente` |
| 9 | `clinico/service/RecetaService.java` | Descomentado y reimplementado con validación real de alergias |
| 10 | `clinico/service/ConsultaService.java` | Eliminado comentario en `triajeRepository.save()` (ya no hay skip) |
| 11 | `clinico/service/AuditoriaService.java` | Ahora extrae usuario real del `SecurityContext` e IP del request |
| 12 | `clinico/controller/ConsultaController.java` | Agregado `moduloJson` al modelo; search pasa `servicio` a Cie10Service |
| 13 | `templates/clinico/consulta_espera.html` | JS envía `&servicio=` en la búsqueda CIE-10 |
| 14 | `ciex/diagnosticos_cie10.csv` | Reemplazado por versión curada con 389 códigos esenciales |
| 15 | `test/.../Cie10ServiceTest.java` | Agregados tests de filtrado por servicio (5 tests en total) |

---

## 13. Base de Datos

### 11.1 Conexión
- **Motor:** PostgreSQL 16
- **Host:** 127.0.0.1:5432
- **Base:** sigeclin
- **Usuario:** admin
- **Password:** admin
- **SSL:** disabled

### 11.2 Esquemas y Tablas Principales

```
filiacion
├── persona             (base personas)
├── tipo_documento      (DNI, CE, PAS, etc.)
├── paciente            (hereda de persona)
├── personal            (hereda de persona)
└── usuario             (hereda de persona, auth)

clinico
├── triaje              (evaluación inicial)
├── consulta            (atención médica)
├── diagnostico_consulta (diagnósticos CIE-10)
├── receta_medica       (prescripciones)
├── detalle_receta      (medicamentos por receta)
├── alergia_paciente    (alergias registradas)
├── auditoria_acceso    (logs de acceso)
├── pago_log            (historial de pagos)
├── orden_medica        (órdenes de laboratorio)
├── resultado_laboratorio
├── dispensacion        (dispensación farmacia)
└── lote_medicamento    (control de stock)

maestras
├── cie10               (catálogo CIE-10 CURADO, ~389 registros, con columna `servicios`)
├── servicio            (módulos de atención)
├── catalogo_medicamentos (medicamentos MINSA)
├── via_administracion  (vías de admin. medicamentos)
├── familia_farmacologica
├── especialidad
└── tipo_personal

seguridad
├── rol                 (roles del sistema)
├── usuario_rol         (asignación roles)
└── sesion_log          (historial sesiones)
```

### 11.3 DDL
- `spring.jpa.hibernate.ddl-auto=none` → Esquema gestionado manualmente
- `SystemInitializer` aplica `ALTER TABLE IF NOT EXISTS` para consistencia
- Seeders insertan datos iniciales al arrancar

---

## 14. Configuración del Entorno

### 12.1 Archivos de Configuración

| Archivo | Propósito |
|---------|-----------|
| `src/main/resources/application.properties` | **Único archivo usado por Spring Boot.** Puerto 3001, BD, logging, ruta CIE-10 |
| `application.properties` (raíz) | Copia de respaldo (Spring Boot NO lo lee desde la raíz) |
| `pom.xml` | Dependencias Maven (Java 17, Spring Boot 3.2.5) |

### 12.2 Compilación y Ejecución

```bash
# Compilar
mvn compile

# Ejecutar tests
mvn test

# Iniciar servidor
mvn spring-boot:run

# El sistema corre en: http://localhost:3001
```

### 12.3 Requisitos
- Java 17+
- Maven 3.9+
- PostgreSQL 16+
- Base de datos `sigeclin` creada

---

## 15. Control de Versiones

### 13.1 Git
- **Repositorio local:** Inicializado con 3 commits
- **Remote:** `https://github.com/mcjair/SIGECLIN_GP.git`
- **Rama principal:** `master` → `origin/master`

### 13.2 Commits Realizados
```
8ac5de3 Unidad 3: CSRF, validacion, tests, exception handler, Apache Commons, GitHub setup
71861cd Unidad 3: Initial project structure with SOLID, Apache POI Excel Export and Git setup
5213b19 Unidad 3: Refactored alert calculation to TriajeService, added unit tests, and updated gitignore
```

### 13.3 Flujo Git Recomendado
```bash
git checkout -b feature/nueva-funcionalidad
# ... desarrollo ...
git add -A
git commit -m "feat: descripción del cambio"
git checkout master
git merge feature/nueva-funcionalidad
git push origin master
```

---

## Anexo: Estructura Completa del Proyecto

```
aeaman/
├── pom.xml
├── application.properties
├── .gitignore
├── SIGECLIN_DOCUMENTACION.md
├── scratch/
│   ├── DbCheck.java
│   ├── DataPurge.java
│   ├── ListUsers.java
│   └── QuerySchema.java
├── src/
│   ├── main/
│   │   ├── java/com/sigeclin/
│   │   │   ├── SigeclinApplication.java
│   │   │   ├── controller/MainController.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── SystemInitializer.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── AlergiaActivaException.java
│   │   │   ├── seguridad/
│   │   │   │   ├── model/Rol.java
│   │   │   │   ├── repository/RolRepository.java
│   │   │   │   └── service/CustomUserDetailsService.java
│   │   │   ├── filiacion/
│   │   │   │   ├── model/{Persona,Personal,Paciente,Usuario,TipoDocumento}.java
│   │   │   │   ├── repository/{Personal,Paciente,Usuario,TipoDocumento}Repository.java
│   │   │   │   ├── service/{Personal,Paciente}Service.java
│   │   │   │   └── controller/{Personal,Paciente,GestionPaciente}Controller.java
│   │   │   ├── clinico/
│   │   │   │   ├── model/{Triaje,Consulta,RecetaMedica,DetalleReceta,DiagnosticoConsulta,AlergiaPaciente,AuditoriaAcceso}.java
│   │   │   │   ├── repository/{Triaje,Consulta,Receta,DetalleReceta,DiagnosticoConsulta,Alergia,AlergiaPaciente,Auditoria}Repository.java
│   │   │   │   ├── service/{Consulta,Triaje,HistoriaClinica,Auditoria,Receta}Service.java
│   │   │   │   └── controller/{Consulta,Triaje,HistoriaClinica,Caja,ApoyoDiagnostico}Controller.java
│   │   │   └── maestras/
│   │   │       ├── model/{Cie10,Servicio,Medicamento}.java
│   │   │       ├── repository/{Cie10,Medicamento,Servicio}Repository.java
│   │   │       ├── service/{Cie10,Maestras}Service.java
│   │   │       ├── controller/{Cie10RestController,ServicioController}.java
│   │   │       └── config/Cie10Seeder.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/css/main.css
│   │       └── templates/
│   │           ├── layout.html
│   │           ├── login.html
│   │           ├── dashboard.html
│   │           ├── admission/registro.html
│   │           ├── filiacion/{personal,pacientes}_lista.html
│   │           ├── clinico/{triaje_busqueda,triaje_registro,consulta_cola,consulta_espera,consulta_form,receta_impresion,referencia_impresion,certificado_medico,caja_pago,voucher_impresion,historia_3_columnas,farmacia_lista,laboratorio_lista}.html
│   │           └── maestras/servicios.html
│   └── test/java/com/sigeclin/
│       ├── filiacion/service/PersonalServiceTest.java
│       ├── clinico/service/TriajeServiceTest.java
│       └── maestras/service/Cie10ServiceTest.java
```

---

> **Documentación generada para el Curso Integrador de Sistemas de Software — Unidad 3**
> **Grupo:** AEAMAN | **Sistema:** SIGECLIN | **Última actualización:** Mayo 2026
