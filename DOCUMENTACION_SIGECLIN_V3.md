# 🏥 SIGECLIN v3.0 - Documentación Técnica y Flujo de Procesos

Este documento detalla la arquitectura, las lógicas de negocio y el estado actual de los módulos desarrollados en el sistema **SIGECLIN (Sistema de Gestión Clínica)** de Grocio Prado.

---

## 1. Arquitectura Base y Stack Tecnológico

*   **Backend:** Java 17 + Spring Boot 3.2.5.
*   **Seguridad:** Spring Security 6 (Interceptor de peticiones, CSRF habilitado, manejo de sesiones).
*   **Persistencia:** Spring Data JPA + Hibernate ORM (Estrategias de Lazy Loading y manejo transaccional).
*   **Base de Datos:** PostgreSQL (Esquemas separados: `filiacion`, `seguridad`, `maestras`, `clinico`).
*   **Frontend:** HTML5, CSS3 Nativo, Thymeleaf (Server-Side Rendering), Bootstrap 5.3, SweetAlert2.
*   **Diseño (UI/UX):** Glassmorphism, CSS variables (Tema "Premium Teal"), tipografía *Plus Jakarta Sans*.

---

## 2. Lógica de Seguridad y Autenticación (Hardening)

Hemos implementado un modelo de seguridad multicapa siguiendo directrices de prevención (OWASP):

1.  **Prevención de Fuerza Bruta (Anti Brute-Force):**
    *   **Proceso:** El sistema cuenta con la clase `AuthenticationEvents` que "escucha" los fallos y aciertos al intentar iniciar sesión.
    *   **Lógica:** Tras 5 intentos fallidos (`CustomUserDetailsService`), el sistema ejecuta un update nativo bloqueando temporal o indefinidamente al usuario (`cuentaBloqueada = true`). Si entra al intento 3, el contador se resetea a cero.

2.  **Manejo Concurrente de Sesiones (Single Session):**
    *   **Proceso:** El personal no puede tener la misma cuenta abierta en dos consultorios o dispositivos distintos al mismo tiempo.
    *   **Lógica:** Si un médico inicia sesión en un ambiente nuevo, su sesión anterior expira forzosamente de inmediato (`expiredUrl("/login?expired")`), previniendo doble manipulación de historias clínicas.

3.  **Cierre Automático por Inactividad (Timeout):**
    *   **Proceso:** Protege terminales desatendidos.
    *   **Lógica:** Configurado (actualmente a 1 minuto para pruebas, estándar 15m) desde `application.properties`. Se integró un cronómetro visual en el Layout maestro (Top Navbar) que avisa y redirige al usuario a la vista de login automáticamente (sin intervención) si no ocurre tráfico de red.

4.  **Flujo Forzado de Cambio de Contraseña (Alta Seguridad):**
    *   **Proceso:** Previene el uso vitalicio de contraseñas por defecto ("admin").
    *   **Lógica:** Al crear un empleado, nace con `requiereCambioPassword = true`. Al loguearse, el Layout intercepta esta bandera y despliega un Modal Ineludible (z-index infinito, clics externos bloqueados) que exige renovar la clave bajo la regla estricta: *8 a 12 caracteres, mezclando mayúsculas, minúsculas y al menos un número*. Inmediatamente tras cambiarse, se anula la sesión y se exige un re-login.

---

## 3. Flujo Funcional de Módulos (Estado de Avance)

### 3.1. Gestión de Personal (Recursos Humanos)
*   **Alta de Personal:** Se registra a médicos, enfermeras y administrativos con sus datos (Nombres, DNI, Colegiatura). Se implementó validación UI (HTML5 `max` en el calendario) para evitar inyecciones de fechas futuras absurdas.
*   **Generación de Cuentas (Automática):** El método `generarUsuario` en `PersonalService` toma por defecto la inicial del primer nombre y el apellido paterno. Genera dinámicamente un Hash BCrypt único con su propio "Salt" para prevenir vulnerabilidades de colisión, incluso si la clave es "admin".
*   **Gestor de Estados:** Se pueden Inactivar trabajadores sin borrarlos (Soft Delete / Toggle Estado).
*   **Control de Accesos:** Permite el emparejamiento directo de roles (ej. Enfermera -> Módulo Triage) desde la vista administrativa.

### 3.2. Módulos de Primera Atención (Front Desk)
*   **Admisión y Triaje:** Acceso al personal (recepcionistas y enfermeros de turno) para crear o buscar historias clínicas y registrar las constantes vitales del paciente previas a la consulta.
*   **Caja:** Pasarela interna para emitir tickets o procesar cobros de consultas y procedimientos médicos.

### 3.3. Módulos de Atención Médica Especializada
El personal derivado del "Seeder Masivo" está segmentado estrictamente por roles jerárquicos (RBAC). El Layout dinámicamente dibuja las opciones del menú basadas en el Rol:
*   **Medicina General / Pediatría / Odontología / Nutrición / Psicología:** Cada rol tiene accesos privativos según las credenciales del usuario logueado.
*   **Dashboard Clínico:** Los médicos pueden consultar atenciones pasadas e imprimir recetas (PDF) y resultados previos, bloqueando el historial para no ser mutado si la consulta ya terminó.

### 3.4. Reportería y Dashboard Gerencial
*   **Vistas Resumidas:** Dashboard gerencial que agrupa KPIs operacionales (Carga de pacientes diaria, ganancias, atenciones por especialidad).
*   **Exportación POI:** Se ha acoplado (y resuelto conflictos de versión en el POM) la librería de Apache POI y bibliotecas complementarias para generar descargas en formatos limpios y formales (.PDF o tablas dinámicas .XLSX).

---

## 4. Diseño y UX Transversal (Premium UI Engine)

El sistema cuenta con un motor CSS integrado en cabeceras de layout que inyecta un entorno visual hospitalario de primera línea:
*   **Glassmorphism & Branding:** Reemplazo de los grises y colores planos por contenedores translúcidos, aplicando `backdrop-filter`, gradientes y sombras difuminadas (`shadow-premium`). El color corporativo maestro es `var(--premium-teal)`.
*   **Notificaciones Unificadas (SweetAlert2):** Se interceptan todas las alertas y modales a nivel global para inyectar diseños curvos, eliminando los clásicos botones azules para homogeneizar la paleta de colores.
*   **Widgets Dinámicos:** Componentes en tiempo real, como el temporizador visual de sesión y el reloj del sistema empotrados en la navegación superior.

## 5. Script de Inicialización (Massive Seeder)
*   Se desarrolló `MassiveSeeder.java`, una rutina que se ejecuta de forma controlada al iniciar el servidor para inyectar automáticamente al equipo fundador (17 profesionales: Médicos, Farmacéuticos, Administradores, etc.), poblar los datos maestros y evitar pérdida de información en despliegues desde cero.

---
*Documento generado en base a los últimos commits y desarrollos (Julio 2026).*
