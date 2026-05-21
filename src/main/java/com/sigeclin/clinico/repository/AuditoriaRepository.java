package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.AuditoriaAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<AuditoriaAcceso, Long> {
}
