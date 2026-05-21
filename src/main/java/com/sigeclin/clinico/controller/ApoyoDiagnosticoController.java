package com.sigeclin.clinico.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/apoyo")
@RequiredArgsConstructor
public class ApoyoDiagnosticoController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/laboratorio")
    public String laboratorio(Model model) {
        // Listar pacientes con triaje realizado hoy que podrían requerir laboratorio
        List<Map<String, Object>> ordenes = jdbcTemplate.queryForList(
            "SELECT p.nombres || ' ' || p.apellido_paterno as paciente, " +
            "p.numero_documento as pacienteDni, 'HEMOGRAMA COMPLETO' as examen, " +
            "'PENDIENTE' as estado, t.fecha_hora as fecha " +
            "FROM clinico.triaje t " +
            "JOIN filiacion.paciente pa ON t.id_paciente = pa.id_paciente " +
            "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
            "WHERE t.fecha_hora >= CURRENT_DATE");
        model.addAttribute("ordenes", ordenes);
        return "clinico/laboratorio_lista";
    }

    @GetMapping("/farmacia")
    public String farmacia(Model model) {
        // Listar recetas generadas hoy para dispensación
        List<Map<String, Object>> recetas = jdbcTemplate.queryForList(
            "SELECT p.nombres || ' ' || p.apellido_paterno as paciente, " +
            "p.numero_documento as pacienteDni, r.id_receta as nro_receta, " +
            "r.estado as estado, r.fecha_emision as fecha " +
            "FROM clinico.receta_medica r " +
            "JOIN clinico.consulta c ON r.id_consulta = c.id_consulta " +
            "JOIN filiacion.paciente pa ON c.id_paciente = pa.id_paciente " +
            "JOIN filiacion.persona p ON pa.id_paciente = p.id_persona " +
            "ORDER BY r.fecha_emision DESC");
        model.addAttribute("recetas", recetas);
        return "clinico/farmacia_lista";
    }
}
