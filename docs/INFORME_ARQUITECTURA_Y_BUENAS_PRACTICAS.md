# 🏛️ Informe de Arquitectura, MVC, GitHub y Buenas Prácticas - SIGECLIN

Este documento técnico recopila la sustentación de la arquitectura de software, el patrón MVC, el control de versiones con GitHub y las buenas prácticas de programación aplicadas en el desarrollo de **SIGECLIN** para cumplir con los estándares académicos y profesionales de calidad.

---

## 🏛️ 1. Arquitectura Propuesta (Decoplamiento Multicapa)

SIGECLIN implementa una arquitectura multicapa (Layered Architecture) que separa las responsabilidades del sistema en capas independientes y acopladas de manera débil mediante interfaces de servicio.

```
┌──────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN (UI)              │
│      Thymeleaf Templates / HTML5 / CSS3 / JS             │
└────────────┬────────────────────────────────┬────────────┘
             │ (Peticiones HTTP/REST)         │ (Retorno de Vistas/JSON)
             ▼                                ▼
┌──────────────────────────────────────────────────────────┐
│                 CAPA DE CONTROL (CONTROLLERS)            │
│         @Controller / @RestController / DTOs             │
└────────────┬────────────────────────────────┬────────────┘
             │ (Inyección de Interfaces)      │ (Retorno de DTOs/Entidades)
             ▼                                ▼
┌──────────────────────────────────────────────────────────┐
│                  CAPA DE NEGOCIO (SERVICES)              │
│       @Service (Interfaces + Implementaciones)           │
└────────────┬────────────────────────────────┬────────────┘
             │ (Acceso a Repositorios)        │ (Retorno de Mapeos JPA)
             ▼                                ▼
┌──────────────────────────────────────────────────────────┐
│               CAPA DE PERSISTENCIA (REPOSITORIES)        │
│          @Repository / JpaRepository / Hibernate         │
└────────────┬────────────────────────────────┬────────────┘
             │ (Consultas SQL/Transacciones)  │ (Filas de Datos)
             ▼                                ▼
┌──────────────────────────────────────────────────────────┐
│                      BASE DE DATOS                       │
│                       PostgreSQL                         │
└──────────────────────────────────────────────────────────┘
```

*   **Flujo del Sistema:** La vista realiza peticiones HTTP. El controlador recibe la petición, valida la entrada, invoca la interfaz de servicio correspondiente y retorna la respuesta. El servicio gestiona las transacciones y utiliza los repositorios JPA para leer/escribir datos en PostgreSQL.

---

## 📐 2. Evidencia de Implementación del Patrón MVC

### 🔹 M - Modelo (Capa de Datos y Entidades)
Representa la estructura de datos y las reglas del negocio persistidas en base de datos.
*   **Código de Evidencia:** Localizado en el paquete [`com.sigeclin.*.model`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/).
*   **Detalle Clínico:** Las entidades representan los esquemas reales de la base de datos (`filiacion`, `seguridad`, `clinico`, `maestras`) usando JPA.
*   **Ejemplo de Buenas Prácticas (Herencia JOINED):** 
    La clase [`Persona.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/filiacion/model/Persona.java) sirve como base y es heredada por `Paciente`, `Personal` y `Usuario` utilizando `@Inheritance(strategy = InheritanceType.JOINED)`. Esto evita la redundancia y normaliza los datos de filiación.

### 🔹 V - Vista (Capa de Interfaz de Usuario)
Define cómo se renderiza la información al usuario en el navegador.
*   **Código de Evidencia:** Localizado en [`src/main/resources/templates/`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/resources/templates/).
*   **Detalle Clínico:** Escrito en HTML5 con **Thymeleaf**, lo que permite inyectar datos del modelo del servidor de forma segura directamente antes de enviar la página al navegador. 
*   **Ejemplo de Buenas Prácticas (Layouts Compartidos):**
    El archivo [`layout.html`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/resources/templates/layout.html) centraliza el menú de navegación lateral, el pie de página y los estilos globales (Bootstrap 5 y `main.css` con diseño Glassmorphism), previniendo la duplicación de código en vistas hijas.

### 🔹 C - Controlador (Capa de Control e Integración)
Recibe la entrada del usuario, interactúa con la capa de servicio y selecciona la vista a renderizar.
*   **Código de Evidencia:** Localizado en [`com.sigeclin.*.controller`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/).
*   **Detalle Clínico:**
    *   `@Controller`: Controladores como [`PacienteController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/filiacion/controller/PacienteController.java) procesan la navegación tradicional.
    *   `@RestController`: Controladores como [`Cie10RestController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/maestras/controller/Cie10RestController.java) exponen endpoints JSON asíncronos para búsquedas rápidas mediante Fetch API/AJAX desde el frontend de la consulta médica.

---

## 🐙 3. Control de Versiones con GitHub

El control y seguimiento del desarrollo del software se gestiona bajo Git y se encuentra integrado con GitHub.

*   **Repositorio Remoto Oficial:** `https://github.com/mcjair/SIGECLIN_GP.git`
*   **Flujo de Ramas (Git Flow):**
    *   **`develop`**: Rama activa de integración de código y refinamiento de interfaces y parches en caliente (rama actual en escucha).
    *   **`master`**: Rama principal de producción que sigue a `origin/master` para despliegues estables.
*   **Ignorado de Archivos (`.gitignore`):**
    *   El archivo [`.gitignore`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/.gitignore) evita la subida de binarios compilados de Maven (`/target`), configuraciones específicas de entornos de desarrollo (IDE como `.vscode`, `.idea`) y archivos temporales de base de datos o logs (`*.log`), garantizando un repositorio ligero y limpio.

---

## 🌟 4. Buenas Prácticas de Programación Aplicadas

### 🔀 A. Inyección de Dependencias por Constructor (IoC)
En lugar de utilizar inyección de campos (`@Autowired` directo sobre variables), se inyectan las dependencias mediante el constructor de la clase.
*   **Por qué es buena práctica:** Facilita la creación de pruebas unitarias (Mocking) y garantiza que las clases no puedan instanciarse en un estado inconsistente (sin sus dependencias requeridas).
*   **Evidencia:** Constructor de [`PacienteController.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/filiacion/controller/PacienteController.java):
    ```java
    public PacienteController(IPacienteService pacienteService, IMaestrasService maestrasService) {
        this.pacienteService = pacienteService;
        this.maestrasService = maestrasService;
    }
    ```

### 🤝 B. Principio de Inversión de Dependencias (DIP - SOLID)
Los controladores e inicializadores del sistema no dependen de implementaciones concretas, sino de abstracciones (Interfaces).
*   **Por qué es buena práctica:** Permite desacoplar el frontend del motor de base de datos y negocio, haciendo posible cambiar la lógica interna (ej. migrar de PostgreSQL a SQL Server) sin afectar a los controladores.
*   **Evidencia:** Inyección de la interfaz `IPacienteService` en el controlador en lugar de la clase directa `PacienteService`.

### 🛡️ C. Control de Transacciones de Datos (`@Transactional`)
Las operaciones que involucran inserciones o actualizaciones múltiples se marcan con la anotación `@Transactional` de Spring.
*   **Por qué es buena práctica:** Garantiza la atomicidad (ACID). Si falla el guardado de la receta médica, se deshace automáticamente el guardado de la consulta y del diagnóstico para evitar datos huérfanos o inconsistencias clínicas.
*   **Evidencia:** Método `guardarConsultaCompleta` en [`ConsultaService.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/service/ConsultaService.java).

### 🏷️ D. Encapsulamiento con Data Transfer Objects (DTO)
Para enviar información compleja desde el cliente AJAX al servidor, se utilizan clases DTO dedicadas en lugar de pasar directamente las entidades de base de datos o mapas genéricos (`Map<String, Object>`).
*   **Por qué es buena práctica:** Previene ataques de asignación masiva (Mass Assignment) y aísla el modelo físico de la base de datos de los datos de entrada del frontend.
*   **Evidencia:** Uso de [`ConsultaRequest.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/clinico/dto/ConsultaRequest.java) en la consulta médica.

### 📝 E. Registro Profesional de Logs (SLF4J)
Se prohíbe el uso de `System.out.println()` o `e.printStackTrace()` para rastrear errores o flujos del sistema. En su lugar, se inyecta la API de logging **SLF4J** con niveles de traza controlados.
*   **Por qué es buena práctica:** Evita el consumo excesivo de memoria del servidor por salidas de consola no controladas y permite configurar salidas a archivos estructurados (`app.log`, `server.log`) segmentados por gravedad (INFO, WARN, ERROR).
*   **Evidencia:** Auditoría clínica y registro de inicialización mediante `log.info()` y `log.error()` en [`SystemInitializer.java`](file:///d:/UTP/SISTEMAS/SIGECLIN_GP_v3.0/src/main/java/com/sigeclin/config/SystemInitializer.java).
