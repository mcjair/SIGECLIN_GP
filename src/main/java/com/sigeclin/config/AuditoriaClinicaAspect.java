package com.sigeclin.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Aspect
@Component
@Slf4j
public class AuditoriaClinicaAspect {

    // Intercepta métodos de búsqueda en PacienteService o visualización de consultas
    @Before("execution(* com.sigeclin.filiacion.service.PacienteService.buscarPor*(..)) || " +
            "execution(* com.sigeclin.clinico.service.*.obtener*(..)) || " +
            "execution(* com.sigeclin.clinico.controller.*.atender*(..))")
    public void auditarAccesoHistoriaClinica(JoinPoint joinPoint) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String usuario = (authentication != null && authentication.getName() != null) ? authentication.getName() : "SISTEMA/ANONIMO";

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String ip = "IP_DESCONOCIDA";
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
            }

            String accion = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Log estructurado (RNF07)
            log.info("[AUDITORIA_MINSA] - T: {} | USUARIO: {} | IP: {} | ACCION: {}", timestamp, usuario, ip, accion);
        } catch (Exception e) {
            log.error("Error al registrar log de auditoría", e);
        }
    }
}
