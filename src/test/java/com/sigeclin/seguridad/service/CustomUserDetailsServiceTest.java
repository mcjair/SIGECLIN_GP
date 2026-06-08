package com.sigeclin.seguridad.service;

import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.seguridad.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private Usuario usuarioActivo;
    private Usuario usuarioBloqueado;
    private Rol rolAdmin;

    @BeforeEach
    void setUp() {
        rolAdmin = new Rol();
        rolAdmin.setCodigo("ADMIN");

        usuarioActivo = new Usuario();
        usuarioActivo.setUsername("admin");
        usuarioActivo.setPasswordHash("hash123");
        usuarioActivo.setCuentaBloqueada(false);
        usuarioActivo.setIntentosFallidos(0);
        usuarioActivo.setRoles(Set.of(rolAdmin));

        usuarioBloqueado = new Usuario();
        usuarioBloqueado.setUsername("bloqueado");
        usuarioBloqueado.setPasswordHash("hash456");
        usuarioBloqueado.setCuentaBloqueada(true);
        usuarioBloqueado.setIntentosFallidos(5);
        usuarioBloqueado.setRoles(Set.of(rolAdmin));
    }

    @Test
    void loadUserByUsername_usuarioValido_retornaUserDetails() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioActivo));

        UserDetails userDetails = service.loadUserByUsername("admin");

        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());
        assertEquals("hash123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_usuarioBloqueado_disabledEsTrue() {
        when(usuarioRepository.findByUsername("bloqueado")).thenReturn(Optional.of(usuarioBloqueado));

        UserDetails userDetails = service.loadUserByUsername("bloqueado");

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_usuarioNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("noexiste"));
    }

    @Test
    void registrarIntentoFallido_cuandoSuperaMaximo_bloqueaCuenta() {
        usuarioActivo.setIntentosFallidos(4);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioActivo));

        service.registrarIntentoFallido("admin");

        assertEquals(5, usuarioActivo.getIntentosFallidos());
        assertTrue(usuarioActivo.getCuentaBloqueada());
        verify(usuarioRepository).save(usuarioActivo);
    }

    @Test
    void registrarIntentoFallido_cuandoNoSuperaMaximo_noBloquea() {
        usuarioActivo.setIntentosFallidos(2);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioActivo));

        service.registrarIntentoFallido("admin");

        assertEquals(3, usuarioActivo.getIntentosFallidos());
        assertFalse(usuarioActivo.getCuentaBloqueada());
    }

    @Test
    void resetearIntentosFallidos_restableceContador() {
        usuarioActivo.setIntentosFallidos(3);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuarioActivo));

        service.resetearIntentosFallidos("admin");

        assertEquals(0, usuarioActivo.getIntentosFallidos());
        assertNotNull(usuarioActivo.getFechaUltimoAcceso());
        verify(usuarioRepository).save(usuarioActivo);
    }

    @Test
    void registrarIntentoFallido_usuarioNoExiste_noHaceNada() {
        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        service.registrarIntentoFallido("noexiste");

        verify(usuarioRepository, never()).save(any());
    }
}
