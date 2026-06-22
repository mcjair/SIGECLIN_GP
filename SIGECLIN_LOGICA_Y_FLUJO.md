# 🏥 Documentación de Lógica y Flujo del Sistema SIGECLIN

Este documento detalla la arquitectura, los procesos de negocio, el flujo de atención al paciente y la lógica técnica implementada en el sistema **SIGECLIN (Sistema de Gestión Clínica)** hasta la fase actual de desarrollo.

---

## 🏗️ 1. Arquitectura Base y Tecnologías

El sistema está construido bajo el paradigma **MVC (Modelo-Vista-Controlador)** utilizando las siguientes tecnologías:
*   **Backend:** Java 17 + Spring Boot 3.x
*   **Seguridad:** Spring Security (Autenticación basada en sesiones, BCrypt, Roles: `ADMIN`, `MEDICO_GENERAL`, `TRIAJE`, etc.)
*   **Persistencia:** Spring Data JPA (Hibernate) + Spring JDBC (para reportes nativos de alto rendimiento).
*   **Base de Datos:** PostgreSQL (Esquemas separados: `filiacion`, `clinico`, `maestras`, `seguridad`).
*   **Frontend:** SSR (Server-Side Rendering) con Thymeleaf, HTML5, Vanilla JavaScript, CSS modular.
*   **Diseño UX/UI:** Estándar de diseño propio denominado **"Impeccable UI"** (Glassmorphism, jerarquía visual de alta densidad, tarjetas premium).

---

## 🛤️ 2. Flujo Operativo Principal (El "Patient Journey")

El ciclo de vida de un paciente dentro del sistema sigue un orden secuencial estricto, modelando un centro de salud real:

### A. Admisión y Filiación (`/admission/registro`)
1.  **Registro:** El paciente llega y entrega su DNI. Se valida si ya existe en la base de datos (BD).
2.  **Historia Clínica:** Si es nuevo, se le genera automáticamente un número de **Historia Clínica (HC)** y se registran sus datos demográficos (Tabla `filiacion.paciente`).
3.  **Derivación:** El recepcionista asigna al paciente al servicio solicitado (Ej: Medicina General, Odontología).

### B. Caja y Recaudación (`/caja/pago`)
1.  **Monitor de Cobros:** El paciente aparece en el panel de Caja en estado "PENDIENTE".
2.  **Pago:** El cajero visualiza el servicio, genera el cobro (calculando el monto basado en el servicio) e imprime el **Comprobante de Pago/Voucher**.
3.  **Transición:** Una vez pagado, el estado del paciente cambia y es enviado a la cola de Triaje.

### C. Triaje y Signos Vitales (`/triaje/nuevo` -> `/triaje/registrar/{id}`)
1.  **Identificación:** El enfermero(a) selecciona al paciente de la lista de espera de Triaje.
2.  **Captura Biométrica:** Se registran el Peso, Talla y se calcula matemáticamente el **IMC** en tiempo real vía JavaScript y validado en el Backend.
3.  **Funciones Vitales:** Se ingresan Presión Arterial, Frecuencia Cardíaca, Temperatura y Saturación de Oxígeno (SpO2).
4.  **Alertas Clínicas Automáticas:** La lógica en `TriajeService` evalúa si hay valores críticos (ej. Fiebre > 38°C, o SpO2 < 95%) y lanza una bandera de **Riesgo Clínico** para el médico.

### D. Consulta Médica (`/consulta/modulo/{servicio}` -> `/consulta/atender/{id}`)
1.  **Gestión de Colas:** El médico ingresa a su módulo (Ej: Nutrición) y ve solo a los pacientes derivados a su especialidad mediante tarjetas de alta densidad (`consulta_cola.html`).
2.  **Historial Clínico (Timeline):** Al abrir la consulta (`consulta_espera.html`), el sistema carga la línea de tiempo del paciente, mostrando cronológicamente atenciones pasadas, diagnósticos, fechas de próximo control (formateadas de ISO a DD/MM/YYYY) e historial de alergias.
3.  **Atención (4 Pasos Básicos):**
    *   **Anamnesis / Motivo de consulta.**
    *   **Examen Físico.**
    *   **Diagnóstico:** Búsqueda asíncrona ultrarrápida del catálogo CIE-10 (MINSA).
    *   **Plan y Tratamiento.**
4.  **Recetario Electrónico Seguro (Detección de Alergias):**
    *   *Lógica Frontend:* Si el médico intenta prescribir un fármaco (ej. Penicilina) y el paciente tiene alergia registrada a ese componente, se bloquea el modal de receta con una alerta `SweetAlert2`.
    *   *Lógica Backend:* Se reafirma el bloqueo a nivel de Servidor (`RecetaService`) lanzando una excepción si la validación del UI es vulnerada.
5.  **Cierre y Documentación:** Al guardar la consulta, de forma **Transaccional** (`@Transactional`), se guarda la consulta, los diagnósticos y las medicinas. Se habilitan los botones para imprimir: **Receta**, **Referencia** o **Certificado Médico**.

---

## ⚙️ 3. Lógica Especializada y Servicios Core

### 📊 A. Motor de Estadísticas (DashboardService)
El Dashboard (`/dashboard`) procesa grandes volúmenes de datos en milisegundos usando `JdbcTemplate`.
*   **Problema resuelto:** Los nombres de los servicios en la base de datos tenían diferentes codificaciones y tildes (ej. "NUTRICION", "Nutrición", "Nutrición ").
*   **Solución Técnica implementada:** Se reemplazó el cotejo estricto por sentencias SQL flexibles usando `ILIKE` (ej. `ILIKE 'NUTRIC%N'`) y sentencias `CASE WHEN` para estandarizar etiquetas en tiempo de ejecución. Esto garantiza que las barras de progreso y la carga diaria nunca proyecten un valor en "0" por errores ortográficos en la BD.

### 🛡️ B. Seguridad y Autenticación
*   Las contraseñas de los usuarios se almacenan encriptadas con algoritmo unidireccional `BCrypt`.
*   `SecurityConfig.java` restringe el acceso a las rutas clínicas. Solo el rol `ADMIN` o roles específicos (ej. `MEDICO_GENERAL`) pueden acceder a sus respectivas áreas.

### 💊 C. Manejo de Apoyo al Diagnóstico (Laboratorio)
*   **Procesamiento Inteligente:** Al visualizar informes de laboratorio, el sistema oculta automáticamente las filas de los exámenes que no fueron solicitados.
*   **Jerarquía de Severidad:** Diferencia visualmente los resultados NORMALES (verde tenue/gris) de aquellos que están FUERA DE RANGO (textos rojos/alertas visuales), facilitando la lectura rápida al médico.

---

## 🎨 4. Patrones de Diseño UX/UI Implementados

El sistema implementa el **"Impeccable UI Standard"**, un framework CSS local que define la estética de alto nivel de la clínica:
1.  **Glassmorphism (Panel de Cristal):** Uso de variables CSS (`--bg-card`, `--glass-border`) para crear contenedores semitransparentes con desenfoque de fondo (`backdrop-filter: blur`).
2.  **Feedback Asíncrono:** Todas las acciones destructivas o importantes (Guardar Consulta, Eliminar Diagnóstico, Notificar Alergia) utilizan la librería `SweetAlert2` renderizada con íconos premium.
3.  **Layout No-Scrollable:** Interfaces clínicas diseñadas para ocupar el `100vh` (alto total de la pantalla) divididas en paneles o columnas (`col-md-3`, `col-md-9`), evitando que el médico tenga que hacer scroll excesivo ("above the fold").
4.  **Skeleton Loaders:** Las vistas pesadas tienen esqueletos de carga animados CSS (`placeholder-glow`) mientras la data viaja desde el servidor hasta el DOM.

---
*Documento autogenerado para evidencia de desarrollo arquitectónico e hitos del sistema SIGECLIN.*
