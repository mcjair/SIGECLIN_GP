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

    private static final String KEY_MESSAGE = "message";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/cambiar-password")
    public String showCambiarPasswordPage() {
        return "seguridad/cambiar_password";
    }

    @PostMapping("/cambiar-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> doCambiarPassword(@RequestParam String newPassword, @RequestParam String confirmPassword, jakarta.servlet.http.HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // Verificar existencia de usuario
            if (!usuarioRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Usuario no encontrado");
            }
            
            if (newPassword == null || confirmPassword == null || !newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(Map.of(KEY_MESSAGE, "Las contraseñas ingresadas no coinciden. Por favor, verifíquelas."));
            }

            // Regla de seguridad: Alfanumérica, 8 a 12 caracteres, mayúsculas, minúsculas, números y especial
            if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d$%&¡!\\?\\*@#\\-_\\.\\+]{8,12}$")) {
                return ResponseEntity.badRequest().body(Map.of(KEY_MESSAGE, "La contraseña debe tener entre 8 y 12 caracteres, incluir al menos una mayúscula, una minúscula, un número y al menos un carácter especial (permitidos: $ % & ¡ ! ? * @ # - _ . +)."));
            }
            
            // Usamos la query @Modifying para evitar validación de los campos de Persona (como 'sexo' en admin)
            usuarioRepository.actualizarPassword(username, passwordEncoder.encode(newPassword), LocalDateTime.now(java.time.ZoneId.systemDefault()));
            
            // Cerrar la sesión forzosamente
            request.getSession().invalidate();
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Contraseña actualizada exitosamente. Inicie sesión con su nueva contraseña."));
        } catch (Exception e) {
            log.error("Error al cambiar password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(KEY_MESSAGE, "Error interno al procesar el cambio."));
        }
    }
}
