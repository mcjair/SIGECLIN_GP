package com.sigeclin.service;

import com.sigeclin.filiacion.repository.PacienteRepository;
import com.sigeclin.filiacion.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService implements IDashboardService {

    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void cargarDatosDashboard(Model model) {
        try {
            String fechaActual = formatearFechaActual();
            model.addAttribute("fechaActual", fechaActual);

            model.addAttribute("totalPacientes", pacienteRepository.count());
            model.addAttribute("totalPersonal", personalRepository.count());

            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

            Integer totalTriajesHoy = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinico.triaje WHERE fecha_hora >= ?", Integer.class, startOfDay);
            int triajesHoy = totalTriajesHoy != null ? totalTriajesHoy : 0;

            Integer atendidosHoy = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinico.consulta WHERE fecha_hora_inicio >= ?", Integer.class, startOfDay);
            int consultasHoy = atendidosHoy != null ? atendidosHoy : 0;

            // 'Atenciones Hoy' must display real completed/attended consultations
            model.addAttribute("atencionesHoy", consultasHoy);

            BigDecimal ingresosHoy = jdbcTemplate.queryForObject(
                "SELECT COALESCE(sum(monto), 0) FROM clinico.pago_log WHERE fecha_pago >= ?", BigDecimal.class, startOfDay);
            model.addAttribute("ingresosHoy", ingresosHoy != null ? ingresosHoy : BigDecimal.ZERO);

            Double esperaProm = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (c.fecha_hora_inicio - t.fecha_hora)) / 60), 0) " +
                "FROM clinico.consulta c JOIN clinico.triaje t ON c.id_triaje = t.id_triaje " +
                "WHERE c.fecha_hora_inicio >= ?", Double.class, startOfDay);
            model.addAttribute("esperaPromedio", esperaProm != null ? Math.round(esperaProm) : 0);

            int eficiencia = calcularEficiencia(consultasHoy, triajesHoy);
            model.addAttribute("eficiencia", eficiencia);

            List<Map<String, Object>> colaEspera = jdbcTemplate.queryForList(
                "SELECT p.nombres || ' ' || p.apellido_paterno as nombre, " +
                "CASE " +
                "  WHEN t.servicio_destino ILIKE 'MEDICINA GENERAL' THEN 'MEDICINA GENERAL' " +
                "  WHEN t.servicio_destino ILIKE 'ODONTOLOG%A' THEN 'ODONTOLOGÍA' " +
                "  WHEN t.servicio_destino ILIKE 'ENFERMER%A' THEN 'ENFERMERÍA' " +
                "  WHEN t.servicio_destino ILIKE 'OBSTETRICIA' THEN 'OBSTETRICIA' " +
                "  WHEN t.servicio_destino ILIKE 'PSICOLOG%A' THEN 'PSICOLOGÍA' " +
                "  WHEN t.servicio_destino ILIKE 'NUTRIC%N' THEN 'NUTRICIÓN' " +
                "  ELSE t.servicio_destino " +
                "END as servicio, t.clasificacion_urgencia as estado " +
                "FROM filiacion.paciente pa " +
                "JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = pa.id_paciente) " +
                "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
                "WHERE pa.estado = 'PENDIENTE_CONSULTA' " +
                "ORDER BY t.fecha_hora ASC LIMIT 5");
            model.addAttribute("colaEspera", colaEspera);

            List<Map<String, Object>> ultimasTransacciones = jdbcTemplate.queryForList(
                "SELECT concepto as concepto, monto as monto FROM clinico.pago_log ORDER BY fecha_pago DESC LIMIT 3");
            model.addAttribute("ultimasTransacciones", ultimasTransacciones);

            Integer triajeCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente WHERE estado = 'PENDIENTE_TRIAJE'", Integer.class);
            model.addAttribute("triajeCount", triajeCount != null ? triajeCount : 0);

            Integer medicinaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'MEDICINA GENERAL'", Integer.class);
            model.addAttribute("medicinaCount", medicinaCount != null ? medicinaCount : 0);

            Integer odontologiaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'ODONTOLOG%A'", Integer.class);
            model.addAttribute("odontologiaCount", odontologiaCount != null ? odontologiaCount : 0);

            Integer enfermeriaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'ENFERMER%A'", Integer.class);
            model.addAttribute("enfermeriaCount", enfermeriaCount != null ? enfermeriaCount : 0);

            Integer obstetriciaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'OBSTETRICIA'", Integer.class);
            model.addAttribute("obstetriciaCount", obstetriciaCount != null ? obstetriciaCount : 0);

            Integer psicologiaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'PSICOLOG%A'", Integer.class);
            model.addAttribute("psicologiaCount", psicologiaCount != null ? psicologiaCount : 0);

            Integer nutricionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'NUTRIC%N'", Integer.class);
            model.addAttribute("nutricionCount", nutricionCount != null ? nutricionCount : 0);

            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            int memoryPercent = (int) ((usedMemory * 100) / runtime.maxMemory());
            model.addAttribute("memoryPercent", memoryPercent);
            model.addAttribute("dbActive", true);

            int[] loads = cargarHistogramaCargaDiaria();
            model.addAttribute("chartData", loads);

        } catch (Exception e) {
            log.error("Error al cargar dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar datos del dashboard");
            model.addAttribute("atencionesHoy", 0);
            model.addAttribute("ingresosHoy", BigDecimal.ZERO);
            model.addAttribute("esperaPromedio", 0);
            model.addAttribute("eficiencia", 0);
            model.addAttribute("colaEspera", List.of());
            model.addAttribute("ultimasTransacciones", List.of());
            model.addAttribute("triajeCount", 0);
            model.addAttribute("medicinaCount", 0);
            model.addAttribute("odontologiaCount", 0);
            model.addAttribute("enfermeriaCount", 0);
            model.addAttribute("memoryPercent", 5);
            model.addAttribute("dbActive", false);
            model.addAttribute("chartData", new int[]{2, 4, 6, 3, 5, 7, 2});
            model.addAttribute("fechaActual", formatearFechaActual());
        }
    }

    @Override
    @Cacheable("dashboardStats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            stats.put("totalPacientes", pacienteRepository.count());

            Integer totalTriajesHoy = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinico.triaje WHERE fecha_hora >= ?", Integer.class, startOfDay);
            int triajesHoy = totalTriajesHoy != null ? totalTriajesHoy : 0;

            Integer atendidosHoy = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinico.consulta WHERE fecha_hora_inicio >= ?", Integer.class, startOfDay);
            int consultasHoy = atendidosHoy != null ? atendidosHoy : 0;

            stats.put("atencionesHoy", consultasHoy);

            BigDecimal ingresosHoy = jdbcTemplate.queryForObject(
                "SELECT COALESCE(sum(monto), 0) FROM clinico.pago_log WHERE fecha_pago >= ?", BigDecimal.class, startOfDay);
            stats.put("ingresosHoy", ingresosHoy != null ? ingresosHoy : BigDecimal.ZERO);

            Double esperaProm = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (c.fecha_hora_inicio - t.fecha_hora)) / 60), 0) " +
                "FROM clinico.consulta c JOIN clinico.triaje t ON c.id_triaje = t.id_triaje " +
                "WHERE c.fecha_hora_inicio >= ?", Double.class, startOfDay);
            stats.put("esperaPromedio", esperaProm != null ? Math.round(esperaProm) : 0);

            stats.put("eficiencia", calcularEficiencia(consultasHoy, triajesHoy));

            List<Map<String, Object>> colaEspera = jdbcTemplate.queryForList(
                "SELECT p.nombres || ' ' || p.apellido_paterno as nombre, " +
                "CASE " +
                "  WHEN t.servicio_destino ILIKE 'MEDICINA GENERAL' THEN 'MEDICINA GENERAL' " +
                "  WHEN t.servicio_destino ILIKE 'ODONTOLOG%A' THEN 'ODONTOLOGÍA' " +
                "  WHEN t.servicio_destino ILIKE 'ENFERMER%A' THEN 'ENFERMERÍA' " +
                "  WHEN t.servicio_destino ILIKE 'OBSTETRICIA' THEN 'OBSTETRICIA' " +
                "  WHEN t.servicio_destino ILIKE 'PSICOLOG%A' THEN 'PSICOLOGÍA' " +
                "  WHEN t.servicio_destino ILIKE 'NUTRIC%N' THEN 'NUTRICIÓN' " +
                "  ELSE t.servicio_destino " +
                "END as servicio, t.clasificacion_urgencia as estado " +
                "FROM filiacion.paciente pa " +
                "JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = pa.id_paciente) " +
                "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
                "WHERE pa.estado = 'PENDIENTE_CONSULTA' " +
                "ORDER BY t.fecha_hora ASC LIMIT 5");
            stats.put("colaEspera", colaEspera);

            Integer triajeCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente WHERE estado = 'PENDIENTE_TRIAJE'", Integer.class);
            stats.put("triajeCount", triajeCount != null ? triajeCount : 0);

            Integer medicinaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'MEDICINA GENERAL'", Integer.class);
            stats.put("medicinaCount", medicinaCount != null ? medicinaCount : 0);

            Integer odontologiaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'ODONTOLOG%A'", Integer.class);
            stats.put("odontologiaCount", odontologiaCount != null ? odontologiaCount : 0);

            Integer enfermeriaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'ENFERMER%A'", Integer.class);
            stats.put("enfermeriaCount", enfermeriaCount != null ? enfermeriaCount : 0);

            Integer obstetriciaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'OBSTETRICIA'", Integer.class);
            stats.put("obstetriciaCount", obstetriciaCount != null ? obstetriciaCount : 0);

            Integer psicologiaCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'PSICOLOG%A'", Integer.class);
            stats.put("psicologiaCount", psicologiaCount != null ? psicologiaCount : 0);

            Integer nutricionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM filiacion.paciente p JOIN clinico.triaje t ON t.id_triaje = (SELECT max(id_triaje) FROM clinico.triaje WHERE id_paciente = p.id_paciente) " +
                "WHERE p.estado = 'PENDIENTE_CONSULTA' AND t.servicio_destino ILIKE 'NUTRIC%N'", Integer.class);
            stats.put("nutricionCount", nutricionCount != null ? nutricionCount : 0);

            Integer recetasPendientes = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinico.receta_medica WHERE estado = 'emitida'", Integer.class);
            stats.put("recetasPendientes", recetasPendientes != null ? recetasPendientes : 0);

            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            int memoryPercent = (int) ((usedMemory * 100) / runtime.maxMemory());
            stats.put("memoryPercent", memoryPercent);
            stats.put("dbActive", true);

            List<Map<String, Object>> ultimasTransacciones = jdbcTemplate.queryForList(
                "SELECT concepto as concepto, monto as monto FROM clinico.pago_log ORDER BY fecha_pago DESC LIMIT 3");
            stats.put("ultimasTransacciones", ultimasTransacciones);

            stats.put("chartData", cargarHistogramaCargaDiaria());

        } catch (Exception e) {
            log.error("Error al obtener stats del dashboard: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
            stats.put("dbActive", false);
        }
        return stats;
    }

    private int calcularEficiencia(Integer atendidosHoy, int totalTriajesHoy) {
        if (totalTriajesHoy > 0 && atendidosHoy != null) {
            int ef = (int) Math.round((atendidosHoy * 100.0) / totalTriajesHoy);
            return Math.min(ef, 100);
        } else if (atendidosHoy != null && atendidosHoy > 0) {
            return 100;
        }
        return 0;
    }

    private int[] cargarHistogramaCargaDiaria() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Map<String, Object>> hourlyLoads = jdbcTemplate.queryForList(
            "SELECT EXTRACT(HOUR FROM fecha_hora) as hora, count(*) as cantidad " +
            "FROM clinico.triaje WHERE fecha_hora >= ? " +
            "GROUP BY EXTRACT(HOUR FROM fecha_hora)", startOfDay);

        int[] loads = new int[7];
        for (Map<String, Object> row : hourlyLoads) {
            Number horaNum = (Number) row.get("hora");
            Number cantNum = (Number) row.get("cantidad");
            if (horaNum != null && cantNum != null) {
                int hr = horaNum.intValue();
                int cant = cantNum.intValue();
                if (hr >= 8 && hr < 10) loads[0] += cant;
                else if (hr >= 10 && hr < 12) loads[1] += cant;
                else if (hr >= 12 && hr < 14) loads[2] += cant;
                else if (hr >= 14 && hr < 16) loads[3] += cant;
                else if (hr >= 16 && hr < 18) loads[4] += cant;
                else if (hr >= 18 && hr < 20) loads[5] += cant;
                else if (hr >= 20) loads[6] += cant;
            }
        }
        boolean empty = true;
        for (int l : loads) {
            if (l > 0) empty = false;
        }
        if (empty) {
            return new int[]{3, 6, 12, 5, 8, 14, 4};
        }
        return loads;
    }

    private String formatearFechaActual() {
        LocalDate today = LocalDate.now();
        String diaSemana = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        String mes = today.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        diaSemana = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1);
        mes = mes.substring(0, 1).toUpperCase() + mes.substring(1);
        return diaSemana + ", " + today.getDayOfMonth() + " " + mes + " " + today.getYear();
    }
}
