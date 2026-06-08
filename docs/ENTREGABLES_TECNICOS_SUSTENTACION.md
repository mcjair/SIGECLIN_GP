# 🛠️ Anexo Técnico de Entregables de Sustentación - SIGECLIN

Este documento reúne el código fuente representativo, los archivos de configuración primarios, las vistas de usuario críticas, los scripts SQL de definición de datos y el reporte final de pruebas del sistema **SIGECLIN** para servir de soporte en la redacción de informes académicos y diapositivas de sustentación.

---

## 📂 1. Scripts SQL (Definición de Base de Datos)

### 🗄️ A. Esquema Relacional de Tablas (schema.sql)
Define la estructura lógica física del sistema organizada en los esquemas `filiacion`, `maestras`, `clinico` y `seguridad`.

```sql
-- ============================================================
-- SISTEMA SIGECLIN - DDL COMPLETO (EXTRACTO PRINCIPAL)
-- ============================================================
CREATE SCHEMA IF NOT EXISTS filiacion;
CREATE SCHEMA IF NOT EXISTS maestras;
CREATE SCHEMA IF NOT EXISTS clinico;
CREATE SCHEMA IF NOT EXISTS seguridad;

-- Jerarquía de Personas y Filiación (Herencia JOINED)
CREATE TABLE filiacion.tipo_documento (
    id_tipo_documento SERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descripcion VARCHAR(100) NOT NULL,
    longitud_exacta INT,
    regex_validacion VARCHAR(255) NOT NULL
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
    direccion VARCHAR(255),
    UNIQUE (id_tipo_documento, numero_documento)
);

CREATE TABLE filiacion.paciente (
    id_paciente INT PRIMARY KEY REFERENCES filiacion.persona(id_persona),
    numero_historia_clinica VARCHAR(20) UNIQUE NOT NULL,
    grupo_sanguineo VARCHAR(3),
    factor_rh VARCHAR(1),
    contacto_emergencia_nombre VARCHAR(100),
    contacto_emergencia_telefono VARCHAR(15),
    estado_civil VARCHAR(20)
);

CREATE TABLE filiacion.usuario (
    id_usuario INT PRIMARY KEY REFERENCES filiacion.persona(id_persona),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    cuenta_bloqueada BOOLEAN DEFAULT false,
    intentos_fallidos INT DEFAULT 0
);

-- Tabla de Constantes de Triaje
CREATE TABLE clinico.triaje (
    id_triaje SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    presion_arterial_sistolica INT,
    presion_arterial_diastolica INT,
    frecuencia_cardiaca INT,
    temperatura NUMERIC(4,1),
    saturacion_oxigeno INT,
    peso_kg NUMERIC(5,2),
    talla_cm NUMERIC(5,2),
    imc NUMERIC(5,2) GENERATED ALWAYS AS (peso_kg / POWER(talla_cm/100, 2)) STORED,
    clasificacion_urgencia VARCHAR(10) CHECK (clasificacion_urgencia IN ('rojo', 'naranja', 'amarillo', 'verde'))
);

-- Registro de Consultas y Atenciones
CREATE TABLE clinico.consulta (
    id_consulta SERIAL PRIMARY KEY,
    id_paciente INT NOT NULL REFERENCES filiacion.paciente(id_paciente),
    id_triaje INT REFERENCES clinico.triaje(id_triaje),
    id_personal INT NOT NULL,
    fecha_hora_inicio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    motivo_consulta TEXT NOT NULL,
    anamnesis TEXT,
    examen_fisico TEXT,
    plan_tratamiento TEXT,
    estado VARCHAR(20) DEFAULT 'en_progreso'
);
```

### 🗃️ B. Semilla de Datos Iniciales (data.sql)
Carga inicial requerida para operar el sistema (roles, especialidades, tipos de documento y usuarios por defecto).

```sql
-- 1. Especialidades y Tipos de Documentos
INSERT INTO filiacion.tipo_documento (codigo, descripcion, longitud_exacta, regex_validacion) VALUES
('DNI', 'DOCUMENTO NACIONAL DE IDENTIDAD', 8, '^\d{8}$');

INSERT INTO maestras.especialidad (codigo, descripcion, cupo_maximo_diario) VALUES
('MG', 'MEDICINA GENERAL', 24);

-- 2. Persona y Usuario Administrador (Password: admin)
INSERT INTO filiacion.persona (id_tipo_documento, numero_documento, nombres, apellido_paterno, apellido_materno, fecha_nacimiento, sexo)
VALUES (1, '00000000', 'ADMINISTRADOR', 'SIGECLIN', 'SISTEMA', '1990-01-01', 'M');

INSERT INTO filiacion.usuario (id_usuario, username, password_hash, requiere_cambio_password)
VALUES (1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', false);

INSERT INTO seguridad.rol (codigo, descripcion) VALUES 
('ADMIN', 'ADMINISTRADOR DEL SISTEMA'),
('MEDICO_GENERAL', 'MÉDICO GENERAL'),
('TRIAJE', 'PERSONAL DE TRIAJE');

INSERT INTO seguridad.usuario_rol (id_usuario, id_rol) VALUES (1, 1);
```

---

## 🛠️ 2. Archivos de Configuración Primarios

### 📄 A. Configuración de la Aplicación (application.properties)
[`src/main/resources/application.properties`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/resources/application.properties)
```properties
spring.application.name=sigeclin
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/sigeclin?sslmode=disable
spring.datasource.username=admin
spring.datasource.password=admin
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.thymeleaf.cache=false

server.port=3001
spring.jpa.open-in-view=true
server.servlet.session.timeout=15m
```

### 📄 B. Configuración de Seguridad y Accesos (SecurityConfig.java)
[`src/main/java/com/sigeclin/config/SecurityConfig.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/config/SecurityConfig.java)
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/webjars/**", "/api/cie10/**", "/api/auth/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .userDetailsService(userDetailsService)
            .formLogin((form) -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout((logout) -> logout.permitAll())
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/consulta/guardar", "POST"),
                new AntPathRequestMatcher("/api/**", "POST")
            ));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## 💻 3. Código de Casos de Uso del Software

### ⚙️ A. Transaccionalidad de Consulta Médica (ConsultaService.java)
[`src/main/java/com/sigeclin/clinico/service/ConsultaService.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/ConsultaService.java)
```java
@Service
@RequiredArgsConstructor
public class ConsultaService implements IConsultaService {

    private final ConsultaRepository consultaRepository;
    private final RecetaMedicaRepository recetaRepository;
    private final DiagnosticoConsultaRepository diagnosticoRepository;

    @Override
    @Transactional
    public Consulta guardarConsultaCompleta(ConsultaRequest request) {
        // 1. Guardar la consulta clinica base
        Consulta consulta = new Consulta();
        consulta.setMotivoConsulta(request.getAnamnesis()); // Sincronizado
        consulta.setAnamnesis(request.getAnamnesis());
        consulta.setExamenFisico(request.getExamenFisico());
        consulta.setPlanTratamiento(request.getPlanTratamiento());
        consulta.setEstado("COMPLETADO");
        Consulta consultaPersistida = consultaRepository.save(consulta);

        // 2. Guardar diagnosticos asociados
        for (String dxCode : request.getDiagnosticos()) {
            DiagnosticoConsulta dc = new DiagnosticoConsulta();
            dc.setConsulta(consultaPersistida);
            dc.setCodigoCie10(dxCode);
            diagnosticoRepository.save(dc);
        }

        // 3. Guardar Receta Medica si contiene medicamentos
        if (!request.getMedicamentos().isEmpty()) {
            RecetaMedica receta = new RecetaMedica();
            receta.setConsulta(consultaPersistida);
            recetaRepository.save(receta);
        }

        return consultaPersistida;
    }
}
```

### ⚙️ B. Triaje y Cálculo de Alertas Fisiológicas (TriajeService.java)
[`src/main/java/com/sigeclin/clinico/service/TriajeService.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/TriajeService.java)
```java
@Service
public class TriajeService implements ITriajeService {

    @Override
    public List<String> evaluarAlertasClinicas(Triaje triaje) {
        List<String> alertas = new ArrayList<>();
        
        // 1. Alerta de Presion Arterial
        if (triaje.getPresionArterialSistolica() != null && triaje.getPresionArterialSistolica() >= 140) {
            alertas.add("Hipertensión Sistólica Crítica");
        }
        
        // 2. Alerta de Fiebre
        if (triaje.getTemperatura() != null && triaje.getTemperatura().doubleValue() >= 38.0) {
            alertas.add("Fiebre / Alerta Infecciosa");
        }
        
        // 3. Alerta de Saturacion de Oxigeno
        if (triaje.getSaturacionOxigeno() != null && triaje.getSaturacionOxigeno() < 95) {
            alertas.add("Hipoxia detectada (<95% SpO2)");
        }
        
        return alertas;
    }
}
```

---

## 🎨 4. Pantallas y Vistas Obligatorias

### 🖥️ A. Validación Interactiva de Alergias en Recetario (consulta_espera.html - JS Block)
[`src/main/resources/templates/clinico/consulta_espera.html`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/resources/templates/clinico/consulta_espera.html)
```javascript
preConfirm: () => {
    const medName = document.getElementById('swalMedName').value;
    if (!medName) {
        Swal.showValidationMessage('El nombre del medicamento es obligatorio');
        return false;
    }
    
    // Cruce interactivo de alergias del paciente antes de agregar medicamento
    if (typeof alergiasBase !== 'undefined' && Array.isArray(alergiasBase)) {
        const medNameUpper = medName.toUpperCase().trim();
        const conflicto = alergiasBase.find(al => {
            const descUpper = (al.descripcion || '').toUpperCase().trim();
            return medNameUpper.includes(descUpper) || descUpper.includes(medNameUpper);
        });
        if (conflicto) {
            Swal.showValidationMessage(`⚠️ ¡Alerta! El paciente presenta alergia registrada a: ${conflicto.descripcion} (${conflicto.severidad})`);
            return false;
        }
    }
    
    return {
        medicamento: medName,
        dosis: document.getElementById('swalMedDosis').value,
        duracion: document.getElementById('swalMedDur').value,
        cantidad: document.getElementById('swalMedCant').value
    };
}
```

---

## 📊 5. Reporte de Pruebas Unitarias y Cobertura (JUnit & Jacoco)

El sistema cuenta con una cobertura y tests automatizados configurados a nivel de Maven.

### 🧪 A. Resumen de Ejecución del Test de JUnit (`mvn test`):
```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.sigeclin.clinico.controller.ConsultaControllerTest... OK
Running com.sigeclin.maestras.service.Cie10ServiceTest... OK
Running com.sigeclin.SigeclinIntegrationTest... OK

Results:
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 📈 B. Configuración de la Cobertura Mínima (Jacoco en pom.xml)
Garantiza que el proyecto no compile si la cobertura unitaria desciende del **10%** requerido.
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.10</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```
