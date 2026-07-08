package com.sigeclin.clinico.model;

import com.sigeclin.maestras.model.Cie10;
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Data
@Entity
@Table(name = "diagnostico_consulta", schema = "clinico")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
public class DiagnosticoConsulta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_diagnostico")
    private Integer idDiagnosticoConsulta;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_consulta", nullable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_cie10", nullable = false)
    private Cie10 cie10;

    @Column(name = "tipo", length = 20)
    private String tipoDiagnostico = "PRESUNTIVO"; // PRESUNTIVO, DEFINITIVO

    private String observaciones;
}
