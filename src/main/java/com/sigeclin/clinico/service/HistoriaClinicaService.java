package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.clinico.repository.ConsultaRepository;
import com.sigeclin.clinico.repository.TriajeRepository;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriaClinicaService implements IHistoriaClinicaService {

    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;
    private final AlergiaPacienteRepository alergiaRepository;
    private final TriajeRepository triajeRepository;
    private final JdbcTemplate jdbcTemplate;

    public Optional<Map<String, Object>> obtenerHistoriaClinicaCompleta(Integer idPaciente) {
        Optional<Paciente> pacienteOpt = pacienteRepository.findById(idPaciente);
        if (pacienteOpt.isEmpty()) return Optional.empty();
        
        Map<String, Object> historia = new HashMap<>();
        Paciente paciente = pacienteOpt.get();
        
        List<Consulta> consultas = consultaRepository.findByPacienteIdPersonaOrderByFechaHoraInicioDesc(idPaciente);
        List<AlergiaPaciente> alergias = alergiaRepository.findByPacienteIdPersonaAndActivaTrue(idPaciente);
        
        List<Triaje> triajes = triajeRepository.findByPacienteIdPersonaOrderByFechaHoraDesc(idPaciente);
        Triaje ultimoTriaje = triajes.isEmpty() ? null : triajes.get(0);
        
        List<Map<String, Object>> antecedentes = jdbcTemplate.queryForList(
            "SELECT tipo, descripcion, fecha_registro FROM clinico.antecedente_paciente WHERE id_paciente = ? ORDER BY fecha_registro DESC",
            idPaciente
        );
        
        List<Map<String, Object>> recetasRecientes = jdbcTemplate.queryForList(
            "SELECT dr.id_detalle, m.nombre_generico as nombre, dr.dosis, dr.frecuencia, dr.duracion_dias as duracion, dr.cantidad_total as cantidad " +
            "FROM clinico.detalle_receta dr " +
            "JOIN clinico.receta_medica rm ON dr.id_receta = rm.id_receta " +
            "JOIN maestras.catalogo_medicamentos m ON dr.id_medicamento = m.id_medicamento " +
            "WHERE rm.id_paciente = ? ORDER BY rm.fecha_emision DESC LIMIT 5",
            idPaciente
        );
        
        historia.put("paciente", paciente);
        historia.put("consultas", consultas);
        historia.put("alergias", alergias);
        historia.put("ultimoTriaje", ultimoTriaje);
        historia.put("antecedentes", antecedentes);
        historia.put("recetasRecientes", recetasRecientes);
        
        return Optional.of(historia);
    }
}
