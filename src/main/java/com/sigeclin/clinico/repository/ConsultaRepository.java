package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Integer> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"medico", "triaje"})
    org.springframework.data.domain.Page<Consulta> findByPacienteIdPersonaOrderByFechaHoraInicioDesc(Integer idPaciente, org.springframework.data.domain.Pageable pageable);

    boolean existsByTriajeIdTriaje(Integer idTriaje);
}
