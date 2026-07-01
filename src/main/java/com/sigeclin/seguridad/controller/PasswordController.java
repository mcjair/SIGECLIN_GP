package com.sigeclin.seguridad.controller;

import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.seguridad.service.CustomUserDetailsService.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PasswordController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/cambiar-password")
    public String showCambiarPasswordPage() {
        return "seguridad/cambiar_password";
    }

    @PostMapping("/cambiar-password")
    @ResponseBody
    public ResponseEntity<?> doCambiarPassword(@RequestParam String newPassword) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Regla de seguridad: obligar a que tenga mínimo 5 caracteres
            if (newPassword == null || newPassword.length() < 5) {
                return ResponseEntity.badRequest().body(Map.of("message", "La contraseña debe tener al menos 5 caracteres."));
            }
            
            // Usamos la query @Modifying para evitar validación de los campos de Persona (como 'sexo' en admin)
            usuarioRepository.actualizarPassword(username, passwordEncoder.encode(newPassword), LocalDateTime.now());
            
            // Actualizar el token en sesión (Spring Security)
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails currentDetails = (CustomUserDetails) auth.getPrincipal();
                CustomUserDetails newDetails = new CustomUserDetails(
                        currentDetails.getUsername(),
                        usuario.getPasswordHash(),
                        currentDetails.isEnabled(),
                        currentDetails.isAccountNonExpired(),
                        currentDetails.isCredentialsNonExpired(),
                        currentDetails.isAccountNonLocked(),
                        currentDetails.getAuthorities(),
                        false // ¡Bandera desactivada!
                );
                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                        newDetails, auth.getCredentials(), newDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
            
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente."));
        } catch (Exception e) {
            log.error("Error al cambiar password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Error interno al procesar el cambio."));
        }
    }
}
