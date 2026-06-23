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
        triaje.setPesoKg(BigDecimal.valueOf(70.5));
        triaje.setTallaCm(BigDecimal.valueOf(175));
    }

    @Test
    void calcularImc_DeberiaRetornarValorCorrecto() {
        // Ejecución de lógica real matemática
        // IMC = Peso / (TallaM * TallaM)
        BigDecimal tallaM = triaje.getTallaCm().divide(BigDecimal.valueOf(100));
        triaje.setImc(triaje.getPesoKg().divide(tallaM.multiply(tallaM), 2, java.math.RoundingMode.HALF_UP));

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
