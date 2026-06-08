package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.DetalleReceta;
import com.sigeclin.clinico.model.RecetaMedica;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Personal;
import java.util.List;
import java.util.Map;

/**
 * Servicio para la emisión de recetas médicas.
 * Valúa alergias activas del paciente antes de prescribir
 * y registra los detalles de cada medicamento recetado.
 */
public interface IRecetaService {

    /**
     * Emite una receta validando alergias activas del paciente.
     * Lanza {@link com.sigeclin.exception.AlergiaActivaException} si
     * algún medicamento está contraindicado.
     */
    RecetaMedica emitirReceta(RecetaMedica receta, List<DetalleReceta> detalles);

    /**
     * Construye y emite una receta a partir de los datos de consulta
     * y la lista de medicamentos en formato {@code Map}. Internamente
     * busca las entidades {@code Medicamento} y delega a
     * {@link #emitirReceta(RecetaMedica, List)}.
     */
    void emitirReceta(Consulta consulta, Paciente paciente, Personal medico,
                      String planTratamiento, List<Map<String, Object>> medicamentos);
}
