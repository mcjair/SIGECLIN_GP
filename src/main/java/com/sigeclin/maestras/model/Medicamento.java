package com.sigeclin.maestras.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "catalogo_medicamentos", schema = "maestras")
public class Medicamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMedicamento;

    @Column(nullable = false)
    private String nombreGenerico;

    private String concentracion;

    private String presentacion;

    @Column(nullable = false)
    private Boolean activo = true;
}
