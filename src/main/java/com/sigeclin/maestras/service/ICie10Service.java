package com.sigeclin.maestras.service;

import com.sigeclin.maestras.model.Cie10;
import java.util.List;

/**
 * Servicio para la gestión del catálogo CIE-10 de diagnósticos.
 * Mantiene una caché en memoria con ~389 códigos curados y
 * ofrece búsqueda por código, descripción y servicio asociado.
 */
public interface ICie10Service {
    List<Cie10> search(String q);
    List<Cie10> search(String q, String servicio);
    int getCacheSize();
}
