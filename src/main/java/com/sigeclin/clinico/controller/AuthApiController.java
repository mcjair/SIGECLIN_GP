package com.sigeclin.clinico.controller;

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

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/recuperar-clave")
    public ResponseEntity<Map<String, Object>> recuperarClave(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String documento = body.get("documento");
        Map<String, Object> res = new LinkedHashMap<>();

        if (username == null || documento == null || username.isBlank() || documento.isBlank()) {
            res.put("error", "Complete todos los campos");
            return ResponseEntity.badRequest().body(res);
        }

        var usuarioOpt = usuarioRepository.findByUsername(username.trim());
        if (usuarioOpt.isEmpty()) {
            res.put("error", "Usuario no encontrado");
            return ResponseEntity.badRequest().body(res);
        }

        var usuario = usuarioOpt.get();
        if (!documento.trim().equals(usuario.getNumeroDocumento())) {
            res.put("error", "El número de documento no coincide con el usuario");
            return ResponseEntity.badRequest().body(res);
        }

        String nuevaClave = documento.trim();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaClave));
        usuario.setRequiereCambioPassword(true);
        usuarioRepository.save(usuario);

        log.info("Clave restablecida para usuario: {}", username);
        res.put("mensaje", "Clave restablecida exitosamente");
        res.put("usuario", username);
        res.put("nuevaClave", nuevaClave);
        return ResponseEntity.ok(res);
    }
}
