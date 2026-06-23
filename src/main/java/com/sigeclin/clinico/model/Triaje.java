package com.sigeclin.clinico.model;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Data
@Entity
@Table(name = "triaje", schema = "clinico")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Triaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTriaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    @NotNull(message = "El paciente es obligatorio")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    private LocalDateTime fechaHora = LocalDateTime.now();

    private Integer presionArterialSistolica;
    private Integer presionArterialDiastolica;
    private Integer frecuenciaCardiaca;
    private Integer frecuenciaRespiratoria;
    private BigDecimal temperatura;
    private Integer saturacionOxigeno;
    private BigDecimal pesoKg;
    private BigDecimal tallaCm;

    @Column(insertable = false, updatable = false)
    private BigDecimal imc;

    @Column(insertable = false, updatable = false)
    private String clasificacionNutricional;

    @NotBlank(message = "La clasificación de urgencia es obligatoria")
    private String clasificacionUrgencia;

    @NotBlank(message = "El servicio destino es obligatorio")
    private String servicioDestino;
    private String observaciones;

    private Boolean alertaClinica = false;
    private String detalleAlerta;
}
