# 📋 Validación y Checklist de Entregables - SIGECLIN

Este documento sirve como **Checklist de Conformidad de Entregables** para el proyecto **SIGECLIN** (Sistema de Gestión Clínica - Centro de Salud CLAS Grocio Prado). Muestra de forma detallada dónde se ubica cada artefacto dentro del código fuente y los documentos de soporte, facilitando la auditoría y sustentación académica.

---

## 📂 1. Índice de Documentación General y Académica

| Requisito Académico / Sección | Ubicación en el Repositorio | Estado | Detalle y Cobertura |
| :--- | :--- | :--- | :--- |
| **Capítulo 1 - Aspectos Generales** | [`docs/PLANIFICACION_PROYECTO_PMBOK.md`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md#L25) | **100% Cumplido** | Define la justificación, objetivos del proyecto, el *Caso de Negocio* y límites del alcance. |
| **Capítulo 2 - Marco Teórico** | [`docs/LOGICA_FLUJO_SISTEMA.md`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/LOGICA_FLUJO_SISTEMA.md#L25) | **100% Cumplido** | Justificación técnica del stack tecnológico (Spring Boot 3.2.5, Thymeleaf, PostgreSQL, caché Guava, BCrypt). |
| **Capítulo 3 - Desarrollo de Solución** | [`docs/LOGICA_FLUJO_SISTEMA.md`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/LOGICA_FLUJO_SISTEMA.md#L185) | **100% Cumplido** | Detalla el flujo paso a paso del paciente, lógica de negocio clínica y patrones implementados. |
| **Bibliografía** | [`docs/PLANIFICACION_PROYECTO_PMBOK.md`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md) | **100% Cumplido** | Basado en el PMBOK Guide, estándares de seguridad OWASP y guías oficiales de Spring. |

---

## 🛠️ 2. Entregables de Gestión de Proyecto y Diagramas (Anexos)

### 🎨 A. Lean Canvas (Modelo de Negocio)
* **Ubicación:** [`docs/PLANIFICACION_PROYECTO_PMBOK.md` - Sección 1](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md#L7-L18)
* **Fórmula:** Cuadrícula de 9 bloques (Problema, Solución, Propuesta Única de Valor, Ventaja Injusta, Segmentación, Canales, Métricas, Costos e Ingresos).
* **Validación:** Completamente documentada en formato de matriz de Markdown.

### 📄 B. Acta de Constitución del Proyecto (Project Charter)
* **Ubicación:** [`docs/PLANIFICACION_PROYECTO_PMBOK.md` - Sección 2](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md#L21-L53)
* **Fórmula:** Basado en estándares PMI/PMBOK. Detalla Patrocinador, Director de Proyecto (MC Jair), Caso de Negocio, Criterios de Éxito, Restricciones y Supuestos.
* **Validación:** 100% Estructurado y validado.

### 🪵 C. WBS / EDT (Estructura de Desglose de Trabajo)
* **Ubicación:** [`docs/PLANIFICACION_PROYECTO_PMBOK.md` - Sección 3](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md#L55-L119)
* **Fórmula:** Diagrama de desglose estructurado en hitos y entregables de la Unidad 1 a la Unidad 4.
* **Representación:** Diagrama interactivo **Mermaid.js** (tipo Árbol Jerárquico).

### 📅 D. Diagrama de Gantt (Cronograma)
* **Ubicación:** [`docs/PLANIFICACION_PROYECTO_PMBOK.md` - Sección 4](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md#L121-L151)
* **Fórmula:** Cronograma secuencial de 18 semanas escolares agrupadas por fases (Planificación, Diseño, Desarrollo, Ops y Cierre).
* **Representación:** Diagrama interactivo **Mermaid.js** de Gantt renderizable.

### 🖼️ E. Mockup del Prototipado en Alta (Alternativas de Solución)
* **Ubicación:** [`docs/prototipos/README.md`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/prototipos/README.md)
* **Fórmula:** Define wireframes y esquemas estructurados de los módulos (Login, Dashboard, Admisión, Caja, Triaje, y la Consulta Médica en 3 columnas), indicando paleta de colores y soporte de modo oscuro.
* **Sustentación:** Las vistas funcionales del sistema (HTML/CSS con Bootstrap Premium y Glassmorphism) operan como el prototipo interactivo en alta fidelidad.

### 🔄 F. Diagrama de Procesos AS-IS / TO-BE
* **Ubicación:** [`docs/bpmn/flujo_atencion.bpmn`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/bpmn/flujo_atencion.bpmn)
* **Lógica:** El archivo BPMN modela el estado TO-BE optimizado. Además, el flujo secuencial de atención (Admisión ➔ Caja ➔ Triaje ➔ Consulta) se detalla gráficamente en la sección de Diagrama de Secuencia de Pacientes en:
  * [`SIGECLIN_LOGICA_Y_FLUJO.md` - Sección 2](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/SIGECLIN_LOGICA_Y_FLUJO.md#L26-L51)

### 🧩 G. Diagrama de Clases
* **Ubicación y Mapeo:**
  * Estructura de Paquetes detallada en [`docs/LOGICA_FLUJO_SISTEMA.md` - Sección 11](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/LOGICA_FLUJO_SISTEMA.md#L432-L487).
  * Las clases del modelo están implementadas en el paquete `com.sigeclin.*.model` (con anotaciones JPA como `@Entity`, `@Inheritance`, `@OneToOne`, etc.).

### 🗄️ H. Diagrama Entidad-Relación (DER)
* **Ubicación:** [`docs/DISEÑO_BASE_DATOS_MER.md` - Sección 1](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/DISEÑO_BASE_DATOS_MER.md#L7-L148)
* **Representación:** Diagrama de Base de Datos Físico completo usando **Mermaid.js**, detallando PKs, FKs, tipos de campos y relaciones de cardinalidad de todos los esquemas (`filiacion`, `clinico`, `maestras`, `seguridad`).

### 📋 I. Lista de Requerimientos (SRS)
* **Ubicación:** Detallados funcionalmente como parte de los objetivos y el alcance del sistema en:
  * [`docs/PLANIFICACION_PROYECTO_PMBOK.md` - Sección 2.3](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md#L35-L40)

---

## 💻 3. Software Funcionando (Validación del 50% de Requerimientos)

El sistema excede ampliamente el mínimo académico del 50%, contando con un flujo punta a punta operativo y validado en tests:

1.  **Módulo de Admisión (Filiación)**:
    *   Formulario de registro y búsqueda automática de paciente por DNI.
    *   Filiación unificada bajo la jerarquía `Persona`.
2.  **Módulo de Caja (Pagos)**:
    *   Procesamiento de cobranza de servicio e inserción JDBC directa de auditoría de pagos en `pago_log`.
    *   Actualización automática de estado de paciente a `PENDIENTE_TRIAJE`.
3.  **Módulo de Triaje**:
    *   Registro de constantes vitales (PA, FC, Temp, SpO2, Peso, Talla).
    *   **Lógica de Alertas Clínicas automática**: Identificación en tiempo real de Hipertensión, Taquicardia, Fiebre, Hipoxia, etc.
4.  **Bandeja y Modulos Clínicos (Cola de Espera)**:
    *   Separación por especialidades (Medicina General, Odontología, Enfermería, etc.) filtrando por estados dinámicos del paciente.
5.  **Módulo de Atención Médica (Detalle)**:
    *   Layout Premium de **3 Columnas** (Signos Vitales y Alergias ➔ Diagnóstico e indicaciones ➔ Buscador interactivo CIE-10).
    *   Buscador autocompletable CIE-10 en tiempo real con más de 380 diagnósticos registrados.
6.  **Documentos Clínicos (Popups Centrados)**:
    *   Impresión de **Receta Médica** dinámica (con llenado de filas de relleno).
    *   Impresión de **Certificado Médico** e **Informe de Referencia**.
7.  **Módulos de Soporte**:
    *   Bandeja de **Farmacia** (Dispensación de recetas médicas prescritas).
    *   Bandeja de **Laboratorio** (Órdenes de exámenes de apoyo al diagnóstico).
8.  **Gestión de Personal**:
    *   CRUD completo del personal de salud con validación de roles y permisos.
9.  **Exportación de Datos**:
    *   Descarga de reporte de pacientes en formato Excel real (`.xlsx`) mediante **Apache POI**.

---

## 📐 4. Evidencias Arquitectónicas y de Desarrollo

### 🏛️ A. Implementación de Arquitectura y MVC
*   **Separación de Capas (MVC)**:
    *   **M (Model)**: Clases en `com.sigeclin.*.model` anotadas con JPA que mapean los esquemas físicos de base de datos.
    *   **V (View)**: Plantillas dinámicas en `src/main/resources/templates/` escritas en Thymeleaf.
    *   **C (Controller)**: Clases en `com.sigeclin.*.controller` que exponen endpoints HTTP y devuelven vistas o payloads REST.
    *   **Service & DAO**: Capa de servicios (`com.sigeclin.*.service`) que implementa la lógica de negocio y repositorios JPA (`com.sigeclin.*.repository`) que heredan de `JpaRepository` para el acceso a datos.

### 🌟 B. Buenas Prácticas de Programación
*   **Inyección de Dependencias**: Uso exclusivo de `@Autowired` y constructores parametrizados para cumplir con el Principio de Inversión de Dependencia.
*   **Encapsulamiento y DTOs**: Transferencia segura de datos mediante DTOs estructurados (`ApiResponse`, `ConsultaRequest`) para evitar sobreexponer entidades de base de datos.
*   **Seguridad**: Encriptación hash robusta mediante **BCrypt**, control de accesos cruzados por roles (Spring Security) y prevención activa de fuerza bruta (bloqueo automático tras 5 intentos fallidos).
*   **Caché en Memoria**: Uso de caché para agilizar la carga del catálogo CIE-10.
*   **Logs Limpios**: Uso exclusivo de la API de logging **SLF4J / Logback** para auditoría y traza de errores, evitando `System.out.println`.

### 🐙 C. Control de Versiones (GitHub)
*   Repositorio local inicializado con estructura de Git.
*   Control de exclusión configurado mediante archivo `.gitignore` detallado para proyectos Maven y entornos de desarrollo (IDE, compilados).
*   Estructura lista para integración con GitHub en la carpeta de configuración `.github/`.
