package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.clinico.repository.ConsultaRepository;
import com.sigeclin.clinico.repository.TriajeRepository;
import com.sigeclin.clinico.service.IConsultaService;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.repository.PersonalRepository;
import com.sigeclin.filiacion.service.IPacienteService;
import com.sigeclin.maestras.service.ICie10Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultaController.class)
@WithMockUser
class ConsultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IConsultaService consultaService;

    @MockBean
    private AlergiaPacienteRepository alergiaRepository;

    @MockBean
    private IPacienteService pacienteService;

    @MockBean
    private TriajeRepository triajeRepository;

    @MockBean
    private ConsultaRepository consultaRepository;

    @MockBean
    private ICie10Service cie10Service;

    @MockBean
    private PersonalRepository personalRepository;

    @MockBean
    private com.sigeclin.filiacion.repository.UsuarioRepository usuarioRepository;

    @Test
    void guardarAtencion_conDatosValidos_retornaSuccess() throws Exception {
        Triaje triaje = new Triaje();
        Paciente paciente = new Paciente();
        paciente.setIdPersona(1);
        triaje.setPaciente(paciente);

        when(triajeRepository.findById(1)).thenReturn(Optional.of(triaje));
        when(consultaService.guardarConsultaCompleta(anyInt(), anyMap())).thenReturn(new Consulta());
        doNothing().when(pacienteService).actualizarEstado(anyInt(), anyString());

        mockMvc.perform(post("/consulta/guardar").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"triajeId\": 1, \"anamnesis\": \"Paciente presenta dolor abdominal\", \"planTratamiento\": \"Reposo y medicación\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void guardarAtencion_sinTriajeId_retornaError() throws Exception {
        mockMvc.perform(post("/consulta/guardar").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"anamnesis\": \"test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void guardarAtencion_triajeNoEncontrado_retornaError() throws Exception {
        when(triajeRepository.findById(99)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Triaje no encontrado")).when(consultaService).guardarConsultaCompleta(anyInt(), anyMap());

        mockMvc.perform(post("/consulta/guardar").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"triajeId\": 99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
