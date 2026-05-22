package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "personal", schema = "filiacion")
@PrimaryKeyJoinColumn(name = "id_personal")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"firmaDigital", "usuario", "horario"})
public class Personal extends Persona {
    
    @NotNull(message = "El tipo de personal es obligatorio")
    private Integer idTipoPersonal;

    private Integer idEspecialidad;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Pattern(regexp = "^(CMP|CEP|COP|CPP|CNP)-\\d{4,5}$", message = "Formato de colegiatura invalido (ej: CMP-12345)")
    private String numeroColegiatura;
    
    @NotNull(message = "La fecha de ingreso es obligatoria")
    @PastOrPresent(message = "La fecha de ingreso no puede ser futura")
    private LocalDate fechaIngreso;
    
    private LocalDate fechaCese;
    
    private String estadoLaboral = "activo";
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String horario;
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.BINARY)
    private byte[] firmaDigital;
    
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
