package com.sigeclin.clinico.model;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Personal;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Data
@Entity
@Table(name = "consulta", schema = "clinico")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Consulta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idConsulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    private Integer idCita;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_triaje")
    private Triaje triaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personal", nullable = false)
    private Personal medico;

    @Column(nullable = false)
    private Integer idEspecialidad;

    private LocalDateTime fechaHoraInicio = LocalDateTime.now();
    private LocalDateTime fechaHoraFin;

    private String tipoConsulta = "presencial";
    
    @Column(nullable = false)
    private String motivoConsulta;

    private String anamnesis;
    private String examenFisico;
    private String planTratamiento;
    private LocalDate proximoControl;
    
    private String estado = "en_progreso";

    @OneToMany(mappedBy = "consulta")
    private java.util.List<DiagnosticoConsulta> diagnosticos;

    @OneToMany(mappedBy = "consulta")
    private java.util.List<RecetaMedica> recetas;
}
