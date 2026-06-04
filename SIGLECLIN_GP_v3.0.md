# Reporte de Verificación de Conexiones — SIGECLIN GP

Este reporte detalla el estado actual de las conexiones del sistema, la base de datos, el frontend y la integración con el repositorio remoto en GitHub.

---

## 1. Conexión de Base de Datos (BD)

* **Motor**: PostgreSQL (v16/v18)
* **Estado del Servicio**: **ACTIVO**
  * El puerto `5432` está en escucha (Listening) bajo el PID `6272` (`postgres.exe`).
* **Credenciales de Configuración** (`application.properties`):
  * **URL**: `jdbc:postgresql://127.0.0.1:5432/sigeclin?sslmode=disable`
  * **Usuario**: `admin`
  * **Contraseña**: `admin`
* **Prueba de Conexión**: **EXITOSA**
  * Se ejecutó una consulta de prueba vía terminal directamente contra la base de datos `sigeclin` y retornó respuesta correcta de forma inmediata.
* **Estado del Esquema**: **INICIALIZADO**
  * La base de datos cuenta con **40 tablas** creadas y distribuidas correctamente a lo largo de los esquemas del sistema:
    * **`clinico`** (16 tablas, ej: `consulta`, `receta_medica`, `triaje`)
    * **`filiacion`** (5 tablas, ej: `paciente`, `personal`, `usuario`)
    * **`maestras`** (15 tablas, ej: `cie10`, `servicio`, `especialidad`)
    * **`seguridad`** (4 tablas, ej: `sesion_log`, `usuario_rol`)

---

## 2. Estado del Backend (Servidor Spring Boot)

* **Tecnología**: Java Spring Boot
* **Puerto Configurado**: `3001`
* **Estado de Ejecución**: **INACTIVO**
  * El puerto `3001` no se encuentra en escucha en este momento, lo que indica que el servidor Spring Boot no está corriendo en segundo plano.
* **Historial de Ejecución** (`app.log` / `server.log`):
  * Los registros de auditoría confirman que el backend corrió con éxito previamente en el puerto `3001`, gestionando endpoints correctamente (ej: `/api/dashboard/stats`) y conectándose a la base de datos PostgreSQL sin errores de persistencia.

---

## 3. Conexión Backend-Frontend

* **Arquitectura**: Monolito integrado.
* **Tecnologías**:
  * **Plantillas (Templates)**: Thymeleaf (`.html` en `src/main/resources/templates`).
  * **Estilos & UI**: Bootstrap v5.3.2, Bootstrap Icons y un diseño premium customizado estilo Glassmorphic mediante `main.css`.
  * **Interacción Dinámica**: JavaScript mediante llamadas `fetch()` asíncronas hacia las APIs REST del Backend.
* **Estado de Conexión**: **INACTIVO**
  * Debido a que el backend no está corriendo actualmente, el frontend no puede ser renderizado o accedido a través del navegador web local (puerto `3001` cerrado). Una vez iniciado el backend, la conexión es automática.

---

## 4. Conexión a GitHub

* **Estado de Git**: Inicializado localmente.
* **Rama Actual**: `develop` (la rama local `master` está configurada para seguir a `origin/master`).
* **Repositorio Remoto (GitHub)**: Configurado correctamente.
  * **URL de Fetch**: `https://github.com/mcjair/SIGECLIN_GP.git`
  * **URL de Push**: `https://github.com/mcjair/SIGECLIN_GP.git`
* **Prueba de Conectividad de Red**: **EXITOSA**
  * La comunicación TCP contra `github.com` a través del puerto `443` (HTTPS) se verificó exitosamente, confirmando que la máquina local tiene acceso a internet y puede sincronizar cambios (pull/push) con el repositorio remoto de GitHub.
