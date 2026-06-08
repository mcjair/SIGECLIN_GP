package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "paciente", schema = "filiacion")
@PrimaryKeyJoinColumn(name = "id_paciente")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Paciente extends Persona {
    
    private String numeroHistoriaClinica;

    private String grupoSanguineo;
    private String factorRh;
    
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String contactoEmergenciaParentesco;
    
    private String estadoCivil;
    private String ocupacion;
    private String etnia;
    
    private Integer idTipoSeguro;
    
    private LocalDate fechaFallecimiento;
    
    private String estado = "activo";
    
    private String servicioSolicitado;
    
    private String referenciaDireccion;
    
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
