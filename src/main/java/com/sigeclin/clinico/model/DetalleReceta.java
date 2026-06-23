package com.sigeclin.clinico.model;

import com.sigeclin.maestras.model.Medicamento;
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Data
@Entity
@Table(name = "detalle_receta", schema = "clinico")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
public class DetalleReceta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
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

    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias;

    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal;

    @Column(name = "id_via_administracion", nullable = false)
    private Integer idViaAdministracion = 1; // Default to 1 (Oral)

    @Column(name = "indicaciones_adicionales")
    private String indicacionesAdicionales;

    @Column(name = "estado_dispensacion")
    private String estadoDispensacion = "pendiente";
}
