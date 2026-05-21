package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "persona", schema = "filiacion", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_tipo_documento", "numero_documento"})
})
@Inheritance(strategy = InheritanceType.JOINED)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPersona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_documento", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private String numeroDocumento;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidoPaterno;

    private String apellidoMaterno;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 1)
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
