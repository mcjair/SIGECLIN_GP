package com.sigeclin.clinico.service;

/**
 * Servicio de auditoría para registrar accesos a la historia clínica.
 * Cumple con el requisito RF56 de trazabilidad de accesos.
 */
public interface IAuditoriaService {
    void registrarAcceso(String accion, String detalle, Integer idPaciente);
}
