package com.sigeclin.clinico.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sigeclin.filiacion.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispensacion", schema = "clinico")
public class Dispensacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDispensacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_detalle_receta", nullable = false)
    private DetalleReceta detalleReceta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private LoteMedicamento lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer cantidadEntregada;

    private LocalDateTime fechaDispensacion;

    private String observaciones;

    @PrePersist
    protected void onCreate() {
        if (fechaDispensacion == null) fechaDispensacion = LocalDateTime.now();
    }
}
