package com.sigeclin.config;

import com.sigeclin.seguridad.service.CustomUserDetailsService.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PasswordForceInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // Excluimos recursos estáticos y rutas críticas que no deben bloquearse
        if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/img") || uri.startsWith("/webjars") 
            || uri.startsWith("/dev") || uri.startsWith("/api/auth") || uri.startsWith("/error") 
            || uri.equals("/cambiar-password") || uri.equals("/login") || uri.equals("/logout")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (userDetails.isRequiereCambioPassword()) {
                response.sendRedirect("/cambiar-password");
                return false;
            }
        }
        return true;
    }
}
