package com.sigeclin.clinico.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resultado_laboratorio", schema = "clinico")
public class ResultadoLaboratorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Integer idResultado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    @JsonIgnoreProperties("resultados")
    private OrdenMedica orden;

    @Column(name = "codigo_examen")
    private String codigoExamen;

    @Column(name = "valor_resultado")
    private String valorResultado;

    private String unidad;

    @Column(name = "rango_minimo")
    private Double rangoMinimo;

    @Column(name = "rango_maximo")
    private Double rangoMaximo;

    @Column(name = "es_anormal")
    private Boolean esAnormal;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento = LocalDateTime.now();
}
