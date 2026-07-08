package com.sigeclin.clinico.controller;

import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_ERROR = "error";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/verificar-usuario")
    public ResponseEntity<Map<String, Object>> verificarUsuario(@RequestBody Map<String, String> body) {
        String username = body.get(PARAM_USERNAME);
        Map<String, Object> res = new LinkedHashMap<>();

        if (username == null || username.isBlank()) {
            res.put(PARAM_ERROR, "Ingrese su nombre de usuario");
            return ResponseEntity.badRequest().body(res);
        }

        var usuarioOpt = usuarioRepository.findByUsername(username.trim());
        if (usuarioOpt.isEmpty()) {
            res.put(PARAM_ERROR, "Usuario no encontrado, contáctese con soporte");
            return ResponseEntity.badRequest().body(res);
        }

        Usuario usuario = usuarioOpt.get();
        String primerNombre = usuario.getNombres().split(" ")[0];
        String primerApellido = usuario.getApellidoPaterno();

        res.put("nombres", primerNombre);
        res.put("apellido", primerApellido);
        res.put(PARAM_USERNAME, usuario.getUsername());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/restablecer-clave")
    public ResponseEntity<Map<String, Object>> restablecerClave(@RequestBody Map<String, String> body) {
        String username = body.get(PARAM_USERNAME);
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        Map<String, Object> res = new LinkedHashMap<>();

        if (username == null || newPassword == null || confirmPassword == null ||
            username.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            res.put(PARAM_ERROR, "Complete todos los campos requeridos");
            return ResponseEntity.badRequest().body(res);
        }

        if (!newPassword.equals(confirmPassword)) {
            res.put(PARAM_ERROR, "Las contraseñas ingresadas no coinciden");
            return ResponseEntity.badRequest().body(res);
        }

        // Regla de seguridad: Alfanumérica, 8 a 12 caracteres, mayúsculas, minúsculas, números y especial
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d$%&¡!\\?\\*@#\\-_\\.\\+]{8,12}$")) {
            res.put(PARAM_ERROR, "La contraseña debe tener entre 8 y 12 caracteres, incluir al menos una mayúscula, una minúscula, un número y un carácter especial ($ % & ¡ ! ? * @ # - _ . +)");
            return ResponseEntity.badRequest().body(res);
        }

        var usuarioOpt = usuarioRepository.findByUsername(username.trim());
        if (usuarioOpt.isEmpty()) {
            res.put(PARAM_ERROR, "Usuario no encontrado, contáctese con soporte");
            return ResponseEntity.badRequest().body(res);
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setRequiereCambioPassword(false); // Ya se cambió exitosamente
        usuarioRepository.save(usuario);

        log.info("Clave restablecida exitosamente por recuperación para usuario: {}", username);
        res.put("mensaje", "Contraseña restablecida exitosamente. Ya puede iniciar sesión.");
        return ResponseEntity.ok(res);
    }
}
