package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.clinico.model.DetalleReceta;
import com.sigeclin.clinico.model.RecetaMedica;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.clinico.repository.DetalleRecetaRepository;
import com.sigeclin.clinico.repository.RecetaRepository;
import com.sigeclin.exception.AlergiaActivaException;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.maestras.model.Medicamento;
import com.sigeclin.maestras.repository.MedicamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecetaServiceTest {

    @Mock
    private AlergiaPacienteRepository alergiaRepository;
    @Mock
    private RecetaRepository recetaRepository;
    @Mock
    private DetalleRecetaRepository detalleRecetaRepository;
    @Mock
    private MedicamentoRepository medicamentoRepository;

    @InjectMocks
    private RecetaService recetaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void emitirReceta_sinAlergias_guardaReceta() {
        RecetaMedica receta = new RecetaMedica();
        Paciente paciente = new Paciente();
        paciente.setIdPersona(1);
        receta.setPaciente(paciente);

        DetalleReceta detalle = new DetalleReceta();
        Medicamento med = new Medicamento();
        med.setIdMedicamento(1);
        detalle.setMedicamento(med);

        when(alergiaRepository.findByPacienteIdPersonaAndActivaTrue(1)).thenReturn(List.of());
        when(recetaRepository.save(any())).thenReturn(receta);

        RecetaMedica result = recetaService.emitirReceta(receta, List.of(detalle));

        assertNotNull(result);
        verify(recetaRepository).save(receta);
        verify(detalleRecetaRepository).save(detalle);
    }

    @Test
    void emitirReceta_conAlergiaActiva_lanzaExcepcion() {
        RecetaMedica receta = new RecetaMedica();
        Paciente paciente = new Paciente();
        paciente.setIdPersona(1);
        receta.setPaciente(paciente);

        DetalleReceta detalle = new DetalleReceta();
        Medicamento med = new Medicamento();
        med.setIdMedicamento(1);
        detalle.setMedicamento(med);

        AlergiaPaciente alergia = new AlergiaPaciente();
        alergia.setMedicamento(med);

        when(alergiaRepository.findByPacienteIdPersonaAndActivaTrue(1))
                .thenReturn(List.of(alergia));

        assertThrows(AlergiaActivaException.class,
                () -> recetaService.emitirReceta(receta, List.of(detalle)));

        verify(recetaRepository, never()).save(any());
    }
}
