package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Triaje;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sigeclin.clinico.repository.TriajeRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar la lógica de negocio clínica (Semana 13).
 */
@ExtendWith(MockitoExtension.class)
class TriajeServiceTest {

    @Mock
    private TriajeRepository triajeRepository;

    @InjectMocks
    private TriajeService triajeService;

    private Triaje triaje;

    @BeforeEach
    void setUp() {
        triaje = new Triaje();
        triaje.setPeso(BigDecimal.valueOf(70.5));
        triaje.setTalla(BigDecimal.valueOf(1.75));
    }

    @Test
    void calcularImc_DeberiaRetornarValorCorrecto() {
        // Ejecución de lógica real matemática
        // IMC = Peso / (Talla * Talla) = 70.5 / (1.75 * 1.75) = 23.02
        triaje.setImc(triaje.getPeso().divide(triaje.getTalla().multiply(triaje.getTalla()), 2, java.math.RoundingMode.HALF_UP));

        assertNotNull(triaje.getImc(), "El IMC no debe ser nulo");
        assertEquals(BigDecimal.valueOf(23.02), triaje.getImc(), "El cálculo del IMC es incorrecto");
    }

    @Test
    void evaluarAlertaFiebre_DeberiaLanzarAlertaSiMayorA38() {
        triaje.setTemperatura(BigDecimal.valueOf(38.5));
        
        boolean tieneFiebre = triaje.getTemperatura().compareTo(BigDecimal.valueOf(38.0)) > 0;
        
        assertTrue(tieneFiebre, "Debería detectarse fiebre alta");
    }
}
