package com.sigeclin.filiacion.service;

import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.repository.PersonalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PersonalServiceTest {

    @Mock
    private PersonalRepository personalRepository;

    @InjectMocks
    private PersonalService personalService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Personal crearPersonalBase() {
        Personal p = new Personal();
        p.setIdPersona(1);
        p.setNombres("Juan");
        p.setApellidoPaterno("Perez");
        p.setNumeroDocumento("12345678");
        p.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        p.setSexo("M");
        p.setFechaIngreso(LocalDate.of(2024, 1, 1));
        p.setIdTipoPersonal(1);
        return p;
    }

    @Test
    public void testBuscarPorId_Existing() {
        Personal p = crearPersonalBase();
        when(personalRepository.findById(1)).thenReturn(Optional.of(p));

        Personal result = personalService.buscarPorId(1);

        assertNotNull(result);
        assertEquals("Juan", result.getNombres());
        verify(personalRepository, times(1)).findById(1);
    }

    @Test
    public void testBuscarPorId_NotFound() {
        when(personalRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> personalService.buscarPorId(99));
    }

    @Test
    public void testBuscarPorId_NullId() {
        assertThrows(RuntimeException.class, () -> personalService.buscarPorId(null));
    }

    @Test
    public void testGuardar_NuevoPersonal() {
        Personal p = crearPersonalBase();
        p.setIdPersona(null);
        when(personalRepository.save(any(Personal.class))).thenReturn(p);

        Personal result = personalService.guardar(p);

        assertNotNull(result);
        assertNotNull(result.getFechaCreacion());
        verify(personalRepository, times(1)).save(p);
    }

    @Test
    public void testGuardar_ActualizarPersonal() {
        Personal p = crearPersonalBase();
        when(personalRepository.save(any(Personal.class))).thenReturn(p);

        Personal result = personalService.guardar(p);

        assertNotNull(result);
        assertNotNull(result.getFechaActualizacion());
        verify(personalRepository, times(1)).save(p);
    }

    @Test
    public void testGuardar_UpperCaseColegiatura() {
        Personal p = crearPersonalBase();
        p.setNumeroColegiatura("cmp-12345");
        when(personalRepository.save(any(Personal.class))).thenReturn(p);

        personalService.guardar(p);

        assertEquals("CMP-12345", p.getNumeroColegiatura());
    }

    @Test
    public void testGuardar_NullPersonal() {
        assertThrows(RuntimeException.class, () -> personalService.guardar(null));
    }

    @Test
    public void testToggleEstado_ActivoToInactivo() {
        Personal p = crearPersonalBase();
        p.setEstadoLaboral("activo");
        when(personalRepository.findById(1)).thenReturn(Optional.of(p));
        when(personalRepository.save(any(Personal.class))).thenReturn(p);

        personalService.toggleEstado(1);

        assertEquals("inactivo", p.getEstadoLaboral());
        verify(personalRepository, times(1)).save(p);
    }

    @Test
    public void testToggleEstado_InactivoToActivo() {
        Personal p = crearPersonalBase();
        p.setEstadoLaboral("inactivo");
        when(personalRepository.findById(1)).thenReturn(Optional.of(p));
        when(personalRepository.save(any(Personal.class))).thenReturn(p);

        personalService.toggleEstado(1);

        assertEquals("activo", p.getEstadoLaboral());
        verify(personalRepository, times(1)).save(p);
    }

    @Test
    public void testEliminar_DesactivaPersonal() {
        Personal p = crearPersonalBase();
        when(personalRepository.findById(1)).thenReturn(Optional.of(p));
        when(personalRepository.save(any(Personal.class))).thenReturn(p);

        personalService.eliminar(1);

        assertEquals("inactivo", p.getEstadoLaboral());
        verify(personalRepository, times(1)).save(p);
    }
}
