# SIGECLIN GP v3.0: Informe de Fortalecimiento de Seguridad e Impresión Segura

Este informe detalla las modificaciones y mejoras implementadas en la infraestructura de seguridad, persistencia de datos y el flujo de impresión de documentos clínicos del sistema **SIGECLIN GP v3.0**.

---

## 1. Protección contra Borrado de Base de Datos (`SystemInitializer.java`)

### **Riesgo Identificado**
El archivo `SystemInitializer.java` contenía lógica para truncar todas las tablas (`purgeDatabase()`) en cada inicio de la aplicación, lo que presentaba un riesgo crítico de pérdida de datos en entornos de prueba y producción.

### **Solución Implementada**
Se restringió la ejecución del método `purgeDatabase()` únicamente cuando la aplicación se ejecuta bajo el perfil de desarrollo (`"dev"`).
- Se inyectó el objeto `Environment` de Spring para comprobar los perfiles activos.
- Se agregó una verificación robusta para asegurar que si no se encuentra activo el perfil `"dev"`, el método retorne inmediatamente sin alterar la base de datos.

```java
public void run(String... args) {
    if (!Arrays.asList(env.getActiveProfiles()).contains("dev")) {
        log.info("Entorno no de desarrollo detectado. Se omite la purga de la base de datos.");
        // Resto de la inicialización segura...
        return;
    }
    // ...
}
```

---

## 2. Refuerzo de Seguridad contra Ataques CSRF

### **Riesgo Identificado**
La protección CSRF estaba desactivada explícitamente para todos los endpoints de la API (`/api/**`), lo que exponía las operaciones de mutación de estado (como guardar consultas, emitir recetas o dispensar medicamentos) a ataques de falsificación de solicitud en sitios cruzados.

### **Solución Implementada**
1. **Activación Global de CSRF**: Se removieron las excepciones de CSRF para `/api/**` en `SecurityConfig.java`, configurando un repositorio de tokens basado en cookies (`CookieCsrfTokenRepository.withHttpOnlyFalse()`).
2. **Inyección de Meta-tags**: Se modificó `layout.html` para inyectar dinámicamente los tokens CSRF en todas las páginas renderizadas por Thymeleaf:
   ```html
   <meta name="_csrf" th:content="${_csrf.token}"/>
   <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
   ```
3. **Inyectores AJAX / Fetch en Frontend**: Se actualizaron de manera uniforme todos los scripts de cliente para capturar los tokens del DOM e incluirlos en las cabeceras de cada petición POST/PUT/DELETE.
   - `consulta_espera.html`
   - `triaje_registro.html`
   - `farmacia_lista.html`
   - `laboratorio_lista.html`
   - `personal_lista.html`
   - `caja_pago.html`

Ejemplo de inyección en peticiones Fetch:
```javascript
const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

const headers = { 'Content-Type': 'application/json' };
if (csrfToken && csrfHeader) {
    headers[csrfHeader] = csrfToken;
}
```

---

## 3. Arquitectura de Impresión Basada en Servidor (`QueryParam`)

### **Riesgo Identificado**
El sistema transfería la información clínica del paciente (recetas, referencias y certificados) hacia las ventanas de impresión utilizando `localStorage`. Esto causaba:
- Pérdida de la hoja de impresión al recargar la pestaña.
- Imposibilidad de re-imprimir registros desde el historial médico.
- Exposición innecesaria de datos sensibles en el almacenamiento del navegador.

### **Solución Implementada**
Se rediseñó el flujo de impresión utilizando parámetros de consulta en el servidor (`?idConsulta=X`) y consumiendo datos en tiempo real mediante una API REST interna segura.

1. **Retorno de ID en Guardado**: El endpoint `/consulta/guardar` ahora retorna el ID único de la consulta creada en la base de datos:
   ```java
   return ResponseEntity.ok(ApiResponse.ok("...", saved.getIdConsulta()));
   ```
2. **Ampliación de API de Detalle**: El endpoint `/consulta/api/detalle/{id}` fue mejorado para serializar y retornar:
   - Identificadores de consulta e histórico.
   - Datos de triaje (presión arterial, temperatura, saturación de oxígeno, frecuencia cardíaca, urgencia).
   - Datos del médico tratante (nombres, especialidad, número de colegiatura/CMP).
   - Diagnósticos CIE-10 asignados.
   - Medicamentos prescritos y su cantidad de despacho total.
3. **Formatos de Impresión Dinámicos**:
   - **`receta_impresion.html`**: Si detecta un `idConsulta` en la URL, descarga la información desde la API en tiempo real y dibuja la receta oficial del MINSA. De lo contrario, cae en el fallback de `localStorage` para previsualizaciones rápidas de recetas no guardadas.
   - **`referencia_impresion.html`**: Carga dinámicamente la hoja de transferencia médica a partir de la consulta.
   - **`certificado_medico.html`**: Renderiza el certificado oficial del Colegio Médico utilizando los datos persistidos en el servidor.
4. **Acciones en el Historial**: El modal de detalle de consultas pasadas ahora muestra de manera dinámica los botones correspondientes:
   - **Imprimir Receta** (se muestra solo si la consulta histórica contiene prescripciones).
   - **Imprimir Hoja de Referencia** (se habilita si la salida del paciente fue marcada como referencia).
   - **Imprimir Certificado Médico** (disponible para certificar cualquier consulta histórica).

---

## 4. Estado de Cumplimiento Académico (Sílaba & PMBOK)

| Criterio Evaluado | Estado | Implementación / Evidencia |
| :--- | :---: | :--- |
| **Seguridad de Datos** | **CUMPLIDO** | CSRF activo para todas las APIs de mutación y persistencia. |
| **Integridad de Persistencia** | **CUMPLIDO** | DB Purge controlado mediante Spring Profiles (`@Profile("dev")`). |
| **Arquitectura Desacoplada** | **CUMPLIDO** | Controladores AJAX y vistas de impresión independientes basadas en REST. |
| **Diseño Visual Premium** | **CUMPLIDO** | Interfaz Glassmorphic, alertas con SweetAlert2 y tipografías Jakarta Sans. |
