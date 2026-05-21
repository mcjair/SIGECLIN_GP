package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.repository.TriajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TriajeServiceTest {

    @Mock
    private TriajeRepository triajeRepository;

    @InjectMocks
    private TriajeService triajeService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testEvaluarAlertasClinicas_NormalVitals() {
        Triaje triaje = new Triaje();
        triaje.setPresionArterialSistolica(120);
        triaje.setPresionArterialDiastolica(80);
        triaje.setFrecuenciaCardiaca(75);
        triaje.setSaturacionOxigeno(98);
        triaje.setTemperatura(new BigDecimal("37.0"));

        triajeService.evaluarAlertasClinicas(triaje);

        assertFalse(triaje.getAlertaClinica());
        assertNull(triaje.getDetalleAlerta());
    }

    @Test
    public void testEvaluarAlertasClinicas_HypertensionDetected() {
        Triaje triaje = new Triaje();
        triaje.setPresionArterialSistolica(145);
        triaje.setPresionArterialDiastolica(95);

        triajeService.evaluarAlertasClinicas(triaje);

        assertTrue(triaje.getAlertaClinica());
        assertTrue(triaje.getDetalleAlerta().contains("HIPERTENSIÓN"));
    }

    @Test
    public void testEvaluarAlertasClinicas_HypoxiaAndFever() {
        Triaje triaje = new Triaje();
        triaje.setSaturacionOxigeno(92);
        triaje.setTemperatura(new BigDecimal("38.5"));

        triajeService.evaluarAlertasClinicas(triaje);

        assertTrue(triaje.getAlertaClinica());
        assertTrue(triaje.getDetalleAlerta().contains("HIPOXIA"));
        assertTrue(triaje.getDetalleAlerta().contains("ESTADO FEBRIL"));
    }

    @Test
    public void testGuardarTriajeCallsEvaluation() {
        Triaje triaje = new Triaje();
        triaje.setPresionArterialSistolica(150);
        triaje.setPresionArterialDiastolica(95);

        when(triajeRepository.save(any(Triaje.class))).thenReturn(triaje);

        Triaje saved = triajeService.guardarTriaje(triaje);

        assertNotNull(saved);
        assertTrue(saved.getAlertaClinica());
        verify(triajeRepository, times(1)).save(triaje);
    }
}
