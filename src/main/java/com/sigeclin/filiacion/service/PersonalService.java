package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalService implements IPersonalService {

    private final PersonalRepository personalRepository;

    public List<Personal> listarTodos() {
        return personalRepository.findAll();
    }

    public Personal buscarPorId(Integer id) {
        Validate.notNull(id, "El ID del personal no puede ser nulo");
        return personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado con ID: " + id));
    }

    @Transactional
    public Personal guardar(Personal personal) {
        Validate.notNull(personal, "Los datos del personal no pueden ser nulos");
        if (StringUtils.isNotBlank(personal.getNumeroColegiatura())) {
            personal.setNumeroColegiatura(StringUtils.upperCase(personal.getNumeroColegiatura()));
        }
        if (personal.getNombres() != null) {
            personal.setNombres(StringUtils.trim(personal.getNombres()));
        }
        if (personal.getApellidoPaterno() != null) {
            personal.setApellidoPaterno(StringUtils.trim(personal.getApellidoPaterno()));
        }
        if (personal.getIdPersona() == null) {
            personal.setFechaCreacion(LocalDateTime.now());
        }
        personal.setFechaActualizacion(LocalDateTime.now());
        return personalRepository.save(personal);
    }

    @Transactional
    public void eliminar(Integer id) {
        Personal p = buscarPorId(id);
        p.setEstadoLaboral("inactivo");
        personalRepository.save(p);
    }

    @Transactional
    public void toggleEstado(Integer id) {
        Personal p = buscarPorId(id);
        p.setEstadoLaboral("activo".equals(p.getEstadoLaboral()) ? "inactivo" : "activo");
        personalRepository.save(p);
    }
}
