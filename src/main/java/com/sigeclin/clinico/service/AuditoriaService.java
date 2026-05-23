package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.AuditoriaAcceso;
import com.sigeclin.clinico.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService implements IAuditoriaService {

    private final AuditoriaRepository repository;

    public void registrarAcceso(String accion, String detalle, Integer idPaciente) {
        AuditoriaAcceso audit = new AuditoriaAcceso();
        String username = "SISTEMA";
        String ip = "127.0.0.1";
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                username = auth.getName();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener usuario autenticado: {}", e.getMessage());
        }
        try {
            jakarta.servlet.http.HttpServletRequest request = 
                ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
            ip = request.getRemoteAddr();
        } catch (Exception e) {
            log.debug("No se pudo obtener IP de origen: {}", e.getMessage());
        }
        audit.setUsuario(username);
        audit.setIpOrigen(ip);
        audit.setAccion(accion);
        audit.setDetalle(detalle);
        audit.setIdPacienteRelacionado(idPaciente);
        audit.setFechaHora(LocalDateTime.now());
        repository.save(audit);
    }
}
