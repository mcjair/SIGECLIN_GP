package com.sigeclin.clinico.model;

import com.sigeclin.maestras.model.Cie10;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "diagnostico_consulta", schema = "clinico")
public class DiagnosticoConsulta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idDiagnosticoConsulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_consulta", nullable = false)
    private Consulta consulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_cie10", nullable = false)
    private Cie10 cie10;

    @Column(length = 20)
    private String tipoDiagnostico = "PRESUNTIVO"; // PRESUNTIVO, DEFINITIVO

    private String observaciones;
}
