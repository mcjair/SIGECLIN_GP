package com.sigeclin.filiacion.repository;

import com.sigeclin.filiacion.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {
    java.util.Optional<TipoDocumento> findByCodigo(String codigo);
}
