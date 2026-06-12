# Prototipos de Interfaz — SIGECLIN

> Los prototipos se entregan como diagramas de estructura alámbrica (wireframes)
> que representan las pantallas principales del sistema.

## Pantallas incluidas

| # | Pantalla | Descripción |
|---|----------|-------------|
| 1 | `login.png` | Página de inicio de sesión |
| 2 | `dashboard.png` | Dashboard con KPIs y cola de espera |
| 3 | `admision.png` | Registro de paciente |
| 4 | `caja.png` | Pantalla de pago |
| 5 | `triaje.png` | Formulario de triaje |
| 6 | `consulta.png` | Atención médica (3 columnas) |

## Diseño visual

### Layout general
```
┌─────────────────────────────────────────────────────────┐
│ Header: Logo + Título + Usuario + Toggle tema            │
├───────┬─────────────────────────────────────────────────┤
│       │                                                 │
│ Menú  │              Contenido principal                 │
│ lateral│                                                 │
│       │                                                 │
│ • Dash│   ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│ • Adm. │   │  KPI 1  │ │  KPI 2  │ │  KPI 3  │          │
│ • Caja │   └─────────┘ └─────────┘ └─────────┘          │
│ • Tri. │                                                 │
│ • Con. │   ┌─────────────────────────────────────┐       │
│ • Per. │   │          Tabla de datos              │       │
│       │   └─────────────────────────────────────┘       │
│       │                                                 │
└───────┴─────────────────────────────────────────────────┘
```

### Pantalla de Consulta (3 columnas)
```
┌──────────────┬──────────────────┬──────────────────────┐
│   Columna 1  │   Columna 2      │    Columna 3          │
│  Estado Clín.│  Registro Clínico│   Diagnóstico         │
├──────────────┼──────────────────┼──────────────────────┤
│ Signos       │ Anamnesis:       │ CIE-10 Search:        │
│ vitales      │ [texto]          │ [input + autocomplete]│
│ • PA: 120/80 │                  │                       │
│ • FC: 72     │ Examen físico:   │ Diagnósticos:         │
│ • SpO2: 98%  │ [texto]          │ • I10 - HTA           │
│ • T°: 36.5   │                  │ • E11.9 - DM2         │
│              │ Plan tratamiento:│                       │
│ Alertas:     │ [texto]          │ Receta:               │
│ ✅ Normal    │                  │ Medicamento | Dosis   │
│              │ Próximo control: │ Paracetamol 500mg | 1 │
│ Alergias:    │ [datepicker]     │ cada 8h x 7 días      │
│ • Penicilina │                  │                       │
│              │ [Guardar]        │ [Agregar medicamento] │
└──────────────┴──────────────────┴──────────────────────┘
```

## Paleta de colores
- **Primario:** #2563eb (azul)
- **Secundario:** #059669 (verde)
- **Fondo claro:** #f1f5f9
- **Fondo oscuro:** #0f172a
- **Texto:** #1e293b / #e2e8f0

## Modo oscuro
Todos los prototipos tienen variante claro/oscuro usando
`data-theme` y variables CSS.

> Los archivos PNG de cada pantalla se generarán
> a partir de capturas del sistema en funcionamiento.
