package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tipo_documento", schema = "filiacion")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TipoDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTipoDocumento;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    private Integer longitudExacta;

    @Column(nullable = false)
    private String regexValidacion;

    private Boolean requiereDigitoVerificacion = false;

    private Boolean activo = true;

    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
