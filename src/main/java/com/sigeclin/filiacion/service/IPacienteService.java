package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Paciente;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de pacientes del sistema SIGECLIN.
 * Define operaciones de registro, búsqueda y actualización de estado
 * dentro del flujo de atención (Admisión → Caja → Triaje → Consulta).
 */
public interface IPacienteService {

    /**
     * Registra un nuevo paciente o actualiza sus datos si ya existe.
     * Genera automáticamente el número de Historia Clínica (AÑO-CORRELATIVO).
     */
    Paciente registrarPaciente(Paciente paciente);

    /** Busca paciente por su ID de persona. */
    Optional<Paciente> buscarPorId(Integer id);

    /** Obtiene pacientes en estado PENDIENTE_PAGO (cola de caja). */
    List<Paciente> obtenerPacientesRecientes();

    /** Obtiene todos los pacientes registrados. */
    List<Paciente> obtenerTodos();

    /** Obtiene pacientes pendientes de triaje (PENDIENTE_TRIAJE). */
    List<Paciente> obtenerPendientesTriaje();

    /** Obtiene pacientes en cola de consulta filtrados por servicio. */
    List<Paciente> obtenerColaConsulta(String servicio);

    /** Actualiza el estado de un paciente en el flujo de atención. */
    void actualizarEstado(Integer idPaciente, String nuevoEstado);

    /** Actualiza estado por número de documento o HC. */
    void actualizarEstadoPorDocumento(String doc, String nuevoEstado);

    /** Busca paciente por número de documento o historia clínica. */
    Optional<Paciente> buscarPorDniOHC(String query);
}
