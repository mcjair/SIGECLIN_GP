package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.RecetaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecetaRepository extends JpaRepository<RecetaMedica, Integer> {
}
