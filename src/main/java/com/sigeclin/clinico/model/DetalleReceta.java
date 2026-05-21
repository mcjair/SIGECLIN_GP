package com.sigeclin.clinico.model;

import com.sigeclin.maestras.model.Medicamento;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "detalle_receta", schema = "clinico")
public class DetalleReceta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDetalleReceta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_receta", nullable = false)
    private RecetaMedica receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medicamento", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private String dosis;

    @Column(nullable = false)
    private String frecuencia;

    @Column(nullable = false)
    private String duracion;

    @Column(nullable = false)
    private Integer cantidadTotal;

    private String indicaciones;
}
