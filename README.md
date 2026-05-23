# SIGECLIN — Sistema Integrado de Gestión Clínica

Sistema web para la gestión integral de atenciones médicas en centros de salud AEAMAN. Cubre el flujo completo: admisión → caja → triaje → consulta → receta → diagnóstico.

## Stack

| Capa       | Tecnología                          |
|------------|-------------------------------------|
| Backend    | Java 17, Spring Boot 3.2.5         |
| ORM        | Spring Data JPA (Hibernate)        |
| Seguridad  | Spring Security 6 + Thymeleaf Extras|
| BD         | PostgreSQL 18                      |
| Frontend   | Thymeleaf, Bootstrap 5.3.2         |
| Build      | Maven 3.9.6                        |
| Tests      | JUnit 5, Mockito, MockMvc          |

## Requisitos

- **Java 17** (JDK)
- **Maven 3.9+** (o usar `./mvnw`)
- **PostgreSQL 16+**
- **Git**

## Configuración

### 1. Base de datos

```sql
CREATE DATABASE sigeclin;
CREATE USER admin WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE sigeclin TO admin;
```

Ejecutar `src/main/resources/schema.sql` para crear esquemas y tablas.

### 2. Propiedades

El archivo `src/main/resources/application.properties` ya contiene valores por defecto:

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/sigeclin
spring.datasource.username=admin
spring.datasource.password=admin
server.port=3001
```

### 3. Catálogo CIE-10

Se requiere un directorio con el archivo `diagnosticos_cie10.csv` (cargado automáticamente al iniciar). Configurar en:

```properties
sigeclin.cie10.dir-path=/ruta/a/tu/ciex
```

### 4. Ejecutar

```bash
mvn spring-boot:run
# o bien:
mvn clean package -DskipTests
java -jar target/aeaman-0.0.1-SNAPSHOT.jar
```

Acceder a: `http://localhost:3001`

### 5. Tests

```bash
mvn test
# 29 tests: 8 controller (MockMvc) + 19 unitarios + 2 RecetaService
```

## Roles de usuario

- `ADMIN` — acceso total
- `MEDICO_GENERAL` — consulta, recetas, diagnósticos
- `ENFERMERIA` — triaje, signos vitales
- `OBSTETRICIA`, `ODONTOLOGIA`, `PSICOLOGIA`, `NUTRICION` — consulta por especialidad
- `CAJA` — registro de pagos

## Flujo de atención

```
Admisión → Caja → Triaje → Consulta → Receta / Diagnóstico
```

1. **Admisión**: registro de paciente con datos personales y HC
2. **Caja**: pago por servicio → paciente pasa a `PENDIENTE_TRIAJE`
3. **Triaje**: signos vitales, clasificación de urgencia, servicio destino
4. **Consulta**: anamnesis, examen físico, plan de tratamiento
5. **Receta**: medicamentos con dosis, frecuencia, duración
6. **Diagnóstico**: códigos CIE-10

## Endpoints principales

| Método | Ruta                    | Descripción                    |
|--------|-------------------------|--------------------------------|
| GET    | `/admission/registro`   | Formulario de admisión         |
| POST   | `/admission/guardar`    | Registrar paciente             |
| GET    | `/caja/pago`            | Pantalla de caja               |
| POST   | `/caja/pagar`           | Procesar pago                  |
| GET    | `/triaje/nuevo`         | Búsqueda de paciente para triaje|
| GET    | `/triaje/registrar/{id}`| Formulario de triaje           |
| POST   | `/triaje/guardar`       | Guardar triaje                 |
| GET    | `/consulta/modulo/{m}`  | Lista de pacientes en espera   |
| POST   | `/consulta/guardar`     | Guardar atención (JSON)        |
| GET    | `/consulta/historial/{id}`| Historial del paciente       |

## Estructura del proyecto

```
src/main/java/com/sigeclin/
├── SigeclinApplication.java
├── config/          → SecurityConfig, SystemInitializer
├── clinico/
│   ├── controller/  → Consulta, Triaje, Caja, ApoyoDiagnostico, HistoriaClinica
│   ├── dto/         → ConsultaRequest, ApiResponse<T>
│   ├── service/     → I*Service + ConsultaService, TriajeService, RecetaService, ApoyoDiagnosticoService
│   ├── repository/  → JPA repositories
│   └── model/       → Consulta, Triaje, RecetaMedica, DetalleReceta, etc.
├── filiacion/
│   ├── controller/  → PacienteController, GestionPacienteController, PersonalController
│   ├── service/     → IP*Service + PacienteService, PersonalService
│   ├── repository/  → JPA repositories
│   └── model/       → Persona, Personal, Paciente, Usuario, etc.
├── maestras/
│   ├── controller/  → Cie10RestController, ServicioController
│   ├── service/     → I*Service + Cie10Service, MaestrasService
│   ├── repository/  → Cie10Repository, MedicamentoRepository
│   └── model/       → Cie10, Medicamento, Servicio, etc.
├── service/         → IDashboardService + DashboardService
├── seguridad/
│   └── service/     → CustomUserDetailsService
└── exception/       → GlobalExceptionHandler, AlergiaActivaException
```
src/main/java/com/sigeclin/
├── SigeclinApplication.java
├── config/          → SecurityConfig, SystemInitializer
├── clinico/
│   ├── controller/  → Consulta, Triaje, Caja
│   ├── service/     → ConsultaService, TriajeService, RecetaService
│   ├── repository/  → JPA repositories
│   └── model/       → Consulta, Triaje, RecetaMedica, DetalleReceta, etc.
├── filiacion/
│   ├── controller/  → PacienteController, PersonalController
│   ├── service/     → PacienteService, PersonalService
│   ├── repository/  → JPA repositories
│   └── model/       → Persona, Paciente, Personal, Usuario, etc.
├── maestras/
│   ├── service/     → Cie10Service
│   ├── repository/  → Cie10Repository, MedicamentoRepository
│   └── model/       → Cie10, Medicamento, Servicio, etc.
└── exception/       → AlergiaActivaException
```

## Principios aplicados

- **SRP**: ConsultaService delega recetas a RecetaService; DashboardService separado de MainController
- **DIP/OCP**: 11 interfaces `I*Service` para toda la capa de negocio
- **Validación**: `@Valid` + `BindingResult` en endpoints POST (Triaje, Paciente, Personal)
- **Logging estructurado**: SLF4J + Lombok `@Slf4j` en 21/21 clases de producción
- **CSRF**: Protección activa, exenta solo para endpoints JSON específicos
- **DTOs tipados**: `ConsultaRequest` + `ApiResponse<T>` reemplazan `Map<String, Object>`
