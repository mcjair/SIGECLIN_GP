package com.sigeclin.maestras.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "servicio", schema = "maestras")
public class Servicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idServicio;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    private String icono; // CSS class for icon (Bootstrap Icons)
}
