package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.service.ITriajeService;
import com.sigeclin.clinico.service.IAuditoriaService;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.filiacion.service.IPacienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TriajeController.class)
class TriajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ITriajeService triajeService;

    @MockBean
    private IAuditoriaService auditoriaService;

    @MockBean
    private IPacienteService pacienteService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser
    void nuevo_sinParametros_retornaVistaBusqueda() throws Exception {
        when(pacienteService.obtenerPendientesTriaje()).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/triaje/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("clinico/triaje_busqueda"));
    }

    @Test
    @WithMockUser
    void nuevo_conHcValido_redirigeARegistrar() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setIdPersona(1);
        when(pacienteService.buscarPorDniOHC("HC-001")).thenReturn(Optional.of(paciente));

        mockMvc.perform(get("/triaje/nuevo").param("hc", "HC-001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/triaje/registrar/1"));
    }

    @Test
    @WithMockUser
    void nuevo_conHcInvalido_retornaBusqueda() throws Exception {
        when(pacienteService.buscarPorDniOHC("INVALIDO")).thenReturn(Optional.empty());
        when(pacienteService.obtenerPendientesTriaje()).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/triaje/nuevo").param("hc", "INVALIDO"))
                .andExpect(status().isOk())
                .andExpect(view().name("clinico/triaje_busqueda"));
    }

    @Test
    @WithMockUser
    void buscar_conDocumentoValido_redirigeARegistrar() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setIdPersona(1);
        when(pacienteService.buscarPorDniOHC("12345678")).thenReturn(Optional.of(paciente));

        mockMvc.perform(get("/triaje/buscar").param("query", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/triaje/registrar/1"));
    }

    @Test
    @WithMockUser
    void buscar_conDocumentoInvalido_retornaBusqueda() throws Exception {
        when(pacienteService.buscarPorDniOHC("00000000")).thenReturn(Optional.empty());

        mockMvc.perform(get("/triaje/buscar").param("query", "00000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("clinico/triaje_busqueda"));
    }
}
