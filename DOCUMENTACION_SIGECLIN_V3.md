# 🏥 SIGECLIN Premium v3.0 - Manual de Arquitectura y Lógica de Negocio

Este documento detalla la lógica interna, la arquitectura de software y el ciclo de vida de los procesos dentro del **Sistema de Gestión Clínica (SIGECLIN)**. 

---

## 1. 🏗️ Arquitectura del Sistema

SIGECLIN v3.0 es una aplicación monolítica estructurada bajo un patrón multicapas de alto rendimiento (Modelo-Vista-Controlador Híbrido), optimizado para procesamiento de datos clínicos e inteligencia de negocios (BI).

### 1.1 Stack Tecnológico
*   **Lenguaje & Core Backend:** Java 17, Spring Boot 3.2.x.
*   **Motor de Base de Datos:** PostgreSQL 15+.
*   **Capa de Persistencia (Híbrida):** 
    *   **Hibernate / Spring Data JPA:** Utilizado para la gestión de entidades complejas, mapeo de objetos, relaciones bidireccionales y flujos de inserción (Filiación, Mantenimiento).
    *   **Spring JdbcTemplate:** Utilizado estratégicamente para operaciones transaccionales críticas, cruces masivos de tablas en memoria y el Motor de Reportes Financieros, donde el rendimiento SQL es vital.
*   **Capa de Seguridad:** Spring Security 6 (manejo de sesiones, Password Encoding B-Crypt, interceptores y protección CSRF).
*   **Motor de Interfaz de Usuario (Frontend):** Thymeleaf integrado nativamente con Server-Side Rendering (SSR). Estilos potenciados por Bootstrap 5 y arquitectura de diseño **Glassmorphism Premium** mediante variables CSS.
*   **Exportación de Datos:** Apache POI para generación dinámica de Archivos Excel (.xlsx).

### 1.2 Modelado de Base de Datos Estructurada
La base de datos está segregada por esquemas (`schemas`) para evitar el acoplamiento cruzado y mantener una escalabilidad corporativa:
1.  **Esquema `filiacion`**: Toda la demografía (Persona, Paciente, Personal, Usuario, Tipo de Documento).
2.  **Esquema `clinico`**: Todo lo asistencial y financiero transaccional (Cita, Triaje, Consulta, Receta Médica, Pago/Caja, Laboratorio).
3.  **Esquema `maestras`**: Catálogos invariables del sistema (CIE-10, Especialidades, Medicamentos, Exámenes).
4.  **Esquema `seguridad`**: Logs, auditoría forense (tablas `_aud`), configuraciones del servidor y permisos.

---

## 2. 🔄 Flujo del Proceso Clínico (Patient Journey)

El sistema impone un ciclo de vida estricto para las atenciones médicas, integrando el recaudo financiero con el proceso médico.

### FASE 1: Admisión e Historias Clínicas (`filiacion.paciente`)
1. El paciente se presenta al establecimiento.
2. Admisión verifica la existencia del DNI/Documento.
3. Se genera un registro único conectando `filiacion.persona` (datos universales) con `filiacion.paciente` (datos de salud y emergencia).
4. El sistema genera o verifica un **Número de Historia Clínica Único**.

### FASE 2: Caja y Registro de Pago (`clinico.pago_log`)
1. El paciente solicita el servicio médico (por especialidad).
2. Se procesa el abono en el módulo de Caja (`CajaController`).
3. El pago se almacena transaccionalmente en la tabla de trazabilidad `pago_log`.
4. **Disparador Lógico (Trigger App):** El estado del paciente pasa a `"PENDIENTE_TRIAJE"`, habilitándolo para pasar al área de pre-evaluación.

### FASE 3: Tópico y Triaje (`clinico.triaje`)
1. La Enfermera recibe al paciente en el módulo de Triaje.
2. Se ingresan los Signos Vitales (Presión, Temperatura, FC, FR).
3. **Automatización:** El servidor de base de datos intercepta el Peso y la Talla, calcula automáticamente el **Índice de Masa Corporal (IMC)** y genera un diagnóstico nutricional en tiempo real mediante `STORED GENERATED COLUMNS`.

### FASE 4: Consulta Externa (`clinico.consulta`)
1. El Médico Tratante atiende al paciente en el consultorio.
2. Accede a la Visor de Historia Clínica resumiendo el estado previo y las constantes vitales recientes de Triaje.
3. El facultativo redacta la Anamnesis, el Examen Físico y emite Diagnósticos avalados bajo la codificación internacional CIE-10 (Buscador indexado).
4. **Atención Concluida:** El paciente queda en estado "Finalizado" y abandona el flujo operativo del día.

### FASE 5: Farmacia y Apoyo Diagnóstico
*   **UI Estricta:** Si el médico prescribe medicinas o exámenes, el módulo de interfaz fuerza internamente un diseño de tipografía en **Mayúsculas** (`UPPERCASE`), estandarizando internacionalmente la legibilidad de indicaciones médicas (prevención de iatrogenias).

---

## 3. 🛡️ Auditoría Forense y Seguridad

Para el cumplimiento con las normativas internacionales de protección de datos en salud (como HIPAA o las directivas del MINSA):
*   **Hibernate Envers:** Inyectado a nivel `pom.xml`. Este subsistema clona todos los eventos de manipulación (`INSERT`, `UPDATE`, `DELETE`) de los objetos más delicados del sistema (Historias Clínicas, Recetas, Consultas). 
*   **Trazabilidad Inmutable:** Cada cambio guarda un identificador de Revisión (`REV`), un Tipo de Revisión y asocia el cambio exacto al **Usuario en Sesión** que lo ejecutó, protegiendo al establecimiento ante peritajes legales.

---

## 4. 📊 Inteligencia de Negocios: Reporteador HISMINSA

Para el cruce multidimensional de datos clínicos y contables, se desarrolló un motor exportador a Excel:
1.  **Filtrado de Frontera:** Permite auditar ventanas de tiempo (`Diario`, `Semanal`, `Mensual`, `Anual`, `Personalizado`) y especialidades médicas específicas a través de la API `LocalDate`.
2.  **Consulta SQL Directa (`ReporteExcelService`):** Cruza 8 tablas diferentes forzando el mapeo de fechas a nivel `java.sql.Date` e igualando transacciones de `pago_log` contra `consulta` en tiempo real. Esto vincula las **Ganancias Económicas (S/)** con el esfuerzo médico desplegado en ese consultorio.
3.  **Generación de Archivo (POI):** Construye binariamente (Byte-streams) un `.xlsx` con una estructura de datos inspirada en los cuadros HISMINSA, utilizando celdas combinadas, agrupadores visuales (Grises, Azules y Verdes institucionales) y formateo de datos exactos (Conversión limpia de montos sin errores de clase `ClassCastException`).

---
_Documento auto-generado bajo el esquema de trabajo SIGECLIN v3.0 - Julio 2026_
