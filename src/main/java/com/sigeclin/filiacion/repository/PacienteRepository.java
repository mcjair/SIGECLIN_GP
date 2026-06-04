package com.sigeclin.filiacion.repository;

import com.sigeclin.filiacion.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Integer>, JpaSpecificationExecutor<Paciente> {
    
    Optional<Paciente> findByNumeroHistoriaClinica(String numeroHistoriaClinica);
    
    Optional<Paciente> findByNumeroDocumento(String numeroDocumento);
    
    Optional<Paciente> findByTipoDocumento_IdTipoDocumentoAndNumeroDocumento(Integer idTipoDocumento, String numeroDocumento);
    
    java.util.List<Paciente> findByEstadoOrderByFechaCreacionAsc(String estado);
    
    java.util.List<Paciente> findByEstadoAndServicioSolicitadoOrderByFechaCreacionAsc(String estado, String servicio);
    
    @Query("SELECT p FROM Paciente p JOIN p.tipoDocumento td WHERE td.codigo = :tipoDoc AND p.numeroDocumento = :numDoc")
    Optional<Paciente> findByTipoAndNumeroDocumento(String tipoDoc, String numDoc);
}
