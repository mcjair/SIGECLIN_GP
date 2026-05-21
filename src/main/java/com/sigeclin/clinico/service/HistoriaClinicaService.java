package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.repository.AlergiaRepository;
import com.sigeclin.clinico.repository.ConsultaRepository;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.repository.PacienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HistoriaClinicaService {

    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;
    private final AlergiaRepository alergiaRepository;

    public Optional<Map<String, Object>> obtenerHistoriaClinicaCompleta(Integer idPaciente) {
        Optional<Paciente> pacienteOpt = pacienteRepository.findById(idPaciente);
        if (pacienteOpt.isEmpty()) return Optional.empty();
        
        Map<String, Object> historia = new HashMap<>();
        Paciente paciente = pacienteOpt.get();
        
        List<Consulta> consultas = consultaRepository.findByPacienteIdPersonaOrderByFechaHoraInicioDesc(idPaciente);
        List<AlergiaPaciente> alergias = alergiaRepository.findByPacienteIdPersonaAndActivaTrue(idPaciente);
        
        historia.put("paciente", paciente);
        historia.put("consultas", consultas);
        historia.put("alergias", alergias);
        
        return Optional.of(historia);
    }
}
