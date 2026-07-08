package com.sigeclin.config;

import com.sigeclin.clinico.service.IAuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final IAuditoriaService auditoriaService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // Determinar si es una acción sensible para auditar
        if (uri.startsWith("/consulta/atender/")) {
            String idTriaje = uri.substring(uri.lastIndexOf("/") + 1);
            auditoriaService.registrarAcceso("ACCESO_CONSULTA", "Acceso a la consulta del Triaje ID: " + idTriaje, null);
        } else if (uri.startsWith("/triaje/registrar/")) {
            String idPaciente = uri.substring(uri.lastIndexOf("/") + 1);
            try {
                auditoriaService.registrarAcceso("ACCESO_TRIAJE", "Acceso a registro de triaje para el Paciente ID: " + idPaciente, Integer.parseInt(idPaciente));
            } catch (Exception e) {
                auditoriaService.registrarAcceso("ACCESO_TRIAJE", "Acceso a registro de triaje", null);
            }
        } else if (uri.startsWith("/caja/pagar") && "POST".equalsIgnoreCase(method)) {
            String idPaciente = request.getParameter("idPaciente");
            String monto = request.getParameter("monto");
            try {
                auditoriaService.registrarAcceso("PAGO_PROCESADO", "Procesamiento de pago por monto de S/. " + monto, Integer.parseInt(idPaciente));
            } catch (Exception e) {
                auditoriaService.registrarAcceso("PAGO_PROCESADO", "Procesamiento de pago", null);
            }
        } else if (uri.startsWith("/api/farmacia/dispensar") && "POST".equalsIgnoreCase(method)) {
            auditoriaService.registrarAcceso("DISPENSACION_FARMACIA", "Dispensación de medicamentos ejecutada", null);
        } else if (uri.startsWith("/api/laboratorio/resultado") && "POST".equalsIgnoreCase(method)) {
            auditoriaService.registrarAcceso("RESULTADO_LABORATORIO", "Ingreso de resultados de laboratorio", null);
        }

        return true;
    }
}
