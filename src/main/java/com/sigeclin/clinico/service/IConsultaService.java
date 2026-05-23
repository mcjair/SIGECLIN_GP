package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.maestras.model.Servicio;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para la gestión de consultas médicas.
 * Cubre el registro de atenciones, historial de pacientes,
 * y la coordinación con triaje, recetas y diagnósticos.
 */
public interface IConsultaService {

    /** Obtiene el historial de consultas de un paciente, ordenado descendente. */
    List<Consulta> obtenerHistorialPaciente(Integer idPaciente);

    /** Obtiene pacientes en espera de consulta del día actual. */
    List<Triaje> obtenerPacientesEnEspera();

    /** Obtiene pacientes en espera filtrados por módulo/servicio. */
    List<Triaje> obtenerPacientesEnEsperaPorModulo(String modulo);

    /** Obtiene un triaje por su ID. */
    Triaje obtenerTriajePorId(Integer id);

    /** Obtiene servicios médicos activos. */
    List<Servicio> obtenerServiciosActivos();

    /** Busca el último triaje de un paciente por documento o HC. */
    Optional<Triaje> buscarUltimoTriajePorDocumento(String doc);

    /**
     * Guarda la consulta completa incluyendo diagnósticos CIE-10,
     * receta médica (delegado a {@link IRecetaService}) y referencias.
     */
    void guardarConsultaCompleta(Integer triajeId, Map<String, Object> data);
}
