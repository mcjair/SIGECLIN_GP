package com.sigeclin.maestras.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "examen", schema = "maestras")
public class Examen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idExamen;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String area;

    private String unidad;

    private BigDecimal rangoMinimo;

    private BigDecimal rangoMaximo;

    private String rangoTexto;

    private Integer tiempoProcesoMin = 60;

    private Boolean activo = true;
}
