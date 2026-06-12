# Análisis de Diseño Frontend — SIGECLIN

> **Enfoque:** Diseño visual, UX, paleta de colores, consistencia y estandarización premium  
> **Versión:** v0.0.3 | **Rama:** develop | **Fecha:** 2026-05-24

---

## 1. Estado Actual del Diseño

El sistema tiene una **base sólida** con un sistema de diseño basado en:
- **Glassmorphism** con `backdrop-filter: blur(20px)`
- **Variables CSS** personalizadas (`--primary`, `--bg-card`, etc.)
- **Modo oscuro** vía `[data-theme="dark"]`
- **Transiciones elásticas** `cubic-bezier(0.34, 1.56, 0.64, 1)`

Sin embargo, hay **fragmentación severa** en la implementación: múltiples componentes hacen lo mismo de formas diferentes, hay 4 fuentes distintas, y los templates de impresión tienen lenguajes visuales completamente divergentes.

---

## 2. Paleta de Colores

### 2.1 Actual

| Variable | Light | Dark | Uso |
|----------|-------|------|-----|
| `--bg-deep` | `#f1f5f9` | `#090f1d` | Fondo general |
| `--bg-card` | `rgba(255,255,255,0.55)` | `rgba(15,23,42,0.5)` | Cards/paneles glass |
| `--primary` | `#4f46e5` (Indigo) | `#818cf8` | Acciones principales |
| `--success` | `#0d9488` (Teal) | `#14b8a6` | Éxito/confirmaciones |
| `--warning` | `#d97706` (Amber) | `#f59e0b` | Advertencias |
| `--danger` | `#e11d48` (Rose) | `#fb7185` | Errores/alertas |
| `--info` | `#0891b2` (Cyan) | `#22d3ee` | Información |
| `--text-main` | `#0f172a` | `#f8fafc` | Texto principal |
| `--text-muted` | `#475569` | `#94a3b8` | Texto secundario |

### 2.2 Problemas de Color

| ID | Problema | Ejemplo |
|----|----------|---------|
| C1 | **Colores hardcodeados** en vez de variables | `#ef4444`, `#4f46e5`, `#6366f1` aparecen 50+ veces en templates |
| C2 | **--info inconsistente**: global `#0891b2`, pero `consulta_espera.html` usa `#0ea5e9` (Sky) en botones | Info tiene 2 tonalidades |
| C3 | **Gradientes duplicados**: `linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #db2777 100%)` vs `linear-gradient(135deg, var(--primary), #6366f1)` | Dos versiones del gradiente primario |
| C4 | **Colores hardcodeados en dark mode**: `[data-theme="dark"]` overrides usan valores fijos en vez de variables | Mantenimiento difícil |
| C5 | **Login usa `@media (prefers-color-scheme: dark)`** en vez de `[data-theme="dark"]` | No respeta toggle manual |

### 2.3 Paleta Propuesta (Unificada)

```
PRIMARIOS:
  --primary:          #4f46e5  (Indigo 600)
  --primary-dark:     #4338ca  (Indigo 700)
  --primary-light:    #818cf8  (Indigo 400)
  --primary-gradient: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #a855f7 100%)

SEMÁNTICOS:
  --success:    #0d9488  (Teal 600)
  --warning:    #d97706  (Amber 600)  
  --danger:     #e11d48  (Rose 600)
  --info:       #0891b2  (Cyan 600)        ← UNIFICAR a este valor

NEUTROS:
  --bg-deep:    #f1f5f9
  --text-main:  #0f172a
  --text-muted: #475569
  --border:     rgba(148, 163, 184, 0.12)

DARK MODE:
  --primary:    #818cf8  (Indigo 400)
  --bg-deep:    #090f1d
  --text-main:  #f8fafc
```

**Decisión clave**: Eliminar el rosa (`#db2777`) del gradiente primario. El rosa no es un color médico/salud. Reemplazar con:

```
--primary-gradient: linear-gradient(135deg, #4f46e5 0%, #6366f1 50%, #818cf8 100%)
                   (Indigo 600 → Indigo 500 → Indigo 400)
```

Esto da un look profesional, corporativo y médico sin colores ajenos al sector salud.

---

## 3. Sistema Tipográfico

### 3.1 Problemas

| ID | Problema | Impacto |
|----|----------|---------|
| T1 | **4 fuentes distintas**: `Plus Jakarta Sans` (sistema), `Outfit` (receta), `Inter` (referencia), `Times New Roman` (certificado) | Inconsistencia total en print |
| T2 | **`fw-950` (font-weight: 950)** usado extensivamente, pero `Plus Jakarta Sans` solo soporta hasta 800 | El navegador lo renderiza como 800 (no hay diferencia real) |
| T3 | **Mezcla de `h1-h6` con clases `fs-* fw-*`** — mismo nivel jerárquico se ve diferente según la página | Headings inconsistentes |
| T4 | `letter-spacing: 3px` en receta vs `letter-spacing: -1px` en dashboard — extremos opuestos | Sin coherencia |

### 3.2 Propuesta

**Fuente única**: `Plus Jakarta Sans` en TODOS los templates (incluyendo impresión).

```css
--font-main: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
--font-weight-light: 300;
--font-weight-regular: 400;
--font-weight-medium: 500;
--font-weight-semibold: 600;
--font-weight-bold: 700;
--font-weight-extrabold: 800;
```

**Jerarquía tipográfica estandarizada:**

| Elemento | Clase | Font Weight | Size | Letter Spacing |
|----------|-------|-------------|------|----------------|
| Page Title | `.page-title` | 800 | 1.5rem | -0.5px |
| Card Title | `.card-title` | 800 | 1.1rem | -0.3px |
| Section Header | `.section-title` | 800 | 0.8rem | 1.5px (UPPERCASE) |
| Label | `.label-premium` | 700 | 0.75rem | 0.8px |
| Body | — | 500 | 0.9rem | normal |
| Small | `.extra-small` | 700 | 0.65rem | 0.5px |
| Micro | (none) | 800 | 0.55rem | 1.2px |

---

## 4. Sistema de Componentes

### 4.1 Botones — Fragmentación Actual

Hay **11 estilos de botón diferentes**:

1. `.btn-primary` — Bootstrap override, gradient
2. `.btn-glow` — Ripple expand (animación compleja)
3. `.btn-premium-action` — Slide-up transition
4. `.btn-premium-action-compact` — Ripple pequeño
5. `.btn-premium` — Pill style
6. `.btn-premium-action-v2` — Brightness filter
7. `.btn-premium-danger-outline` — Outline rojo
8. `.btn-premium-danger-outline-compact` — Outline rojo compacto
9. `.btn-premium-main` — Gradient con disabled state
10. `.btn-white` — Fondo blanco
11. `.btn-outline-light-modern` — Outline sutil con check state

**Propuesta**: Estandarizar a **4 variantes**:

```css
/* 1. PRIMARIO — acción principal */
.btn-premium {
    background: var(--primary-gradient);
    border: none; border-radius: 12px;
    padding: 12px 28px; font-weight: 800;
    color: white; box-shadow: var(--shadow-glow);
    transition: var(--transition);
}
.btn-premium:hover { transform: translateY(-2px); box-shadow: var(--shadow-glow-hover); }

/* 2. SECUNDARIO — acción alternativa */
.btn-premium-outline {
    background: transparent; border: 2px solid var(--primary);
    color: var(--primary); border-radius: 12px;
    padding: 10px 24px; font-weight: 700;
}

/* 3. PELIGRO — acción destructiva */
.btn-premium-danger {
    background: var(--danger); border: none; border-radius: 12px;
    padding: 12px 28px; font-weight: 800; color: white;
}

/* 4. GHOST — acción sutil */
.btn-premium-ghost {
    background: transparent; border: none;
    color: var(--text-muted); padding: 8px 16px; font-weight: 700;
}
```

**Compact variant**: Agregar clase `.btn-sm` que reduzca padding/font-size manteniendo el mismo estilo base.

### 4.2 Cards — Fragmentación Actual

| Nombre | Dónde se usa | Border-radius | Padding | Hover |
|--------|-------------|---------------|---------|-------|
| `.card` | main.css (global override) | `var(--radius-lg)=16px` | 1.8rem | translateY(-6px) |
| `.glass-card-premium` | dashboard | `var(--radius-lg)` | 1.5rem? - 1.8rem | translateY(-4px) |
| `.glass-panel-premium` | múltiples (definido 2 veces!) | `var(--radius-xl)=24px` | varía | none |
| `.card-medical-glass` | triaje, caja (inline!) | 20px? 24px? | varía | translateY(-8px) |
| `.card-ultra-premium` | consulta_cola | 24px | varía | translateY(-8px) |
| `.clinical-card` | main.css | `var(--radius-xl)` | 24px | translateY(-8px) |

**Propuesta**: Unificar a **3 tipos**:

```css
/* CARD BASE — glassmorphism estándar */
.card {
    background: var(--bg-card); backdrop-filter: blur(20px);
    border: 1px solid var(--glass-border); border-radius: 16px;
    padding: 1.5rem; box-shadow: var(--shadow-md);
    transition: var(--transition);
}
.card:hover { transform: translateY(-4px); box-shadow: var(--shadow-premium); }

/* CARD-COMPACT — para grids densos */
.card-compact { padding: 1rem; border-radius: 12px; }

/* CARD-PREMIUM — para features destacados */
.card-premium { border-radius: 24px; padding: 2rem; }
```

### 4.3 Formularios

**Problemas:**
- `.input-premium` (pill style) vs `.form-control-premium` (rounded-3) — 2 estilos de input
- `.input-premium-group` (flexbox con icono) vs `.input-premium-icon` (absolute) — 2 formas de iconos
- `select-premium` usa `border-radius: var(--radius-full)` pero `.form-select-premium` usa `12px`
- Login usa `form-floating` de Bootstrap (fuera del sistema premium)

**Propuesta:**
- **Unificar a un solo input**: `.form-control-premium` con iconos vía `.input-group` (flex)
- **Eliminar** `.input-premium` (pill) — mantener solo el estilo rounded-12px
- **Migrar login** de `form-floating` a `.input-premium-group`

### 4.4 Tablas

**Problemas:**
- Tablas solo tienen estilo base en main.css
- `laboratorio_lista.html` y `farmacia_lista.html` no usan clase `.table` de Bootstrap? (revisar)
- Sin variante "premium" para tablas (sin glassmorphism)

**Propuesta:**
```css
.table-premium { ... }  /* Glass table con header sticky */
```

---

## 5. Espaciado y Layout

### 5.1 Problemas

| ID | Problema |
|----|----------|
| S1 | `main-wrapper` padding: `2rem 3rem` (dashboard), pero otros templates usan `p-4` o contenedor propio |
| S2 | Gap inconsistente: `gap-3` (12px), `gap-4` (24px), `gap-2` (8px) mezclados sin criterio |
| S3 | Sidebar padding: `1rem 0.5rem` → 8px horizontal es muy poco, texto muy pegado al borde |
| S4 | `calc(100vh - 120px)` aparece en múltiples templates — no es mantenible como constante |

### 5.2 Propuesta

```css
:root {
    --space-xs: 4px;
    --space-sm: 8px;
    --space-md: 16px;
    --space-lg: 24px;
    --space-xl: 32px;
    --space-2xl: 48px;
    
    --sidebar-width: 260px;
    --sidebar-collapsed-width: 80px;
    --header-height: 80px;    /* top-nav + padding */
    --content-offset: 120px;  /* header-height + extra */
}
```

---

## 6. Modo Oscuro

### 6.1 Problemas

| ID | Problema | Severidad |
|----|----------|-----------|
| D1 | Login usa `@media (prefers-color-scheme: dark)` en vez de `[data-theme="dark"]` | **Alto** — no respeta el toggle manual |
| D2 | `consulta_form.html` usa clases `table-dark`, `bg-dark` — siempre oscuro | **Alto** |
| D3 | Templates de impresión no soportan dark mode (no aplica, pero rompe vista previa) | Bajo |
| D4 | `!important` excesivo en dark overrides (30+ reglas) | Medio |
| D5 | `@media print` no considera modo oscuro en main.css | Bajo |

### 6.2 Propuesta

- Migrar `login.html` de `@media (prefers-color-scheme)` a `[data-theme="dark"]`
- Reemplazar `consulta_form.html` o migrar al diseño 3-columnas
- Reducir `!important` usando mayor especificidad con `[data-theme="dark"] .class`

---

## 7. Animaciones y Transiciones

### 7.1 Problemas

| ID | Problema |
|----|----------|
| A1 | `.pulse-red-ring` definido 2 veces en main.css (líneas 2154 y 2306) |
| A2 | `animate-pulse` definido en triaje_registro.html inline y en Bootstrap |
| A3 | `hover-scale` definido en main.css (0.2s), pero `card-hover-effect` definido inline en triaje_busqueda.html (0.3s) |
| A4 | `transition` mezcla `0.2s`, `0.25s`, `0.3s`, `0.4s` — sin estándar |

### 7.2 Propuesta

```css
:root {
    --transition-fast: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    --transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    --transition-elastic: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.animate-fade-in { animation: slideUpBlur 0.6s var(--ease-out-expo) forwards; }
.animate-scale { transition: var(--transition-base); }
.animate-scale:hover { transform: scale(1.02); }
.animate-lift { transition: var(--transition-base); }
.animate-lift:hover { transform: translateY(-4px); }
```

---

## 8. Templates de Impresión — Rediseño Unificado

### 8.1 Problemas

Los 4 templates de impresión tienen lenguajes visuales completamente diferentes:

| Template | Fuente | Formato | Branding | Estilo |
|----------|--------|---------|----------|--------|
| `voucher_impresion.html` | Inter | 80mm térmico | Sin logo | Minimalista |
| `receta_impresion.html` | Outfit | A4 | Logo MINSA | Corporativo médico |
| `referencia_impresion.html` | Inter | A4 | Sin logo | Formal |
| `certificado_medico.html` | Times New Roman | A4 | Wikipedia (lento!) | Clásico/antiguo |

### 8.2 Propuesta

- **Fuente única**: `Plus Jakarta Sans` en los 4
- **Logo**: Asset local (`/img/logo-minsa.png` o `/img/logo-sigeclin.png`) — eliminar dependencia de Wikipedia
- **Colores**: Usar `#0f172a` y `#4f46e5` como colores corporativos en todos
- **Header unificado**: Mismo header institucional en receta, referencia y certificado
- **Voucher**: Mantener 80mm pero con misma fuente y colores corporativos

---

## 9. Responsive y Mobile

### 9.1 Problemas

| ID | Problema | Severidad |
|----|----------|-----------|
| R1 | Sidebar fixed de 260px sin off-canvas en <768px | **Crítico** |
| R2 | Dashboard KPI grid solo tiene breakpoint en 1200px y 576px — falta 992px | Medio |
| R3 | Tablas sin `table-responsive` wrapper en algunos casos | Bajo |
| R4 | Search containers con widths fijos (350px, 450px) que se desbordan | Medio |
| R5 | Los modales SweetAlert2 tienen `width: 90%` pero los cards adentro tienen tamaños fijos | Medio |

---

## 10. Checklist de Estandarización

### Prioridad Alta (rompe experiencia)

- [ ] **Unificar a 1 fuente**: reemplazar Outfit, Inter, Times New Roman por `Plus Jakarta Sans`
- [ ] **Migrar login** de `@media (prefers-color-scheme)` a `[data-theme="dark"]`
- [ ] **Eliminar `consulta_form.html`** (legacy roto) o migrar completamente
- [ ] **Off-canvas sidebar** para mobile < 768px
- [ ] **Reemplazar imagen Wikipedia** en certificado por asset local

### Prioridad Media (inconsistencias notables)

- [ ] **Unificar botones** de 11 variantes a 4
- [ ] **Unificar cards** de 7 variantes a 3
- [ ] **Eliminar duplicados CSS**: `.glass-panel-premium`, `.fw-950`, `.pulse-red-ring`, etc.
- [ ] **Mover estilos inline** de `triaje_busqueda.html`, `caja_pago.html` a `main.css`
- [ ] **Reemplazar colores hardcodeados** por variables CSS
- [ ] **Unificar gradiente primario** (eliminar rosa `#db2777`)
- [ ] **Estandaizar `--info`** a `#0891b2` en todo el sistema
- [ ] **Migrar padding/margin** a sistema de espaciado basado en variables

### Prioridad Baja (cosméticos)

- [ ] **Estandarizar animaciones**: 3 variantes máximo
- [ ] **Unificar headers de sección** (todos deben verse igual)
- [ ] **Reemplazar `fw-950`** por `fw-800` (no hay peso 950 en la fuente)
- [ ] **Agregar breakpoint 992px** al dashboard KPI grid
- [ ] **Estandarizar placeholders** (estilo, opacidad, color)

---

## 11. Conclusión

El sistema tiene una **base de diseño premium prometedora** pero sufre de **fragmentación por crecimiento orgánico**. Los templates se construyeron en momentos diferentes con criterios distintos.

**Para lograr un look profesional, tenaz y premium se requiere:**

1. **Unificar** el sistema de diseño (1 fuente, 4 botones, 3 cards, 2 inputs)
2. **Limpiar** CSS duplicado y mover estilos inline a main.css
3. **Auditar** cada template para que use variables CSS en vez de colores hardcodeados
4. **Rediseñar** los 4 templates de impresión bajo el mismo lenguaje visual
5. **Agregar** responsive off-canvas sidebar
6. **Eliminar** el legacy (`consulta_form.html`) y dependencias externas (Wikipedia)

**Esfuerzo estimado**: ~3-4 días de trabajo frontend dedicado para implementar todos los cambios.
