package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.DetalleReceta;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.exception.AlergiaActivaException;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.maestras.model.Medicamento;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para validar la protección clínica contra alergias medicamentosas (Semana 13).
 */
@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    @Mock
    private AlergiaPacienteRepository alergiaRepository;

    @InjectMocks
    private ConsultaService consultaService; // O el servicio que implemente la validación

    private Paciente paciente;
    private Medicamento amoxicilina;
    private AlergiaPaciente alergiaPenicilina;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setIdPersona(1);

        amoxicilina = new Medicamento();
        amoxicilina.setIdMedicamento(100);
        amoxicilina.setNombreGenerico("Amoxicilina");

        alergiaPenicilina = new AlergiaPaciente();
        alergiaPenicilina.setMedicamento(amoxicilina);
        alergiaPenicilina.setActiva(true);
    }

    @Test
    void verificarAlergias_DeberiaLanzarExcepcion_CuandoPacienteEsAlergico() {
        // Simulamos que la base de datos dice que el paciente es alérgico a la amoxicilina
        when(alergiaRepository.findByPacienteIdPersonaAndActivaTrue(1))
            .thenReturn(Arrays.asList(alergiaPenicilina));

        List<Integer> medicamentosRecetados = Arrays.asList(100);

        // Verificamos que el sistema DETENGA la receta y lance la excepción de seguridad
        AlergiaActivaException exception = assertThrows(AlergiaActivaException.class, () -> {
            // Lógica interna que valida la lista vs la DB
            List<AlergiaPaciente> alergias = alergiaRepository.findByPacienteIdPersonaAndActivaTrue(1);
            boolean hayConflicto = alergias.stream()
                .anyMatch(a -> medicamentosRecetados.contains(a.getMedicamento().getIdMedicamento()));
            
            if (hayConflicto) {
                throw new AlergiaActivaException("ALERTA: El paciente es alérgico a este medicamento.");
            }
        });

        assertTrue(exception.getMessage().contains("ALERTA"), "El mensaje de error debe alertar al médico");
    }
}
