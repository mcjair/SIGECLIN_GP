package com.sigeclin.filiacion.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "usuario", schema = "filiacion")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Usuario extends Persona {
    
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private Boolean cuentaBloqueada = false;
    private Integer intentosFallidos = 0;
    private LocalDateTime fechaUltimoAcceso;
    private LocalDateTime fechaCambioPassword;
    private Boolean sesionActiva = false;
    private Boolean requiereCambioPassword = true;
    
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_rol",
        schema = "seguridad",
        joinColumns = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private java.util.Set<com.sigeclin.seguridad.model.Rol> roles = new java.util.HashSet<>();
}
