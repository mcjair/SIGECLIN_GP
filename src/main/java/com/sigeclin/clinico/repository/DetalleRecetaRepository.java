package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.DetalleReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DetalleRecetaRepository extends JpaRepository<DetalleReceta, Integer> {
    List<DetalleReceta> findByEstadoDispensacionOrderByRecetaFechaEmisionDesc(String estadoDispensacion);
}
