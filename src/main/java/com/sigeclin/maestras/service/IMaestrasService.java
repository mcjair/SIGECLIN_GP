package com.sigeclin.maestras.service;

import com.sigeclin.maestras.model.Servicio;
import java.util.List;

/**
 * Servicio para la gestión de tablas maestras del sistema.
 * Proporciona acceso a catálogos base como servicios médicos.
 */
public interface IMaestrasService {
    List<Servicio> obtenerServiciosActivos();
}
