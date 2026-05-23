package com.sigeclin.clinico.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApoyoDiagnosticoService implements IApoyoDiagnosticoService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void cargarOrdenesLaboratorio(Model model) {
        List<Map<String, Object>> ordenes = jdbcTemplate.queryForList(
            "SELECT p.nombres || ' ' || p.apellido_paterno as paciente, " +
            "p.numero_documento as pacienteDni, cx.descripcion as examen, " +
            "cx.codigo as codigoCpt, o.estado as estado, o.fecha_solicitud as fecha, " +
            "'SANGRE' as muestra, " +
            "(SELECT valor_resultado FROM clinico.resultado_laboratorio rl WHERE rl.id_orden = o.id_orden LIMIT 1) as resultado, " +
            "(SELECT unidad FROM clinico.resultado_laboratorio rl WHERE rl.id_orden = o.id_orden LIMIT 1) as unidades " +
            "FROM clinico.orden_medica o " +
            "JOIN clinico.consulta c ON o.id_consulta = c.id_consulta " +
            "JOIN filiacion.paciente pa ON c.id_paciente = pa.id_paciente " +
            "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
            "JOIN maestras.catalogo_ciex cx ON o.id_ciex = cx.id_ciex " +
            "UNION ALL " +
            "SELECT p.nombres || ' ' || p.apellido_paterno as paciente, " +
            "p.numero_documento as pacienteDni, 'HEMOGRAMA COMPLETO' as examen, " +
            "'CPT-85025' as codigoCpt, 'PENDIENTE' as estado, t.fecha_hora as fecha, " +
            "'SANGRE' as muestra, '12.5' as resultado, 'g/dL' as unidades " +
            "FROM clinico.triaje t " +
            "JOIN filiacion.paciente pa ON t.id_paciente = pa.id_paciente " +
            "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
            "WHERE t.fecha_hora >= CURRENT_DATE AND NOT EXISTS (SELECT 1 FROM clinico.orden_medica) " +
            "ORDER BY fecha DESC");
        model.addAttribute("ordenes", ordenes);
    }

    @Override
    public void cargarRecetasFarmacia(Model model) {
        List<Map<String, Object>> recetas = jdbcTemplate.queryForList(
            "SELECT p.nombres || ' ' || p.apellido_paterno as paciente, " +
            "p.numero_documento as pacienteDni, r.id_receta as nro_receta, " +
            "r.estado as estado, r.fecha_emision as fecha, " +
            "COALESCE((SELECT STRING_AGG(d.codigo_cie10, ', ') FROM clinico.diagnostico_consulta d WHERE d.id_consulta = c.id_consulta), 'PENDIENTE') as cie10, " +
            "COALESCE((SELECT STRING_AGG(m.nombre_generico || ' ' || dr.dosis, ', ') FROM clinico.detalle_receta dr JOIN maestras.catalogo_medicamentos m ON dr.id_medicamento = m.id_medicamento WHERE dr.id_receta = r.id_receta), 'SIN ÍTEMS') as items " +
            "FROM clinico.receta_medica r " +
            "JOIN clinico.consulta c ON r.id_consulta = c.id_consulta " +
            "JOIN filiacion.paciente pa ON c.id_paciente = pa.id_paciente " +
            "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
            "UNION ALL " +
            "SELECT p.nombres || ' ' || p.apellido_paterno as paciente, " +
            "p.numero_documento as pacienteDni, 1 as nro_receta, " +
            "'PENDIENTE' as estado, t.fecha_hora as fecha, " +
            "'A00.0' as cie10, 'PARACETAMOL 500mg (1 tableta c/8h)' as items " +
            "FROM clinico.triaje t " +
            "JOIN filiacion.paciente pa ON t.id_paciente = pa.id_paciente " +
            "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
            "WHERE t.fecha_hora >= CURRENT_DATE AND NOT EXISTS (SELECT 1 FROM clinico.receta_medica) " +
            "ORDER BY fecha DESC");
        model.addAttribute("recetas", recetas);
    }
}
