package com.sigeclin.clinico.service;

import com.sigeclin.clinico.dto.LoteRequest;
import com.sigeclin.clinico.model.DetalleReceta;
import com.sigeclin.clinico.model.Dispensacion;
import com.sigeclin.clinico.model.LoteMedicamento;
import com.sigeclin.clinico.repository.DetalleRecetaRepository;
import com.sigeclin.clinico.repository.DispensacionRepository;
import com.sigeclin.clinico.repository.LoteMedicamentoRepository;
import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.maestras.model.Medicamento;
import com.sigeclin.maestras.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmaciaService {

    private final DetalleRecetaRepository detalleRecetaRepository;
    private final LoteMedicamentoRepository loteMedicamentoRepository;
    private final DispensacionRepository dispensacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicamentoRepository medicamentoRepository;

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecetasPendientes() {
        List<DetalleReceta> pendientes = detalleRecetaRepository
            .findByEstadoDispensacionOrderByRecetaFechaEmisionDesc("pendiente");
        Map<Integer, Map<String, Object>> recetasMap = new LinkedHashMap<>();
        for (DetalleReceta d : pendientes) {
            Integer idReceta = d.getReceta().getIdReceta();
            if (!recetasMap.containsKey(idReceta)) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("idReceta", idReceta);
                r.put("paciente", d.getReceta().getPaciente().getNombres() + " " +
                    d.getReceta().getPaciente().getApellidoPaterno());
                r.put("pacienteDni", d.getReceta().getPaciente().getNumeroDocumento());
                r.put("fecha", d.getReceta().getFechaEmision());
                r.put("estado", d.getReceta().getEstado());
                r.put("items", new ArrayList<Map<String, Object>>());
                recetasMap.put(idReceta, r);
            }
            List<Map<String, Object>> items = (List<Map<String, Object>>) recetasMap.get(idReceta).get("items");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idDetalle", d.getIdDetalleReceta());
            item.put("idMedicamento", d.getMedicamento().getIdMedicamento());
            item.put("medicamento", d.getMedicamento().getNombreGenerico());
            item.put("concentracion", d.getMedicamento().getConcentracion());
            item.put("dosis", d.getDosis());
            item.put("frecuencia", d.getFrecuencia());
            item.put("duracion", d.getDuracionDias());
            item.put("cantidad", d.getCantidadTotal());
            item.put("estadoDispensacion", d.getEstadoDispensacion());
            items.add(item);
        }
        return new ArrayList<>(recetasMap.values());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLotesDisponibles(Integer idMedicamento) {
        List<LoteMedicamento> lotes = loteMedicamentoRepository
            .findByMedicamentoIdMedicamentoAndStockActualGreaterThanOrderByFechaVencimientoAsc(idMedicamento, 0);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LoteMedicamento l : lotes) {
            if (l.getFechaVencimiento().isAfter(LocalDate.now())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("idLote", l.getIdLote());
                item.put("numeroLote", l.getNumeroLote());
                item.put("fechaVencimiento", l.getFechaVencimiento());
                item.put("stockActual", l.getStockActual());
                result.add(item);
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Object> dispensar(Integer idDetalleReceta, Integer idLote, Integer cantidad, String observaciones, Integer idUsuario) {
        DetalleReceta detalle = detalleRecetaRepository.findById(idDetalleReceta)
            .orElseThrow(() -> new RuntimeException("Detalle de receta no encontrado: " + idDetalleReceta));
        LoteMedicamento lote = loteMedicamentoRepository.findById(idLote)
            .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + idLote));
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));

        if (lote.getStockActual() < cantidad) {
            throw new RuntimeException("Stock insuficiente en lote " + lote.getNumeroLote() +
                ". Disponible: " + lote.getStockActual() + ", solicitado: " + cantidad);
        }
        if (lote.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new RuntimeException("El lote " + lote.getNumeroLote() + " está vencido desde " + lote.getFechaVencimiento());
        }
        if (cantidad > detalle.getCantidadTotal()) {
            throw new RuntimeException("Cantidad excede lo prescrito (" + detalle.getCantidadTotal() + ")");
        }

        lote.setStockActual(lote.getStockActual() - cantidad);
        loteMedicamentoRepository.save(lote);

        Dispensacion d = new Dispensacion();
        d.setDetalleReceta(detalle);
        d.setLote(lote);
        d.setUsuario(usuario);
        d.setCantidadEntregada(cantidad);
        d.setObservaciones(observaciones);
        dispensacionRepository.save(d);

        long totalDispensado = dispensacionRepository.findByDetalleRecetaIdDetalleReceta(idDetalleReceta)
            .stream().mapToInt(Dispensacion::getCantidadEntregada).sum();
        if (totalDispensado >= detalle.getCantidadTotal()) {
            detalle.setEstadoDispensacion("dispensado");
        } else {
            detalle.setEstadoDispensacion("parcial");
        }
        detalleRecetaRepository.save(detalle);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("idDispensacion", d.getIdDispensacion());
        result.put("medicamento", detalle.getMedicamento().getNombreGenerico());
        result.put("lote", lote.getNumeroLote());
        result.put("cantidad", cantidad);
        result.put("stockRestante", lote.getStockActual());
        result.put("estadoDetalle", detalle.getEstadoDispensacion());
        log.info("Dispensación exitosa: {} x {} del lote {} (stock restante: {})",
            detalle.getMedicamento().getNombreGenerico(), cantidad, lote.getNumeroLote(), lote.getStockActual());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAlertas() {
        List<Map<String, Object>> alertas = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();

        List<LoteMedicamento> stockBajo = loteMedicamentoRepository.findByStockActualLessThan(10);
        for (LoteMedicamento l : stockBajo) {
            vistos.add(l.getIdLote());
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("tipo", "stock_bajo");
            a.put("medicamento", l.getMedicamento().getNombreGenerico() + " " + l.getMedicamento().getConcentracion());
            a.put("lote", l.getNumeroLote());
            a.put("stockActual", l.getStockActual());
            a.put("stockMinimo", 10);
            alertas.add(a);
        }

        LocalDate dentroDe60Dias = LocalDate.now().plusDays(60);
        List<LoteMedicamento> porVencer = loteMedicamentoRepository.findByFechaVencimientoBefore(dentroDe60Dias);
        for (LoteMedicamento l : porVencer) {
            if (l.getStockActual() > 0 && l.getFechaVencimiento().isAfter(LocalDate.now()) && !vistos.contains(l.getIdLote())) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("tipo", "proximo_vencer");
                a.put("medicamento", l.getMedicamento().getNombreGenerico() + " " + l.getMedicamento().getConcentracion());
                a.put("lote", l.getNumeroLote());
                a.put("fechaVencimiento", l.getFechaVencimiento());
                a.put("stockActual", l.getStockActual());
                alertas.add(a);
            }
        }

        return alertas;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStockGeneral() {
        List<Medicamento> medicamentos = medicamentoRepository.findAll();
        Map<Integer, List<LoteMedicamento>> lotesPorMed = loteMedicamentoRepository.findAllWithMedicamento()
            .stream().collect(Collectors.groupingBy(l -> l.getMedicamento().getIdMedicamento(), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Medicamento m : medicamentos) {
            List<LoteMedicamento> lotes = lotesPorMed.getOrDefault(m.getIdMedicamento(), Collections.emptyList());
            int totalStock = lotes.stream().mapToInt(LoteMedicamento::getStockActual).sum();
            Optional<LoteMedicamento> loteVigente = lotes.stream()
                .filter(l -> l.getFechaVencimiento().isAfter(LocalDate.now()) && l.getStockActual() > 0)
                .findFirst();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idMedicamento", m.getIdMedicamento());
            item.put("medicamento", m.getNombreGenerico());
            item.put("concentracion", m.getConcentracion());
            item.put("presentacion", m.getPresentacion());
            item.put("stockTotal", totalStock);
            if (loteVigente.isPresent()) {
                item.put("numeroLote", loteVigente.get().getNumeroLote());
                item.put("fechaVencimiento", loteVigente.get().getFechaVencimiento());
                item.put("stockActual", loteVigente.get().getStockActual());
                item.put("vencido", false);
                item.put("sinStock", false);
            } else if (!lotes.isEmpty()) {
                LoteMedicamento ultimo = lotes.get(lotes.size() - 1);
                item.put("numeroLote", ultimo.getNumeroLote());
                item.put("fechaVencimiento", ultimo.getFechaVencimiento());
                item.put("stockActual", totalStock);
                item.put("vencido", ultimo.getFechaVencimiento().isBefore(LocalDate.now()));
                item.put("sinStock", totalStock == 0);
            } else {
                item.put("numeroLote", "--");
                item.put("fechaVencimiento", null);
                item.put("stockActual", 0);
                item.put("vencido", false);
                item.put("sinStock", true);
            }
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistorialDispensaciones() {
        List<Dispensacion> list = dispensacionRepository.findAllWithDetails();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dispensacion d : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idDispensacion", d.getIdDispensacion());
            item.put("medicamento", d.getDetalleReceta().getMedicamento().getNombreGenerico());
            item.put("paciente", d.getDetalleReceta().getReceta().getPaciente().getNombres() + " " +
                d.getDetalleReceta().getReceta().getPaciente().getApellidoPaterno());
            item.put("lote", d.getLote().getNumeroLote());
            item.put("cantidad", d.getCantidadEntregada());
            item.put("fecha", d.getFechaDispensacion());
            item.put("usuario", d.getUsuario().getUsername());
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> crearLote(LoteRequest request, Integer idUsuario) {
        Medicamento med = medicamentoRepository.findById(request.getIdMedicamento())
            .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + request.getIdMedicamento()));
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));

        if (request.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new RuntimeException("La fecha de vencimiento debe ser posterior a la fecha actual");
        }

        boolean existeLote = loteMedicamentoRepository.existsByMedicamentoIdMedicamentoAndNumeroLoteIgnoreCase(
            request.getIdMedicamento(), request.getNumeroLote());
        if (existeLote) {
            throw new RuntimeException("El lote " + request.getNumeroLote() + " ya existe para este medicamento");
        }

        LoteMedicamento lote = new LoteMedicamento();
        lote.setMedicamento(med);
        lote.setNumeroLote(request.getNumeroLote().toUpperCase());
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setStockInicial(request.getCantidadInicial());
        lote.setStockActual(request.getCantidadInicial());
        lote.setUsuarioRegistro(usuario);
        lote.setFechaIngreso(java.time.LocalDateTime.now());
        loteMedicamentoRepository.save(lote);

        log.info("Lote creado: {} para {} (cant: {}, vence: {})",
            lote.getNumeroLote(), med.getNombreGenerico(), request.getCantidadInicial(), request.getFechaVencimiento());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("idLote", lote.getIdLote());
        result.put("medicamento", med.getNombreGenerico());
        result.put("numeroLote", lote.getNumeroLote());
        result.put("fechaVencimiento", lote.getFechaVencimiento());
        result.put("cantidadInicial", request.getCantidadInicial());
        result.put("mensaje", "Lote registrado exitosamente");
        return result;
    }
}
