package com.sigeclin.config.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity auditEntity = (AuditRevisionEntity) revisionEntity;
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            // Sellar la auditoría con el username exacto que originó el cambio
            auditEntity.setUsername(authentication.getName());
        } else {
            // Si el cambio fue por un proceso en background o seeder del sistema
            auditEntity.setUsername("SYSTEM");
        }
    }
}
