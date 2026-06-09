# Manual de Usuario — SIGECLIN

## Índice

1. [Acceso al Sistema](#1-acceso-al-sistema)
2. [Módulo de Admisión](#2-módulo-de-admisión)
3. [Módulo de Caja](#3-módulo-de-caja)
4. [Módulo de Triaje](#4-módulo-de-triaje)
5. [Módulo de Consulta Médica](#5-módulo-de-consulta-médica)
6. [Gestión de Personal](#6-gestión-de-personal)
7. [Dashboard](#7-dashboard)
8. [Solución de Problemas](#8-solución-de-problemas)

---

## 1. Acceso al Sistema

1. Abrir navegador en `http://localhost:3001`
2. Ingresar credenciales:

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| admin | admin | Administrador (todos los módulos) |
| medicina | admin | Médico General |
| enfermeria | admin | Enfermería |
| caja | admin | Caja |

3. Presionar **Iniciar Sesión**

---

## 2. Módulo de Admisión

**Propósito:** Registrar nuevos pacientes o buscar existentes.

### Registrar paciente nuevo
1. Ir a **Admisión** en el menú lateral
2. Completar: tipo documento, número documento, nombres, apellidos, fecha nacimiento, sexo
3. Seleccionar servicio solicitado (ej. Medicina General)
4. Presionar **Guardar**
5. El sistema genera automáticamente el número de Historia Clínica (HC)

### Buscar paciente existente
1. Ingresar DNI o HC en el campo de búsqueda
2. Si existe, los datos se cargan automáticamente
3. Actualizar datos si es necesario y guardar

---

## 3. Módulo de Caja

**Propósito:** Cobrar por servicio de atención.

### Procesar pago
1. Ir a **Caja** en el menú lateral
2. La lista muestra pacientes con estado "Pendiente de Pago"
3. Seleccionar paciente o buscar por HC
4. Confirmar monto y tipo de pago (EFECTIVO, TARJETA, etc.)
5. Presionar **Pagar**
6. El paciente pasa automáticamente a "Pendiente de Triaje"

---

## 4. Módulo de Triaje

**Propósito:** Evaluación inicial de signos vitales.

### Realizar triaje
1. Ir a **Triaje** → **Nuevo Triaje**
2. Buscar paciente por DNI o HC
3. Registrar:
   - Peso (kg) y Talla (cm) — IMC se calcula automáticamente
   - Presión Arterial (sistólica/diastólica)
   - Frecuencia Cardíaca y Respiratoria
   - Temperatura y Saturación de Oxígeno
4. Seleccionar clasificación de urgencia
5. Asignar servicio destino
6. Presionar **Guardar Triaje**
7. Alertas clínicas se generan automáticamente si hay valores anormales

---

## 5. Módulo de Consulta Médica

**Propósito:** Atención médica completa con diagnóstico y receta.

### Atender paciente
1. Ir a **Consulta** → seleccionar módulo (Medicina General, etc.)
2. La cola muestra pacientes pendientes
3. Presionar **Atender** en el paciente correspondiente

### Pantalla de 3 columnas

**Columna 1 — Estado Clínico**
- Signos vitales del triaje
- Alertas clínicas activas
- Alergias del paciente

**Columna 2 — Registro Clínico**
- Anamnesis: motivo de consulta
- Examen físico: hallazgos relevantes
- Plan de tratamiento
- Seleccionar tipo de salida (ALTA, RECITA, REFERENCIA)
- Fecha de próximo control (opcional)

**Columna 3 — Diagnóstico y Receta**
- Buscar diagnóstico CIE-10 por código o descripción (autocompletado)
- Agregar medicamentos: nombre, dosis, frecuencia, duración
- Presionar **Finalizar Atención** para guardar todo

---

## 6. Gestión de Personal

**Propósito:** Administrar personal médico y administrativo.

### Funciones disponibles (solo ADMIN)
- **Listar:** Ver todo el personal registrado
- **Guardar:** Registrar nuevo personal con datos profesionales
- **Editar:** Modificar datos existentes
- **Desactivar:** Cambiar estado a inactivo (no desaparece del historial)

---

## 7. Dashboard

**Propósito:** Monitoreo en tiempo real del centro.

### KPIs mostrados
- Ingresos del día
- Atenciones realizadas
- Pacientes en espera
- Tiempo promedio de espera
- Ocupación por servicio
- Últimas transacciones de caja

El dashboard se actualiza automáticamente cada 15 segundos.

---

## 8. Solución de Problemas

| Problema | Causa posible | Solución |
|----------|--------------|----------|
| No carga la página | Servidor caído | Ejecutar `mvn spring-boot:run` |
| Error de conexión BD | PostgreSQL no iniciado | Iniciar servicio PostgreSQL |
| Login no funciona | Credenciales incorrectas | Usar admin/admin |
| 403 Forbidden | CSRF token faltante | Recargar página e intentar de nuevo |
| CIE-10 no carga | Ruta de CSV incorrecta | Verificar `sigeclin.cie10.dir-path` en properties |
| Error al guardar consulta | Datos incompletos | Completar todos los campos obligatorios |

---

> **Versión:** 1.0 | **Sistema:** SIGECLIN | **Última actualización:** Mayo 2026
