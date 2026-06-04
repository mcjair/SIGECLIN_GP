package com.sigeclin.clinico.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sigeclin.filiacion.model.Personal;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orden_medica", schema = "clinico")
public class OrdenMedica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idOrden;

    @Column(name = "id_consulta", nullable = false)
    private Integer idConsulta;

    @Column(name = "id_ciex", nullable = false)
    private Integer idCiex;

    @Column(name = "id_personal_solicitante", nullable = false)
    private Integer idPersonalSolicitante;

    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaResultado;

    @Column(name = "id_personal_ejecutor")
    private Integer idPersonalEjecutor;

    private String tipo;

    private String estado = "solicitada";

    private String indicaciones;

    private String resultadoTexto;

    private byte[] resultadoArchivo;

    private Boolean urgente = false;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("orden")
    private List<ResultadoLaboratorio> resultados = new ArrayList<>();
}
