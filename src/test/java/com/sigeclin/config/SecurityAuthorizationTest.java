package com.sigeclin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.sigeclin.clinico.service.IHistoriaClinicaService;
import com.sigeclin.clinico.service.IAuditoriaService;
import com.sigeclin.clinico.service.IApoyoDiagnosticoService;
import com.sigeclin.service.IDashboardService;
import com.sigeclin.clinico.controller.*;
import com.sigeclin.controller.MainController;
import com.sigeclin.filiacion.controller.*;
import com.sigeclin.filiacion.service.IPersonalService;
import com.sigeclin.filiacion.service.IPacienteService;
import com.sigeclin.clinico.service.IConsultaService;
import com.sigeclin.clinico.service.ITriajeService;
import com.sigeclin.clinico.repository.*;
import com.sigeclin.filiacion.repository.*;
import com.sigeclin.maestras.service.ICie10Service;
import com.sigeclin.maestras.service.IMaestrasService;
import com.sigeclin.maestras.controller.ServicioController;
import com.sigeclin.seguridad.service.CustomUserDetailsService;

import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
    PersonalController.class,
    GestionPacienteController.class,
    HistoriaClinicaController.class,
    ApoyoDiagnosticoController.class,
    ServicioController.class,
    MainController.class
})
@Import(SecurityConfig.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private IPersonalService personalService;
    @MockBean private IPacienteService pacienteService;
    @MockBean private IConsultaService consultaService;
    @MockBean private ITriajeService triajeService;
    @MockBean private IHistoriaClinicaService historiaClinicaService;
    @MockBean private IAuditoriaService auditoriaService;
    @MockBean private IApoyoDiagnosticoService apoyoDiagnosticoService;
    @MockBean private ICie10Service cie10Service;
    @MockBean private IMaestrasService maestrasService;
    @MockBean private AlergiaPacienteRepository alergiaRepository;
    @MockBean private TriajeRepository triajeRepository;
    @MockBean private ConsultaRepository consultaRepository;
    @MockBean private PersonalRepository personalRepository;
    @MockBean private IDashboardService dashboardService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @MockBean private OrdenMedicaRepository ordenMedicaRepository;
    @MockBean private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Map<String, Object> historiaData = new HashMap<>();
        historiaData.put("paciente", new com.sigeclin.filiacion.model.Paciente());
        historiaData.put("consultas", List.of());
        historiaData.put("alergias", List.of());

        when(historiaClinicaService.obtenerHistoriaClinicaCompleta(1)).thenReturn(Optional.of(historiaData));
        when(personalService.listarTodos()).thenReturn(List.of());
        when(pacienteService.obtenerTodos()).thenReturn(List.of());
        when(pacienteService.obtenerTodosPaginado(
                org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(maestrasService.obtenerServiciosActivos()).thenReturn(List.of());
    }

    @Test
    void accesoSinAutenticar_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void usuarioAutenticado_accedeARutaPublica() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_accedeAPersonal() throws Exception {
        mockMvc.perform(get("/personal/lista"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ENFERMERIA")
    void enfermeria_accedeAPersonal() throws Exception {
        mockMvc.perform(get("/personal/lista"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void usuarioAutenticado_accedeALaboratorio() throws Exception {
        mockMvc.perform(get("/apoyo/laboratorio"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void usuarioAutenticado_accedeAServicios() throws Exception {
        mockMvc.perform(get("/servicios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void usuarioAutenticado_accedeAListaPacientes() throws Exception {
        mockMvc.perform(get("/pacientes/lista"))
                .andExpect(status().isOk());
    }
}
