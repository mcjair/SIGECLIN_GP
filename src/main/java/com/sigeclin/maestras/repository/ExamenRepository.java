package com.sigeclin.maestras.repository;

import com.sigeclin.maestras.model.Examen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Integer> {
    List<Examen> findByAreaOrderByNombre(String area);
    List<Examen> findByActivoTrueOrderByAreaAscNombreAsc();
    List<Examen> findByCodigo(String codigo);
    @Query("SELECT e FROM Examen e WHERE e.activo = true ORDER BY e.area, e.nombre")
    List<Examen> findAllActivosOrderByArea();
}
