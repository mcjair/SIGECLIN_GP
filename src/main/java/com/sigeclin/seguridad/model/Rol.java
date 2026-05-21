package com.sigeclin.seguridad.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "rol", schema = "seguridad")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRol;

    @Column(unique = true, nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String descripcion;

    private Boolean activo = true;
}
