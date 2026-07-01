package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Personal;
import java.util.List;

/**
 * Servicio para la gestión del personal médico y administrativo.
 * Administra el registro, actualización y estado laboral del
 * personal del centro de salud.
 */
public interface IPersonalService {
    List<Personal> listarTodos();
    Personal buscarPorId(Integer id);
    Personal guardar(Personal personal);
    void eliminar(Integer id);
    void toggleEstado(Integer id);
    String generarUsuario(Integer idPersonal);
}
