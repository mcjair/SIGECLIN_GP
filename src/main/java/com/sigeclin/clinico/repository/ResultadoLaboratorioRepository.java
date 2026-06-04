package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.ResultadoLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResultadoLaboratorioRepository extends JpaRepository<ResultadoLaboratorio, Integer> {
    List<ResultadoLaboratorio> findByOrdenIdOrden(Integer idOrden);
    long countByOrdenIdOrdenAndEsAnormalTrue(Integer idOrden);
    java.util.List<ResultadoLaboratorio> findByOrdenIdOrdenAndCodigoExamen(Integer idOrden, String codigoExamen);
}
