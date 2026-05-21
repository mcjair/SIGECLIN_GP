package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.AuditoriaAcceso;
import com.sigeclin.clinico.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public void registrarAcceso(String accion, String detalle, Integer idPaciente) {
        AuditoriaAcceso audit = new AuditoriaAcceso();
        audit.setUsuario("SISTEMA"); // Default user for initial phase
        audit.setIpOrigen("127.0.0.1");
        audit.setAccion(accion);
        audit.setDetalle(detalle);
        audit.setIdPacienteRelacionado(idPaciente);
        audit.setFechaHora(LocalDateTime.now());
        repository.save(audit);
    }
}
