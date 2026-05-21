package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.DiagnosticoConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagnosticoConsultaRepository extends JpaRepository<DiagnosticoConsulta, Integer> {
}
