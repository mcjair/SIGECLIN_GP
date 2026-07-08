package com.sigeclin.clinico.service;

import com.sigeclin.clinico.dto.LoteRequest;
import com.sigeclin.clinico.model.DetalleReceta;
import com.sigeclin.clinico.model.Dispensacion;
import com.sigeclin.clinico.model.LoteMedicamento;
import com.sigeclin.clinico.model.RecetaMedica;
import com.sigeclin.clinico.repository.DetalleRecetaRepository;
import com.sigeclin.clinico.repository.DispensacionRepository;
import com.sigeclin.clinico.repository.LoteMedicamentoRepository;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.maestras.model.Medicamento;
import com.sigeclin.maestras.repository.MedicamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmaciaServiceTest {

    @Mock
    private DetalleRecetaRepository detalleRecetaRepository;
    @Mock
    private LoteMedicamentoRepository loteMedicamentoRepository;
    @Mock
    private DispensacionRepository dispensacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MedicamentoRepository medicamentoRepository;

    @InjectMocks
    private FarmaciaService farmaciaService;

    @Test
    void getRecetasPendientes_DeberiaMapearCorrectamente() {
        Paciente paciente = new Paciente();
        paciente.setNombres("Juan");
        paciente.setApellidoPaterno("Perez");
        paciente.setNumeroDocumento("12345678");

        RecetaMedica receta = new RecetaMedica();
        receta.setIdReceta(1);
        receta.setPaciente(paciente);
        receta.setFechaEmision(LocalDateTime.of(2026, Month.JULY, 7, 12, 0));
        receta.setEstado("pendiente");

        Medicamento med = new Medicamento();
        med.setIdMedicamento(10);
        med.setNombreGenerico("Paracetamol");
        med.setConcentracion("500mg");

        DetalleReceta d = new DetalleReceta();
        d.setIdDetalleReceta(100);
        d.setReceta(receta);
        d.setMedicamento(med);
        d.setDosis("1 tableta");
        d.setFrecuencia("Cada 8 horas");
        d.setDuracionDias(3);
        d.setCantidadTotal(9);
        d.setEstadoDispensacion("pendiente");

        when(detalleRecetaRepository.findByEstadoDispensacionOrderByRecetaFechaEmisionDesc("pendiente"))
            .thenReturn(List.of(d));

        List<Map<String, Object>> result = farmaciaService.getRecetasPendientes();

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> rMap = result.get(0);
        assertEquals(1, rMap.get("idReceta"));
        assertEquals("Juan Perez", rMap.get("paciente"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) rMap.get("items");
        assertEquals(1, items.size());
        assertEquals("Paracetamol", items.get(0).get("medicamento"));
    }

    @Test
    void getLotesDisponibles_DeberiaFiltrarYRetornar() {
        LoteMedicamento loteValido = new LoteMedicamento();
        loteValido.setIdLote(1);
        loteValido.setNumeroLote("LOT-123");
        loteValido.setFechaVencimiento(LocalDate.of(2026, Month.DECEMBER, 31));
        loteValido.setStockActual(50);

        when(loteMedicamentoRepository.findByMedicamentoIdMedicamentoAndStockActualGreaterThanOrderByFechaVencimientoAsc(10, 0))
            .thenReturn(List.of(loteValido));

        List<Map<String, Object>> result = farmaciaService.getLotesDisponibles(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOT-123", result.get(0).get("numeroLote"));
    }

    @Test
    void dispensar_DeberiaRegistrarDispensacionYReducirStock() {
        Paciente paciente = new Paciente();
        paciente.setNombres("Juan");
        paciente.setApellidoPaterno("Perez");

        RecetaMedica receta = new RecetaMedica();
        receta.setPaciente(paciente);

        Medicamento med = new Medicamento();
        med.setNombreGenerico("Paracetamol");

        DetalleReceta d = new DetalleReceta();
        d.setIdDetalleReceta(100);
        d.setMedicamento(med);
        d.setCantidadTotal(10);
        d.setEstadoDispensacion("pendiente");
        d.setReceta(receta);

        LoteMedicamento lote = new LoteMedicamento();
        lote.setIdLote(5);
        lote.setNumeroLote("LOT-XYZ");
        lote.setStockActual(20);
        lote.setFechaVencimiento(LocalDate.of(2027, Month.JULY, 7));

        Usuario usuario = new Usuario();
        usuario.setIdPersona(2);

        when(detalleRecetaRepository.findById(100)).thenReturn(Optional.of(d));
        when(loteMedicamentoRepository.findById(5)).thenReturn(Optional.of(lote));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario));
        when(dispensacionRepository.findByDetalleRecetaIdDetalleReceta(100)).thenReturn(new ArrayList<>());

        Map<String, Object> result = farmaciaService.dispensar(100, 5, 5, "Dispensación de prueba", 2);

        assertNotNull(result);
        assertEquals(15, lote.getStockActual());
        assertEquals("parcial", d.getEstadoDispensacion());
        verify(loteMedicamentoRepository, times(1)).save(lote);
        verify(dispensacionRepository, times(1)).save(any(Dispensacion.class));
    }

    @Test
    void getAlertas_DeberiaDetectarStockBajoYVencimiento() {
        Medicamento med = new Medicamento();
        med.setNombreGenerico("Ibuprofeno");
        med.setConcentracion("400mg");

        LoteMedicamento lBajo = new LoteMedicamento();
        lBajo.setIdLote(1);
        lBajo.setMedicamento(med);
        lBajo.setNumeroLote("L-001");
        lBajo.setStockActual(5);

        LoteMedicamento lVence = new LoteMedicamento();
        lVence.setIdLote(2);
        lVence.setMedicamento(med);
        lVence.setNumeroLote("L-002");
        lVence.setStockActual(30);
        lVence.setFechaVencimiento(LocalDate.of(2026, Month.JULY, 22));

        when(loteMedicamentoRepository.findByStockActualLessThan(10)).thenReturn(List.of(lBajo));
        when(loteMedicamentoRepository.findByFechaVencimientoBefore(any(LocalDate.class))).thenReturn(List.of(lVence));

        List<Map<String, Object>> result = farmaciaService.getAlertas();

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void getStockGeneral_DeberiaMapearStockCompleto() {
        Medicamento m = new Medicamento();
        m.setIdMedicamento(1);
        m.setNombreGenerico("Amoxicilina");

        LoteMedicamento l = new LoteMedicamento();
        l.setMedicamento(m);
        l.setStockActual(100);
        l.setFechaVencimiento(LocalDate.of(2026, Month.DECEMBER, 31));
        l.setNumeroLote("LOT-111");

        when(medicamentoRepository.findAll()).thenReturn(List.of(m));
        when(loteMedicamentoRepository.findAllWithMedicamento()).thenReturn(List.of(l));

        List<Map<String, Object>> result = farmaciaService.getStockGeneral();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Amoxicilina", result.get(0).get("medicamento"));
        assertEquals(100, result.get(0).get("stockTotal"));
    }

    @Test
    void getHistorialDispensaciones_DeberiaRetornarHistorial() {
        Paciente paciente = new Paciente();
        paciente.setNombres("Maria");
        paciente.setApellidoPaterno("Gomez");

        RecetaMedica receta = new RecetaMedica();
        receta.setPaciente(paciente);

        Medicamento med = new Medicamento();
        med.setNombreGenerico("Loratadina");

        DetalleReceta det = new DetalleReceta();
        det.setMedicamento(med);
        det.setReceta(receta);

        LoteMedicamento lote = new LoteMedicamento();
        lote.setNumeroLote("LOTE-X");

        Usuario user = new Usuario();
        user.setUsername("farmaceutico");

        Dispensacion d = new Dispensacion();
        d.setIdDispensacion(10);
        d.setDetalleReceta(det);
        d.setLote(lote);
        d.setUsuario(user);
        d.setCantidadEntregada(5);
        d.setFechaDispensacion(LocalDateTime.of(2026, Month.JULY, 7, 12, 0));

        when(dispensacionRepository.findAllWithDetails()).thenReturn(List.of(d));

        List<Map<String, Object>> result = farmaciaService.getHistorialDispensaciones();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Loratadina", result.get(0).get("medicamento"));
        assertEquals("Maria Gomez", result.get(0).get("paciente"));
    }

    @Test
    void crearLote_DeberiaCrearExitosamente() {
        Medicamento med = new Medicamento();
        med.setIdMedicamento(1);
        med.setNombreGenerico("Vitamina C");

        Usuario user = new Usuario();
        user.setIdPersona(2);

        LoteRequest req = new LoteRequest();
        req.setIdMedicamento(1);
        req.setNumeroLote("L-NEW");
        req.setFechaVencimiento(LocalDate.of(2027, Month.JULY, 7));
        req.setCantidadInicial(500);

        when(medicamentoRepository.findById(1)).thenReturn(Optional.of(med));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(user));
        when(loteMedicamentoRepository.existsByMedicamentoIdMedicamentoAndNumeroLoteIgnoreCase(1, "L-NEW")).thenReturn(false);

        Map<String, Object> result = farmaciaService.crearLote(req, 2);

        assertNotNull(result);
        assertEquals("L-NEW", result.get("numeroLote"));
        assertEquals("Vitamina C", result.get("medicamento"));
        verify(loteMedicamentoRepository, times(1)).save(any(LoteMedicamento.class));
    }
}
