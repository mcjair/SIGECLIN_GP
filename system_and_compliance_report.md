# 📊 Informe de Evaluación Técnica y Cumplimiento de Sílabo — SIGECLIN GP

Este informe evalúa el estado del sistema **SIGECLIN GP**, estructurado en tres secciones primordiales: **Backend**, **Frontend & Diseño**, y **Cumplimiento Académico** según las unidades del sílabo oficial del **Curso Integrador de Sistemas de Software**.

---

## 🛠️ 1. Diagnóstico del Backend

La arquitectura del backend es un diseño robusto basado en **Java 17 (LTS)** y **Spring Boot 3.2.5**, implementando el patrón clásico de capas (Controller ➔ Service ➔ Repository ➔ Entity).

### 🟢 Aspectos Fuertes del Backend:
1. **Modelado Físico y Herencia**: Uso correcto de `@Inheritance(strategy = InheritanceType.JOINED)` para el mapeo físico de `Persona`, `Paciente`, `Personal` y `Usuario`, respetando el diseño relacional y evitando duplicidad de columnas en PostgreSQL.
2. **Capa DAO Optimizada**: Implementación de `JpaRepository` combinado con `JpaSpecificationExecutor` en `PacienteRepository` para posibilitar filtrado dinámico mediante Criteria API.
3. **Caché en Memoria (Google Guava)**: Integración exitosa de `CacheBuilder` para búsquedas en la base de datos de diagnósticos CIE-10 en menos de **5 milisegundos**, aliviando la carga transaccional sobre PostgreSQL.
4. **Reportes Profesionales (Apache POI)**: Implementación para exportar padrones de personal y pacientes a archivos Excel (.xlsx) con fuentes y formatos definidos.
5. **Seguridad (Spring Security 6.x)**: Autenticación por roles (RBAC) con soporte de contraseñas hasheadas mediante **BCrypt** y controles de cabeceras HTTP de protección activa (CSP, X-Frame-Options para Clickjacking).

### 🔴 Riesgos y Deficiencias Críticas Detectadas:
*   **Pérdida de Datos en Producción (`SystemInitializer.java`)**: ⚠️ **CRÍTICO**. El inicializador del sistema ejecuta un método `purgeDatabase()` (truncar todas las tablas y resembrar) en **cada inicio del servidor** en cualquier perfil excepto `test`. Si el servidor se apaga o reinicia por mantenimiento en producción, toda la información de historias clínicas, recetas y pagos se borrará definitivamente.
*   **Exclusión de Protección CSRF Laxa (`SecurityConfig.java`)**: Para facilitar el desarrollo, se ignoró la validación CSRF para toda la ruta `/api/**`. Esto representa una vulnerabilidad crítica ante ataques *Cross-Site Request Forgery*, ya que un atacante podría ejecutar peticiones asíncronas de guardado en el navegador de un médico logueado.
*   **Ausencia de Transaccionalidad en Caja**: La derivación y cobro del paciente no tiene `@Transactional` estricto en la capa de servicios, lo que puede resultar en pagos registrados con fallos de derivación (registros huérfanos).

---

## 🎨 2. Diagnóstico del Frontend y Diseño Premium

El frontend está acoplado al backend mediante plantillas dinámicas en **Thymeleaf**, maquetado bajo **Bootstrap v5.3.2**, **Bootstrap Icons**, y alertas modales estilizadas mediante **SweetAlert2**.

### 🟢 Aspectos Fuertes del Diseño:
1. **Alineación Estética**: Implementación de un tema premium estilo **Glassmorphism** que le da al sistema una apariencia moderna y de alta gama.
2. **Soporte de Modo Oscuro**: Hoja de estilos global `main.css` adaptada para alternar entre temas claro/oscuro manteniendo consistencia en colores HSL y contrastes legibles.
3. **Panel de Atención Unificado**: Mapeo visual de 3 columnas para la consulta médica en tiempo real (Signos vitales ➔ Historial Clínico ➔ Diagnóstico y Recetario), lo que eleva significativamente la UX del médico.

### 🔴 Deficiencias Críticas del Frontend:
*   **Dependencia de `localStorage` para Impresión**: Las páginas de impresión de recetas, certificados y referencias cargan los datos usando `localStorage.getItem()`. Si el navegador bloquea las ventanas emergentes o si el usuario abre la página en una pestaña nueva limpia, la impresión se rompe por falta de contexto.
*   **Duplicidad de Código CSS**: Hay más de 5 clases duplicadas y redefinidas en la hoja global `main.css`, además de extensos bloques `<style>` inline en plantillas como `consulta_espera.html`, afectando el rendimiento de renderizado.
*   **Falta de Diseño Adaptivo (Responsive)**: El sidebar fijo de navegación no cuenta con una versión colapsable en pantallas móviles (<768px), lo que deforma el layout en tablets o smartphones.

---

## 🎓 3. Verificación de Cumplimiento del Sílabo

El proyecto cubre de forma excelente las metas de aprendizaje exigidas en cada unidad académica del curso integrador:

| Unidad del Sílabo | Entregable / Requisito Solicitado | Estado en SIGECLIN | Ubicación en el Código / Documentación |
| :--- | :--- | :--- | :--- |
| **Unidad 1: Planificación** | Lean Canvas, Project Charter (PMBOK), Acta de Constitución. | **CUMPLIDO** | [PLANIFICACION_PROYECTO_PMBOK.md](file:///D:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/PLANIFICACION_PROYECTO_PMBOK.md) |
| **Unidad 2: Diseño** | Modelado de Procesos BPMN, Diseño de BD (DER) y Prototipos. | **CUMPLIDO** | Diagramas BPMN en `/docs/bpmn`, DER en [DISEÑO_BASE_DATOS_MER.md](file:///D:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/docs/DISEÑO_BASE_DATOS_MER.md). |
| **Unidad 3: Desarrollo** | Java 17+, Frameworks (Spring Boot), Git, Librerías externas (Guava, POI, Commons Lang3). | **CUMPLIDO** | Estructura Maven, `pom.xml`, catálogo en caché con Guava y exportación a Excel con POI. |
| **Unidad 4: Ops & Cierre** | Pruebas Unitarias (TDD), Telemetría (Actuator), Dockerización, Scripts de Backups. | **CUMPLIDO** | Pruebas JUnit5 en `/src/test`, `docker-compose.yml`, `/scripts/backup_db.ps1` y Actuator activo en `/actuator`. |

---

## 🚀 4. Plan de Acción Recomendado (Para Asegurar la Calificación Máxima)

1.  **Corregir `SystemInitializer.java`**: Modificar el método de purga para que **únicamente** se ejecute en el perfil de test o desarrollo, y nunca en entornos de ejecución regular.
2.  **Asegurar CSRF**: Modificar la configuración de Spring Security para requerir tokens CSRF en las cabeceras AJAX `X-CSRF-TOKEN` y eliminar la excepción de la ruta `/api/**`.
3.  **Corregir la Impresión**: Cambiar el paso de parámetros de `localStorage` a variables de sesión de Thymeleaf o parámetros directos de URL (`?idConsulta=...`) para que las páginas de impresión sean 100% estables y recargables.
