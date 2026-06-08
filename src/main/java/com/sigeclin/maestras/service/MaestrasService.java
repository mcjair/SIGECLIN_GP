package com.sigeclin.maestras.service;

import com.sigeclin.maestras.model.Servicio;
import com.sigeclin.maestras.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaestrasService implements IMaestrasService {

    private final ServicioRepository servicioRepository;

    public List<Servicio> obtenerServiciosActivos() {
        return servicioRepository.findByActivoTrue();
    }
}
