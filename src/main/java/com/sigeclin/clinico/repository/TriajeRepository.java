package com.sigeclin.clinico.repository;

import com.sigeclin.clinico.model.Triaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TriajeRepository extends JpaRepository<Triaje, Integer> {
    List<Triaje> findByPacienteIdPersonaOrderByFechaHoraDesc(Integer idPaciente);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"paciente"})
    List<Triaje> findByFechaHoraBetweenOrderByFechaHoraAsc(LocalDateTime start, LocalDateTime end);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"paciente"})
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Triaje t WHERE t.servicioDestino = :servicio AND t.paciente.estado = 'PENDIENTE_CONSULTA' AND t.fechaHora >= :start ORDER BY t.fechaHora ASC")
    List<Triaje> buscarPendientesPorModulo(String servicio, LocalDateTime start);

    java.util.Optional<Triaje> findTopByPacienteNumeroDocumentoOrPacienteNumeroHistoriaClinicaOrderByFechaHoraDesc(String dni, String hc);
}
