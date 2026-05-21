package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.repository.TriajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TriajeService {

    private final TriajeRepository triajeRepository;

    @Transactional
    public Triaje guardarTriaje(Triaje triaje) {
        return triajeRepository.save(triaje);
    }

    public List<Triaje> obtenerHistorialTriaje(Integer idPaciente) {
        return triajeRepository.findByPacienteIdPersonaOrderByFechaHoraDesc(idPaciente);
    }
}
