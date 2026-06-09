package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Triaje;
import java.util.List;

/**
 * Servicio para la gestión del triaje clínico.
 * Evalúa signos vitales, genera alertas automáticas y
 * determina la clasificación de urgencia del paciente.
 */
public interface ITriajeService {
    Triaje guardarTriaje(Triaje triaje);
    void evaluarAlertasClinicas(Triaje triaje);
    List<Triaje> obtenerHistorialTriaje(Integer idPaciente);
}
