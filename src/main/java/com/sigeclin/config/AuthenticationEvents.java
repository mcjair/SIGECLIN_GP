package com.sigeclin.config;

import com.sigeclin.seguridad.service.CustomUserDetailsService;
import com.sigeclin.seguridad.service.LoginRateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEvents {

    private final CustomUserDetailsService userDetailsService;
    private final LoginRateLimiterService rateLimiterService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("[ANTI-BRUTE-FORCE] Login exitoso. Reseteando contador a 0 para el usuario: {}", username);
        userDetailsService.resetearIntentosFallidos(username);

        Object details = event.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails) {
            String ip = ((WebAuthenticationDetails) details).getRemoteAddress();
            rateLimiterService.loginSucceeded(ip);
        }
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof String) {
            String username = (String) principal;
            log.warn("[ANTI-BRUTE-FORCE] Login fallido. Incrementando contador para el usuario: {}", username);
            userDetailsService.registrarIntentoFallido(username);
        }

        Object details = event.getAuthentication().getDetails();
        if (details instanceof WebAuthenticationDetails) {
            String ip = ((WebAuthenticationDetails) details).getRemoteAddress();
            rateLimiterService.loginFailed(ip);
        }
    }
}
