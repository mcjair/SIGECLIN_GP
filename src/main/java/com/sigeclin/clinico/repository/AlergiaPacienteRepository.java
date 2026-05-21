package com.sigeclin.clinico.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sigeclin.clinico.model.Triaje; // Placeholder

public interface AlergiaPacienteRepository extends JpaRepository<Triaje, Integer> {
    // List<AlergiaPaciente> findByIdPacienteAndActivaTrue(Integer idPaciente);
}
