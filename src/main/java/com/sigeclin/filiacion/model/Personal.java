package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "personal", schema = "filiacion")
@PrimaryKeyJoinColumn(name = "id_personal")
public class Personal extends Persona {
    
    @Column(nullable = false)
    private Integer idTipoPersonal;

    private Integer idEspecialidad;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String numeroColegiatura;
    
    @Column(nullable = false)
    private LocalDate fechaIngreso;
    
    private LocalDate fechaCese;
    
    private String estadoLaboral = "activo";
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String horario;
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.BINARY)
    private byte[] firmaDigital;
    
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
