package com.sigeclin.clinico.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "auditoria_acceso", schema = "clinico")
public class AuditoriaAcceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAuditoria;

    private String usuario;
    private String accion;
    private String detalle;
    private String ipOrigen;
    private LocalDateTime fechaHora;
    private Integer idPacienteRelacionado;
}
