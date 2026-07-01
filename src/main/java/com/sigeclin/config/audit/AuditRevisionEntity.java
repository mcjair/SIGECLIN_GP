package com.sigeclin.config.audit;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "revinfo", schema = "public")
@RevisionEntity(AuditRevisionListener.class)
@Getter
@Setter
public class AuditRevisionEntity extends DefaultRevisionEntity {
    
    // Almacenará el nombre de usuario del doctor o administrador que hizo el cambio
    private String username;
    
}
