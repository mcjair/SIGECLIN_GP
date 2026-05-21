package com.sigeclin.clinico.model;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.maestras.model.Medicamento;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "alergia_paciente", schema = "clinico")
public class AlergiaPaciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAlergiaPaciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    private Integer idAlergiaTipo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medicamento")
    private Medicamento medicamento;

    private String descripcion;
    private String severidad; // LEVE, MODERADA, SEVERA
    private LocalDate fechaDiagnostico;
    private Boolean activa = true;
}
