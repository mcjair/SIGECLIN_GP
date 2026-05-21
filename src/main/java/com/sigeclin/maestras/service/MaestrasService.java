package com.sigeclin.maestras.service;

import com.sigeclin.maestras.model.Servicio;
import com.sigeclin.maestras.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaestrasService {

    private final ServicioRepository servicioRepository;

    public List<Servicio> obtenerServiciosActivos() {
        return servicioRepository.findByActivoTrue();
    }
}
