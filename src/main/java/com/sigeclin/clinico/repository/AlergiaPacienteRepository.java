package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.AlergiaPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlergiaPacienteRepository extends JpaRepository<AlergiaPaciente, Integer> {

    List<AlergiaPaciente> findByPacienteIdPersonaAndActivaTrue(Integer idPaciente);
}
