package com.sigeclin.seguridad.service;

import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rol.getCodigo()))
                .collect(java.util.stream.Collectors.toList());

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .disabled(usuario.getCuentaBloqueada())
                .build();
    }
}
