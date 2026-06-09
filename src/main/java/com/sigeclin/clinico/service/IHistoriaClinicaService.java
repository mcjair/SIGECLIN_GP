package com.sigeclin.clinico.service;

import java.util.Map;
import java.util.Optional;

/**
 * Servicio para la obtención de la historia clínica completa del paciente.
 * Agrega datos de consultas, triajes, alergias, antecedentes y recetas
 * en un solo objeto de respuesta.
 */
public interface IHistoriaClinicaService {
    Optional<Map<String, Object>> obtenerHistoriaClinicaCompleta(Integer idPaciente);
}
