package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.OrdenMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrdenMedicaRepository extends JpaRepository<OrdenMedica, Integer> {
    List<OrdenMedica> findByTipoAndEstadoOrderByFechaSolicitudDesc(String tipo, String estado);
    List<OrdenMedica> findByTipoOrderByFechaSolicitudDesc(String tipo);
    long countByTipoAndEstado(String tipo, String estado);
    @Query("SELECT o FROM OrdenMedica o LEFT JOIN FETCH o.resultados WHERE o.tipo = :tipo ORDER BY o.fechaSolicitud DESC")
    List<OrdenMedica> findAllByTipoWithResultados(String tipo);
    @Query("SELECT o FROM OrdenMedica o LEFT JOIN FETCH o.resultados WHERE o.tipo = :tipo AND o.estado = :estado ORDER BY o.fechaSolicitud DESC")
    List<OrdenMedica> findByTipoAndEstadoWithResultados(String tipo, String estado);
}
