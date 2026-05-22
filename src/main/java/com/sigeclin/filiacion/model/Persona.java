package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "persona", schema = "filiacion", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_tipo_documento", "numero_documento"})
})
@Inheritance(strategy = InheritanceType.JOINED)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "fotografia"})
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPersona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El numero de documento es obligatorio")
    @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "Formato de documento invalido")
    private String numeroDocumento;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPaterno;

    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[MF]$", message = "Sexo debe ser M o F")
    private String sexo;

    private String telefonoPrincipal;
    private String telefonoSecundario;
    private String correoElectronico;
    
    private Integer idUbigeoNacimiento;
    private Integer idUbigeoResidencia;
    
    private String direccion;
    
    @Column(name = "fotografia")
    private byte[] fotografia;

    @Transient
    public String getEdadCompleta() {
        if (fechaNacimiento == null) return "---";
        try {
            java.time.Period period = java.time.Period.between(fechaNacimiento, java.time.LocalDate.now());
            return String.format("%d Años, %d Meses, %d Días", period.getYears(), period.getMonths(), period.getDays());
        } catch (Exception e) {
            return "Error";
        }
    }

    private LocalDateTime fechaRegistro = LocalDateTime.now();
    private LocalDateTime fechaActualizacion;
}
