package com.sigeclin.maestras.repository;

import com.sigeclin.maestras.model.Cie10;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Cie10Repository extends JpaRepository<Cie10, String> {
    
    @Query("SELECT c FROM Cie10 c WHERE LOWER(c.codigo) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Cie10> search(String query, Pageable pageable);

    java.util.Optional<Cie10> findByCodigo(String codigo);
}
