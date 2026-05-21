package com.sigeclin.maestras.repository;

import com.sigeclin.maestras.model.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Integer> {
    @Query("SELECT m FROM Medicamento m WHERE m.activo = true AND LOWER(m.nombreGenerico) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Medicamento> buscarPorNombre(String query);
}
