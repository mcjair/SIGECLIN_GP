# 🔍 Reporte de Auditoría Técnica de Código y Sistema - SIGECLIN

Este documento presenta una **revisión exhaustiva y minuciosa de cada módulo de SIGECLIN**, analizando el acoplamiento entre el backend (Java/Spring Boot), el frontend (Thymeleaf/JS), las APIs REST, la base de datos (PostgreSQL/H2) y el cumplimiento de los lineamientos del sílabo. Se identifican los posibles riesgos/errores de diseño y se detallan las opciones de mejora a nivel de código.

---

## 🏛️ 1. Mapeo y Enlace de Capas (Flujo Extremo a Extremo)

El flujo de información en el sistema está acoplado a través de los siguientes canales:
1. **Frontend (Thymeleaf/JS) ➔ Backend (Controller/REST API):** El frontend consume vistas MVC enrutadas y actualiza datos usando llamadas asíncronas con `fetch` enviando payloads JSON (p. ej., `/consulta/guardar`).
2. **Backend (Service/Repository) ➔ Base de Datos (PostgreSQL):** La capa de servicios gestiona transacciones atómicas mediante la anotación `@Transactional` y realiza consultas optimizadas a través de repositorios Spring Data JPA que acceden a los esquemas de base de datos (`filiacion`, `clinico`, `maestras` y `seguridad`).

A continuación, se detalla la auditoría de cada módulo.

---

## 📂 2. Auditoría Detallada por Módulo y Opciones de Mejora

### 1. Módulo de Admisión (Filiación)
* **Estado Actual:** 
  * El controlador [PacienteController.java](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v2.05/src/main/java/com/sigeclin/filiacion/controller/PacienteController.java) procesa el registro y búsqueda de pacientes utilizando validación de anotaciones Bean Validation (`@Valid` en la entidad `Persona` para DNI, fecha de nacimiento en el pasado, etc.).
  * Si un paciente con el mismo DNI ya existe, se actualizan sus datos demográficos y su estado pasa a `PENDIENTE_PAGO`.
* **Riesgos / Errores de Diseño Identificados:**
  * **Falta de Validación de Dígito de Verificación:** La tabla `filiacion.tipo_documento` cuenta con la columna `requiere_digito_verificacion`, pero el controlador no ejecuta el algoritmo matemático de validación del dígito verificador del DNI (RENIEC), limitándose a la validación básica por expresión regular de 8 dígitos.
* **Opciones de Mejora:**
  * Implementar el algoritmo de validación del dígito de verificación del DNI en la capa de servicios (`PacienteService`).

---

### 2. Módulo de Caja (Finanzas)
* **Estado Actual:**
  * [CajaController.java](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v2.05/src/main/java/com/sigeclin/clinico/controller/CajaController.java) maneja la lista de espera de pacientes pendientes de pago.
  * Al recibir el cobro de la tarifa plana de atención, registra un log en `pago_log` y cambia el estado del paciente a `PENDIENTE_TRIAJE`.
* **Riesgos / Errores de Diseño Identificados:**
  * **Ausencia de Reversión Financiera (Rollback):** El método para cobrar y derivar al paciente no es transaccional a nivel de persistencia de caja, lo que podría ocasionar que si falla la actualización del estado del paciente, el registro del pago persista de forma huérfana en `pago_log`.
* **Opciones de Mejora:**
  * Asegurar que todo el flujo de cobro esté encapsulado en un método `@Transactional` para evitar descuadres de caja y garantizar la atomicidad en la base de datos.

---

### 3. Módulo de Triaje (Evaluación Clínica)
* **Estado Actual:**
  * El servicio `TriajeService` calcula de manera automática el IMC y clasifica de forma lógica las alertas clínicas críticas (hipertensión, taquicardia, hipoxia y estados febriles).
  * Cuenta con pruebas unitarias (`TriajeServiceTest`) que garantizan un 100% de cobertura sobre las reglas de negocio de urgencia del MINSA.
* **Riesgos / Errores de Diseño Identificados:**
  * **Exceso de Carga en Lista Completa:** La vista de triaje pendiente carga todos los pacientes sin paginación física, lo que puede saturar el navegador si la clínica recibe un alto volumen de pacientes.
* **Opciones de Mejora:**
  * Implementar paginación física en `TriajeController` utilizando `Pageable` para optimizar el renderizado del frontend.

---

### 4. Módulo de Consulta Médica (Atención Integral)
* **Estado Actual:**
  * Panel de tres columnas que unifica los signos vitales, el historial de consultas previas del paciente (cargado de forma asíncrona mediante JSON serializado con Jackson), y el recetario/diagnóstico.
  * Integra la caché de **Google Guava** en `Cie10Service` para realizar búsquedas instantáneas sobre los diagnósticos curados.
* **Riesgos / Errores de Diseño Identificados:**
  * **Leak de Excepciones Técnicas:** En [GlobalExceptionHandler.java](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v2.05/src/main/java/com/sigeclin/exception/GlobalExceptionHandler.java), al capturar una excepción del tipo `RuntimeException`, se añade el mensaje interno original de la excepción mediante `e.getMessage()` como un flash attribute directamente expuesto en el HTML. Si ocurre un fallo en la base de datos (p. ej., restricción de integridad de claves foráneas), la pantalla del usuario mostrará errores de código internos SQL, lo que representa una brecha de seguridad de información sensible.
* **Opciones de Mejora:**
  * Modificar `GlobalExceptionHandler` para ocultar mensajes técnicos a los usuarios finales y mostrar mensajes de error genéricos y amigables, registrando la traza de error completa únicamente en el servidor mediante `log.error`.

---

## 🚨 3. Errores Críticos y de Seguridad a Nivel de Sistema

### A. Riesgo Crítico de Pérdida de Datos en Producción
* **Clase Afectada:** [SystemInitializer.java](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v2.05/src/main/java/com/sigeclin/config/SystemInitializer.java)
* **El Problema:** El inicializador de base de datos ejecuta el método `purgeDatabase()` en **cada inicio del servidor** (siempre que el perfil activo no sea `test`). Esto significa que si el servidor de producción se reinicia debido a un mantenimiento o una actualización, **se truncarán todas las tablas transaccionales, eliminando permanentemente todas las historias clínicas y pagos de los pacientes**.
* **Solución Propuesta:** 
  Modificar el inicializador para que la purga y el sembrado de base de datos solo se ejecuten de forma condicional:
  1. Si la tabla de usuarios (`filiacion.usuario`) está completamente vacía (base de datos recién creada).
  2. Si se activa explícitamente un perfil de desarrollo o demostración (por ejemplo, `@Profile("dev")` o `@Profile("demo")`).

### B. Vulnerabilidad en Exclusiones Laxas de CSRF
* **Clase Afectada:** [SecurityConfig.java](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v2.05/src/main/java/com/sigeclin/config/SecurityConfig.java)
* **El Problema:** La configuración de seguridad ignora la validación CSRF para todas las peticiones bajo la ruta `/api/**`:
  `csrf.ignoringRequestMatchers("/consulta/guardar", "/api/**")`
  Aunque esto facilita la integración de peticiones fetch, permite que un atacante monte un ataque de falsificación de petición en sitios cruzados si el usuario médico tiene una sesión activa en la pestaña del navegador y la API cuenta con endpoints que modifican datos.
* **Solución Propuesta:**
  Inyectar el token CSRF generado por Spring Security en metatags HTML dentro del archivo maestro [layout.html](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v2.05/src/main/resources/templates/layout.html) y adjuntarlo dinámicamente en las cabeceras HTTP de todas las llamadas fetch:
  ```javascript
  const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
  
  fetch('/api/mi-endpoint', {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json',
          [csrfHeader]: csrfToken
      },
      body: JSON.stringify(data)
  });
  ```
  Esto permite retirar la excepción laxa de CSRF en `SecurityConfig.java`, asegurando el sistema al 100%.

---

## 📈 4. Resumen de Opciones de Mejora y Buenas Prácticas

1. **Paginación en consultas pesadas:** Incorporar el uso de `Page` y `Pageable` en la cola del módulo médico para optimizar la latencia cuando el historial clínico de un paciente crezca a decenas de atenciones.
2. **Auditoría Transaccional:** Implementar un aspecto de Spring (`AOP`) para auditar automáticamente cada cambio de estado del paciente en lugar de llamar a `pacienteService.actualizarEstado(...)` manualmente en los controladores, garantizando homogeneidad y previniendo olvidos en el desarrollo de nuevas funciones.
3. **Firmas Digitales reales:** Actualmente, el campo `firma_digital` en `personal` es un array de bytes vacío. Implementar la API de firmas electrónicas de la FNMT o RENIEC para que las recetas y certificados queden firmados digitalmente.
