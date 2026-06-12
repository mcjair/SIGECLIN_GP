# Reporte de Análisis Frontend — SIGECLIN

> **Versión:** v0.0.3 | **Rama:** develop | **Fecha:** 2026-05-24  
> **Enfoque:** Exclusivamente frontend (templates HTML + CSS + JS). No se toca backend.

---

## 1. Resumen General

Se analizaron **20 archivos** (15 templates + 1 layout + 1 login + 1 dashboard + 1 CSS + 1 servicios).  
El sistema usa **Thymeleaf + Bootstrap 5.3.2 + Bootstrap Icons + SweetAlert2**.  
El CSS global (`main.css`) tiene **2314 líneas** con un sistema de diseño glassmorphic premium, modo oscuro completo y animaciones personalizadas.

---

## 2. Archivos Analizados

| # | Archivo | Líneas | Propósito |
|---|---------|--------|-----------|
| 1 | `login.html` | — | Pantalla de inicio de sesión |
| 2 | `layout.html` | — | Layout base con sidebar y header |
| 3 | `dashboard.html` | — | Panel de control con KPIs |
| 4 | `admission/registro.html` | — | Registro de admisión |
| 5 | `clinico/triaje_busqueda.html` | — | Monitor de espera de triaje |
| 6 | `clinico/triaje_registro.html` | — | Formulario de triaje |
| 7 | `clinico/consulta_cola.html` | 124 | Cola de pacientes en espera (cards) |
| 8 | `clinico/consulta_espera.html` | 645+ | Atención clínica 3 columnas (moderno) |
| 9 | `clinico/consulta_form.html` | 300 | Atención clínica formulario (legacy) |
| 10 | `clinico/caja_pago.html` | 323 | Gestión de cobros y pagos |
| 11 | `clinico/voucher_impresion.html` | 157 | Comprobante de pago (ticket 80mm) |
| 12 | `clinico/historia_3_columnas.html` | 144 | Historia clínica en 3 columnas |
| 13 | `clinico/receta_impresion.html` | 368 | Receta médica formato A4 |
| 14 | `clinico/referencia_impresion.html` | 195 | Hoja de referencia |
| 15 | `clinico/certificado_medico.html` | 228 | Certificado médico |
| 16 | `clinico/laboratorio_lista.html` | 78 | Órdenes de laboratorio |
| 17 | `clinico/farmacia_lista.html` | 65 | Recetas pendientes de farmacia |
| 18 | `filiacion/pacientes_lista.html` | 360 | Directorio de pacientes |
| 19 | `filiacion/personal_lista.html` | 304 | Gestión de personal |
| 20 | `maestras/servicios.html` | 57 | Catálogo de servicios |
| 21 | `static/css/main.css` | 2314 | Estilos globales |

---

## 3. Hallazgos por Módulo

### 3.1 Login (`login.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| L1 | UI | CSS inline extenso (modo oscuro, animaciones, partículas) dentro del HTML en vez de en `main.css` | Media |
| L2 | UX | No hay feedback visual de "cargando" al enviar credenciales | Baja |
| L3 | Acc | No hay atributos `aria-*` en los inputs del formulario | Baja |
| L4 | UX | Placeholder "usuario" y "contraseña" no tienen traducción consistente | Baja |

### 3.2 Layout (`layout.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| LY1 | UI | Sidebar usa `position: fixed` con `z-index: 1000` — puede solaparse con modales de Bootstrap | Media |
| LY2 | UX | En modo colapsado (80px), los tooltips no existen — el usuario no sabe qué hace cada icono | Media |
| LY3 | UI | El toggle collapse está posicionado con `right: -12px` — se sale visualmente del sidebar | Baja |
| LY4 | Resp | No hay off-canvas/mobile sidebar — en <768px el sidebar fijo de 260px rompe el layout | Alta |

### 3.3 Dashboard (`dashboard.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| D1 | UI | Histograma SVG con datos hardcodeados en lugar de dinámicos | Media |
| D2 | UX | Botón "Nuevo Paciente" redirige a `/admission/registro` — pérdida de contexto del dashboard | Baja |
| D3 | UI | Tamaños de KPI inconsistentes: algunos usan `fs-3`, otros `fs-4` | Baja |
| D4 | Acc | Las tarjetas clínicas no tienen `role="button"` o `tabindex` — no accesibles por teclado | Media |

### 3.4 Admisión (`admission/registro.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| A1 | UI | Selector de servicio con `btn-check` no muestra el nombre del servicio seleccionado en ninguna parte | Baja |
| A2 | UX | Búsqueda de paciente existente no tiene feedback de "no results" — solo no muestra nada | Media |
| A3 | UI | `max-height: 300px; overflow-y: auto` en la lista de pacientes sin indicador visual de scroll | Baja |

### 3.5 Triaje (`triaje_busqueda.html`, `triaje_registro.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| T1 | Bug | *(fix aplicado)* `th:field="*{observaciones}"` fuera de ámbito `th:object` — ERR_INCOMPLETE_CHUNKED_ENCODING | **Crítico** |
| T2 | Bug | *(fix aplicado)* `servicioDestino` hidden sin `form="triajeForm"` — "servicio destino obligatorio" | **Alto** |
| T3 | UI | Cards de triaje en busqueda repiten estilos `.card-medical-glass` y `.badge-hc` que ya están en `main.css` | Baja |

### 3.6 Consulta — Cola de Espera (`consulta_cola.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| CQ1 | UI | Toda la card se comporta como clickeable (`th:onclick="'window.location.href=...'"`) pero también hay botón "ATENDER" — doble navegación | Media |
| CQ2 | UX | Filtro JS solo busca en nombre y HC, no en servicio destino ni alerta | Baja |
| CQ3 | JS | `querySelector('.extra-small.fw-800')` puede fallar si hay múltiples elementos con esa clase | Media |
| CQ4 | UI | Empty state usa `opacity-50` que es muy tenue sobre fondo claro | Baja |

### 3.7 Consulta — Atención 3 Columnas (`consulta_espera.html`) ★ Principal

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| CE1 | JS | Variables `historialBase`, `pacienteBase`, etc. se inicializan con JSON.parse de strings Thymeleaf. Si el modelo no pasa datos, `historialJson` vacío causa error | Media |
| CE2 | JS | RECITA detection usa `historialBase.find(h => h.proximoControl != null)` — solo detecta la primera coincidencia | Baja |
| CE3 | JS | `addItem()` asigna `frecuencia: dos` (usa dosis como frecuencia) — no hay campo separado de frecuencia | **Dato** |
| CE4 | JS | `addDiagnostico()` sin selección parsea `cie10Search.value.split(' - ')` — frágil si el formato cambia | Media |
| CE5 | UI | Timeline usa `border-primary border-opacity-10` con `border-width: 3px` — la opacidad 10% es casi invisible | Baja |
| CE6 | CSS | Estilos `<style>` inline duplican clases que ya existen en `main.css` (`.glass-panel-premium`, `.input-premium`, etc.) | Media |
| CE7 | Acc | No hay `aria-live` en las listas dinámicas de diagnósticos/medicamentos | Baja |
| CE8 | UX | No hay confirmación antes de "FINALIZAR CONSULTA" | Media |

### 3.8 Consulta — Formulario Legacy (`consulta_form.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| CF1 | **Bug** | HTML mal formado: `</div>` extra en línea 28 sin apertura — rompe estructura DOM | **Alto** |
| CF2 | UI | Usa tema oscuro (`table-dark`, `bg-dark`) inconsistente con el diseño premium claro del nuevo sistema | Alta |
| CF3 | JS | `fetch(/api/cie10/search?q=...)` — ruta relativa sin prefijo `/consulta/api/` — puede fallar según contexto | **Alto** |
| CF4 | UX | Placeholder de medicamentos tiene datos hardcodeados (Paracetamol 500mg) — confunde al usuario | Baja |
| CF5 | UI | Los inputs de receta no tienen `name` — no se envían al backend | **Alto** |
| CF6 | UI | Tab `dx` carece de `th:field` para almacenar diagnósticos en el objeto `consulta` | Medio |

### 3.9 Caja/Pagos (`caja_pago.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| CP1 | JS | `initCajaModal()` con `setTimeout` como fallback si Bootstrap no está cargado — frágil | Media |
| CP2 | JS | `confirmarYEmitir()` abre print window y luego hace submit del form — la ventana puede ser bloqueada por popup blocker | **Alto** |
| CP3 | UI | Hardcoded "S/ 50.00" en receipt preview — no se obtiene del backend | Alta |
| CP4 | UI | CSS vars locales `--p-indigo` duplican las del `:root` global en `main.css` | Baja |
| CP5 | UX | No hay selector de método de pago (solo efectivo, hardcoded) | Media |

### 3.10 Voucher Impresión (`voucher_impresion.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| VI1 | UI | `onload="window.print(); window.close()"` — `window.close()` puede no funcionar en algunos navegadores | Media |
| VI2 | UI | Tamaño fijo `80mm` para ticket — puede variar según impresora/configuración regional | Baja |
| VI3 | UI | IGV hardcoded a 0% — debería ser configurable | Baja |
| VI4 | UI | El total "S/ 50.00" está hardcoded | Alta |

### 3.11 Historia Clínica 3 Columnas (`historia_3_columnas.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| HC1 | UI | `th:text="${#strings.substring(paciente.nombres, 0, 1)}"` — si nombres es null, lanza error | Media |
| HC2 | UX | Timeline solo muestra especialidad "Medicina General" hardcodeada vía `idEspecialidad == 1` | Baja |
| HC3 | UI | El botón "Volver a Cola" tiene `background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%)` inline en lugar de usar clases | Baja |
| HC4 | Acc | Las cards del timeline no tienen `role="button"` — no accesibles | Baja |

### 3.12 Receta Impresión (`receta_impresion.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| RI1 | JS | Usa `localStorage.getItem('current_prescription')` — frágil si se limpia el storage o se abre en otra pestaña | **Alto** |
| RI2 | UI | Logo MINSA `/img/Minsa.png` — si no existe el archivo, se rompe el header visual | Media |
| RI3 | JS | `data.medicamentos.length` sin validación de null — si `medicamentos` no es array, error | Media |
| RI4 | UI | El diseño usa `font-family: 'Outfit', sans-serif` importado de Google Fonts — inconsistente con `Plus Jakarta Sans` del sistema | Media |

### 3.13 Referencia Impresión (`referencia_impresion.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| RF1 | JS | `localStorage.getItem('current_referral')` — mismo problema de fragilidad que receta | **Alto** |
| RF2 | JS | El número de referencia se genera con `Math.random()` — no es un número real del sistema | **Alto** |
| RF3 | UI | `font-family: 'Inter', sans-serif` hardcoded en style tag — inconsistente con el sistema | Media |

### 3.14 Certificado Médico (`certificado_medico.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| CM1 | UI | Imagen de watermark de Wikipedia externa (`upload.wikimedia.org`) — depende de conectividad externa, lenta y puede ser bloqueada por CSP | **Alto** |
| CM2 | UI | `background-blend-mode: overlay` + imagen remota — efecto visual inconsistente entre navegadores | Media |
| CM3 | JS | `localStorage.getItem('current_certificate')` — mismo problema | **Alto** |
| CM4 | JS | Número de serie generado con `Math.random()` — no es un número real | **Alto** |
| CM5 | UI | `font-family: 'Times New Roman', Times, serif` completamente diferente al sistema | Media |
| CM6 | UI | Puerto 3001 hardcoded en el CSP de SecurityConfig — si cambia el puerto, las imágenes externas pueden fallar | Baja |

### 3.15 Laboratorio (`laboratorio_lista.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| LB1 | **Backend** | Usa `Map<String, Object>` (`o.get('paciente')`) en vez de DTOs tipados — inconsistente con la migración | **Alto** |
| LB2 | UI | Tabla simple sin paginación — si hay muchas órdenes, la página se vuelve pesada | Media |
| LB3 | UX | Botón "NUEVA ORDEN" no tiene funcionalidad JS asociada | Baja |
| LB4 | UI | No hay indicador de resultados "anormales" con código de color | Media |

### 3.16 Farmacia (`farmacia_lista.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| FA1 | **Backend** | Usa `Map<String, Object>` (`r.get('medicamento')`) — mismo problema que laboratorio | **Alto** |
| FA2 | UI | Tabla sin paginación | Media |
| FA3 | UX | Botón "DISPENSAR" sin funcionalidad JS | Baja |
| FA4 | UI | No hay columna de stock disponible | Media |

### 3.17 Pacientes (`filiacion/pacientes_lista.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| PA1 | JS | SweetAlert2 usado como modal de expediente — el scroll horizontal del timeline puede ser confuso en móviles | Media |
| PA2 | JS | `console.table(historial)` expone datos de pacientes en consola del navegador en producción | **Medio** |
| PA3 | JS | `mostrarMas()` formatea fecha con split manual — podría fallar si el formato del backend cambia | Media |
| PA4 | UI | Filtro de pacientes por fecha (`filterT`) usa input type date — ok, pero no hay label asociado | Baja |
| PA5 | UX | No hay debounce en `filterPatients` — puede ser lento con muchos registros | Baja |

### 3.18 Personal (`filiacion/personal_lista.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| PE1 | JS | `form.querySelectorAll('input, select').forEach(el => { if (el.type !== 'submit' ...) el.value = '' })` — resetea también el hidden `idPersona` | **Bug** |
| PE2 | JS | Manejo de fechas como arrays `[year, month, day]` del backend — frágil | Media |
| PE3 | UI | Modal de permisos usa checkboxes sin conexión real al backend — `guardarPermisos()` solo muestra un Swal | **Dato** |
| PE4 | UI | `btn-outline-warning` para toggle estado — icono `bi-toggle-on` sin cambio visual | Baja |
| PE5 | JS | `fetch('/personal/api/' + id)` — no hay manejo de error 404 | Media |

### 3.19 Servicios (`maestras/servicios.html`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| SE1 | UI | "Ver Detalles" no tiene onclick ni href — es decorativo | Media |
| SE2 | UI | Indicador "Activo" con un `span.bg-success.rounded-circle` — siempre verde aunque el servicio esté inactivo | **Bug** |
| SE3 | UI | `th:class="${'bi ' + (servicio.icono != null ? servicio.icono : 'bi-shield-plus') + ' fs-1'}"` — concatenación manual frágil | Baja |

### 3.20 CSS Global (`main.css`)

| ID | Tipo | Descripción | Severidad |
|----|------|-------------|-----------|
| CSS1 | **Duplicado** | `.glass-panel-premium` definido en línea 598 y sobrescrito en línea 2169 | Baja |
| CSS2 | **Duplicado** | `.fw-950` en línea 317 y 2176 | Baja |
| CSS3 | **Duplicado** | `.form-label-premium` en línea 477 y 505 | Baja |
| CSS4 | **Duplicado** | `.pulse-red-ring` en línea 2154 y 2306 | Baja |
| CSS5 | **Duplicado** | `.extra-small` en línea 371 y 2178 | Baja |
| CSS6 | CSS | `#pG stop`, `#chartPathMain` usan selectores de ID — frágiles si el SVG cambia | Baja |
| CSS7 | CSS | `::-webkit-scrollbar` estilos solo para WebKit — Firefox/Edge nuevos no los aplican | Baja |
| CSS8 | CSS | Múltiples `!important` en dark mode overrides — dificulta mantenimiento | Media |

---

## 4. Problemas Transversales

| ID | Problema | Archivos Afectados |
|----|----------|--------------------|
| G1 | **localStorage para datos de impresión** — frágil, no funciona si se abre en pestaña nueva sin datos | `receta_impresion.html`, `referencia_impresion.html`, `certificado_medico.html` |
| G2 | **Dos versiones de consulta médica** — `consulta_form.html` (legacy, roto) y `consulta_espera.html` (moderno) coexisten | `clinico/` |
| G3 | **Uso inconsistente de DTOs vs Map** — laboratorio y farmacia usan `Map<String, Object>` | `laboratorio_lista.html`, `farmacia_lista.html` |
| G4 | **Google Fonts inconsistente** — 3 fuentes diferentes: `Plus Jakarta Sans` (sistema), `Outfit` (receta), `Inter` (referencia), `Times New Roman` (certificado) | Múltiples |
| G5 | **SweetAlert2 como modal principal** — usado para modales complejos (historial, expediente) en vez de modales Bootstrap nativos | `pacientes_lista.html`, `consulta_espera.html` |
| G6 | **Números/importes hardcoded** — "S/ 50.00" aparece en múltiples lugares sin venir del backend | `caja_pago.html`, `voucher_impresion.html` |
| G7 | **Sin paginación en tablas** — laboratorio, farmacia, pacientes pueden tener cientos de registros | `laboratorio_lista.html`, `farmacia_lista.html`, `filiacion/pacientes_lista.html` |
| G8 | **CSS inline vs main.css** — múltiples templates duplican estilos que ya existen en `main.css` | `consulta_espera.html`, `caja_pago.html`, otros |

---

## 5. Prioridades de Corrección

### Crítico (rompe funcionalidad)
- CF1: HTML mal formado en `consulta_form.html`
- CF3: Ruta de API incorrecta en `consulta_form.html`

### Alto (impacta experiencia/seguridad)
- LY4: Sin sidebar responsive mobile
- CP2: Popup blocker en pago
- CM1: Imagen externa de Wikipedia
- G1: localStorage frágil en los 3 templates de impresión
- RF2, CM4: Números generados con `Math.random()`
- CF5: Inputs de receta sin `name`

### Medio (inconsistencias notables)
- D1: SVG con datos hardcodeados
- CE1: JSON.parse sin fallback
- CSS1-5: Duplicación de clases CSS
- G4: Múltiples fuentes inconsistentes
- G6: Importes hardcodeados

### Bajo (cosméticos)
- L1: CSS inline en login
- CQ4: Opacidad empty state
- HC1: substring sin null check
- SE2: Indicador activo siempre verde

---

## 6. Recomendaciones

1. **Eliminar** `consulta_form.html` o migrarlo completamente al diseño 3-columnas de `consulta_espera.html`
2. **Reemplazar localStorage** por URL params o sessionStorage en templates de impresión
3. **Unificar fuentes** a `Plus Jakarta Sans` en todos los templates
4. **Centralizar valores** como tarifas en el backend en vez de hardcodear "S/ 50.00"
5. **Migrar laboratorio y farmacia** de `Map<String, Object>` a DTOs tipados
6. **Agregar paginación** a todas las tablas con muchos registros
7. **Refactorizar CSS** eliminando duplicados y moviendo estilos inline a `main.css`
8. **Agregar modo responsive** con off-canvas sidebar para mobile
9. **Reemplazar imágenes externas** (Wikipedia) por assets locales
10. **Agregar confirmación** antes de acciones destructivas (finalizar consulta, dispensar)
