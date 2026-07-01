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

        return new CustomUserDetails(
                usuario.getUsername(),
                usuario.getPasswordHash(),
                !Boolean.TRUE.equals(usuario.getCuentaBloqueada()),
                true,
                true,
                !Boolean.TRUE.equals(usuario.getCuentaBloqueada()),
                authorities,
                Boolean.TRUE.equals(usuario.getRequiereCambioPassword())
        );
    }

    public static class CustomUserDetails extends User {
        private final boolean requiereCambioPassword;

        public CustomUserDetails(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities, boolean requiereCambioPassword) {
            super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
            this.requiereCambioPassword = requiereCambioPassword;
        }

        public boolean isRequiereCambioPassword() {
            return requiereCambioPassword;
        }
    }

    @Transactional
    public void registrarIntentoFallido(String username) {
        usuarioRepository.updateFailedAttempt(username, MAX_INTENTOS_FALLIDOS);
        log.warn("Se registró un intento fallido para el usuario: {}", username);
    }

    @Transactional
    public void resetearIntentosFallidos(String username) {
        usuarioRepository.updateLoginSuccess(username, LocalDateTime.now());
        log.debug("Estadísticas de login reiniciadas con éxito para: {}", username);
    }
}
