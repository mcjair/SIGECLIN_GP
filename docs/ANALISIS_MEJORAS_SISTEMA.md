# 🔬 Análisis de Alineación de Requerimientos y Oportunidades de Mejora (SIGECLIN)

Este documento detalla el análisis de cruce (Matriz de Trazabilidad) entre los requerimientos teóricos/prácticos del plan del curso y el estado actual del desarrollo de **SIGECLIN**, identificando brechas (Gap Analysis) y proponiendo planes de mejora específicos para elevar la calidad, seguridad y robustez del sistema.

---

## 🎯 1. Matriz de Trazabilidad de Requerimientos (RTM)

A continuación, se cruza la lista de requerimientos definidos en el alcance del proyecto contra el estado de implementación en el software funcionando:

| Cód. Req | Tipo | Descripción del Requerimiento | Estado | Archivo / Componente de Evidencia |
| :--- | :--- | :--- | :--- | :--- |
| **RF-01** | Funcional | Registro de Datos de Filiación del Paciente | **100% Cumplido** | [`PacienteController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/filiacion/controller/PacienteController.java) |
| **RF-02** | Funcional | Registro de Transacción en Caja y Pago de Consultas | **100% Cumplido** | [`CajaController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/controller/CajaController.java) |
| **RF-03** | Funcional | Módulo de Triaje con cálculo automático de IMC | **100% Cumplido** | [`TriajeService.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/TriajeService.java) |
| **RF-04** | Funcional | Alertas de Seguridad en Constantes Vitales | **100% Cumplido** | [`TriajeService.java` - `evaluarAlertasClinicas`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/TriajeService.java) |
| **RF-05** | Funcional | Búsqueda Autocompletable de CIE-10 en <50ms | **100% Cumplido** | [`Cie10RestController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/maestras/controller/Cie10RestController.java) |
| **RF-06** | Funcional | Guardado transaccional Consulta + Receta + Diagnósticos | **100% Cumplido** | [`ConsultaService.java` - `guardarConsultaCompleta`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/ConsultaService.java) |
| **RF-07** | Funcional | Detección de Alergias Medicamentosas en Prescripción | **100% Cumplido** | [`RecetaService.java` - `emitirReceta`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/RecetaService.java) |
| **RF-08** | Funcional | Bandejas de Apoyo (Farmacia y Laboratorio) | **100% Cumplido** | [`ApoyoDiagnosticoController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/controller/ApoyoDiagnosticoController.java) |
| **RF-09** | Funcional | Exportación a Reportes de Calidad (Excel) | **100% Cumplido** | [`GestionPacienteController.java` - Apache POI](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/filiacion/controller/GestionPacienteController.java) |
| **RNF-01** | Seguridad | Control de acceso basado en Roles y Permisos | **100% Cumplido** | [`SecurityConfig.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/config/SecurityConfig.java) |
| **RNF-02** | Seguridad | Encriptación Hash e inmutabilidad de contraseñas | **100% Cumplido** | [`CustomUserDetailsService.java` - BCrypt](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/seguridad/service/CustomUserDetailsService.java) |
| **RNF-03** | Calidad | Pruebas Unitarias y Cobertura de Línea > 10% | **100% Cumplido** | [`pom.xml` - Plugin Jacoco](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/pom.xml#L127-L160) (46 tests exitosos, ~15% cov) |
| **RNF-04** | Seguridad | Auditoría Activa ante accesos clínicos (MINSA) | **100% Cumplido** | [`AuditoriaClinicaAspect.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/config/AuditoriaClinicaAspect.java) |

---

## 🔍 2. Análisis de Brechas (Gap Analysis) y Oportunidades de Mejora

Realizando una comparación profunda entre las mejores prácticas de arquitectura de software para el nivel corporativo y el estado actual, se identifican las siguientes **oportunidades de mejora**:

### 📊 A. Módulo de Consulta Médica (Validación Interactiva de Alergias en Frontend)
*   **Estado actual:** Cuando el médico prescribe un fármaco al que el paciente es alérgico, el sistema detiene la transacción en la capa de negocio (`RecetaService`) lanzando una excepción `AlergiaActivaException`. Esto se renderiza como un mensaje de error HTTP 409.
*   **Oportunidad de Mejora:** Integrar una validación interactiva asíncrona en el frontend (vía JavaScript/AJAX). Al momento de seleccionar el medicamento en el buscador, el sistema debe consultar las alergias del paciente registradas en la columna izquierda y mostrar una advertencia visual inmediata (bloqueando el botón de adición o exigiendo una confirmación especial al médico), antes de enviar el formulario completo.

### 🔒 B. Módulo de Seguridad y Auditoría (Trazabilidad Ampliada)
*   **Estado actual:** La auditoría se realiza a través de un aspecto AOP (`AuditoriaClinicaAspect`) que intercepta la ejecución de métodos del servicio clínico.
*   **Oportunidad de Mejora:** Ampliar la auditoría para capturar eventos de seguridad críticos como:
    1.  Intentos fallidos de inicio de sesión persistidos en la base de datos para auditorías de intrusión.
    2.  Modificaciones a los registros del Personal de Salud (CRUD) para evitar alteraciones fraudulentas de médicos/colegiaturas.
    3.  Exportación de datos de Pacientes (descarga de Excel), capturando qué usuario descargó la lista de ciudadanos.

### 🌐 C. DevOps y Operaciones (Métricas Personalizadas en Actuator)
*   **Estado actual:** Spring Actuator está integrado en `pom.xml`, exponiendo métricas genéricas de salud del sistema, CPU y memoria.
*   **Oportunidad de Mejora:** Implementar contadores y métricas personalizadas de negocio clínicas mediante la clase `MeterRegistry` de Micrometer. Por ejemplo:
    *   Un contador de "Alertas Clínicas críticas en Triaje" por hora.
    *   Una métrica de tiempo de procesamiento de consulta médica promedio.
    Esto permite conectar el sistema a herramientas de monitoreo como Prometheus/Grafana a nivel de salud del MINSA.

### 🧪 D. Incremento de la Cobertura de Pruebas a > 20%
*   **Estado actual:** El proyecto cuenta con un límite mínimo del 10% en el plugin Jacoco y cuenta con 46 pruebas unitarias implementadas que cubren seguridad, inicializadores y flujos de triaje.
*   **Oportunidad de Mejora:** Escribir pruebas unitarias adicionales enfocadas en las capas más complejas como `ConsultaService` (casos de éxito de atención y fallos por alergias medicamentosas) y validación de tipos de documento, elevando el umbral de Jacoco a **20%** o **30%** para blindar el sistema contra regresiones de código.

---

## 📈 3. Plan de Acción y Ejecución de Mejoras

Para la sustentación final y demostración de evolución del software, las siguientes mejoras representan el mayor valor académico y técnico:

1.  **Mejora 1 (Seguridad - Auditoría Ampliada):** Interceptar el controlador de exportación a Excel y el controlador de Personal para guardar logs de auditoría inmutables en la base de datos.
2.  **Mejora 2 (Frontend - Alertas de Alergias Interactivas):** Añadir validación interactiva en `consulta_espera.html` para cruzar en tiempo real la lista de alergias del panel izquierdo con los fármacos seleccionados por el médico.
3.  **Mejora 3 (Calidad - Incremento de Cobertura):** Agregar pruebas unitarias enfocadas en `ConsultaService` para validar la lógica del diagnóstico y la receta médica.
