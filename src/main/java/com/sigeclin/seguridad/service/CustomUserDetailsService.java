package com.sigeclin.seguridad.service;

import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    static final int MAX_INTENTOS_FALLIDOS = 5;

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (Boolean.TRUE.equals(usuario.getCuentaBloqueada())) {
            log.warn("Intento de login en cuenta bloqueada: {}", username);
        }

        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rol.getCodigo()))
                .collect(java.util.stream.Collectors.toList());

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(Boolean.TRUE.equals(usuario.getCuentaBloqueada()))
                .accountLocked(Boolean.TRUE.equals(usuario.getCuentaBloqueada()))
                .build();
    }

    @Transactional
    public void registrarIntentoFallido(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() != null ? usuario.getIntentosFallidos() + 1 : 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= MAX_INTENTOS_FALLIDOS) {
                usuario.setCuentaBloqueada(true);
                log.warn("Cuenta bloqueada por {} intentos fallidos: {}", MAX_INTENTOS_FALLIDOS, username);
            }
            usuarioRepository.save(usuario);
        });
    }

    @Transactional
    public void resetearIntentosFallidos(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setFechaUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }
}
