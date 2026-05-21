package com.sigeclin.maestras.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cie10", schema = "maestras")
public class Cie10 {
    @Id
    @Column(name = "codigo", length = 10)
    private String codigo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "subcategoria", length = 50)
    private String subcategoria;

    @Column(name = "capitulo", length = 10)
    private String capitulo;

    @Column(name = "activo")
    private Boolean activo = true;
}
