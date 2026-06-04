package com.sigeclin.clinico.service;

import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.OrdenMedica;
import com.sigeclin.clinico.model.ResultadoLaboratorio;
import com.sigeclin.clinico.repository.ConsultaRepository;
import com.sigeclin.clinico.repository.OrdenMedicaRepository;
import com.sigeclin.clinico.repository.ResultadoLaboratorioRepository;
import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.repository.PersonalRepository;
import com.sigeclin.maestras.model.Examen;
import com.sigeclin.maestras.repository.ExamenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaboratorioService {

    private final OrdenMedicaRepository ordenMedicaRepository;
    private final ResultadoLaboratorioRepository resultadoLaboratorioRepository;
    private final ExamenRepository examenRepository;
    private final ConsultaRepository consultaRepository;
    private final PersonalRepository personalRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrdenesConResultados(String tipo) {
        List<OrdenMedica> ordenes = tipo != null
            ? ordenMedicaRepository.findByTipoAndEstadoWithResultados(tipo, "solicitada")
            : ordenMedicaRepository.findAllByTipoWithResultados("LABORATORIO");
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (OrdenMedica o : ordenes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idOrden", o.getIdOrden());
            item.put("idCiex", o.getIdCiex());
            item.put("fecha", o.getFechaSolicitud());
            item.put("estado", o.getEstado());
            item.put("urgente", o.getUrgente());
            item.put("indicaciones", o.getIndicaciones());
            item.put("resultados", o.getResultados());

            String pacienteNombre = "---";
            String pacienteDni = "---";
            Integer pacienteId = null;
            String medicoSolicitante = "---";
            try {
                Consulta consulta = consultaRepository.findById(o.getIdConsulta()).orElse(null);
                if (consulta != null && consulta.getPaciente() != null) {
                    pacienteNombre = consulta.getPaciente().getNombres() + " " + consulta.getPaciente().getApellidoPaterno();
                    pacienteDni = consulta.getPaciente().getNumeroDocumento();
                    pacienteId = consulta.getPaciente().getIdPersona();
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener paciente para orden {}: {}", o.getIdOrden(), e.getMessage());
            }
            try {
                Personal doctor = personalRepository.findById(o.getIdPersonalSolicitante()).orElse(null);
                if (doctor != null) {
                    medicoSolicitante = doctor.getNombres() + " " + doctor.getApellidoPaterno()
                        + (doctor.getNumeroColegiatura() != null ? " (" + doctor.getNumeroColegiatura() + ")" : "");
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener medico solicitante para orden {}: {}", o.getIdOrden(), e.getMessage());
            }
            item.put("pacienteNombre", pacienteNombre);
            item.put("pacienteDni", pacienteDni);
            item.put("pacienteId", pacienteId);
            item.put("medicoSolicitante", medicoSolicitante);
            if (!o.getResultados().isEmpty()) {
                ResultadoLaboratorio r = o.getResultados().get(0);
                item.put("resultado", r.getValorResultado());
                item.put("unidad", r.getUnidad());
            } else {
                item.put("resultado", null);
                item.put("unidad", null);
            }
            resultado.add(item);
        }
        return resultado;
    }

    public List<Examen> getExamenesPorArea(String area) {
        return examenRepository.findByAreaOrderByNombre(area);
    }

    @Transactional(readOnly = true)
    public Map<String, List<Examen>> getExamenesAgrupados() {
        return examenRepository.findAllActivosOrderByArea()
            .stream().collect(Collectors.groupingBy(Examen::getArea, LinkedHashMap::new, Collectors.toList()));
    }

    @Transactional
    public ResultadoLaboratorio ingresarResultado(Integer idOrden, String codigoExamen, String valor,
                                                   Double rangoMin, Double rangoMax, String unidad) {
        OrdenMedica orden = ordenMedicaRepository.findById(idOrden)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + idOrden));
        ResultadoLaboratorio rl = new ResultadoLaboratorio();
        rl.setOrden(orden);
        rl.setCodigoExamen(codigoExamen);
        rl.setValorResultado(valor);
        rl.setUnidad(unidad);
        rl.setRangoMinimo(rangoMin);
        rl.setRangoMaximo(rangoMax);
        if (rangoMin != null && rangoMax != null && valor != null) {
            try {
                double numValor = Double.parseDouble(valor.replace(",", "."));
                rl.setEsAnormal(numValor < rangoMin || numValor > rangoMax);
            } catch (NumberFormatException e) {
                rl.setEsAnormal(false);
            }
        } else {
            rl.setEsAnormal(false);
        }
        ResultadoLaboratorio saved = resultadoLaboratorioRepository.save(rl);
        orden.setFechaResultado(saved.getFechaProcesamiento());
        orden.setResultadoTexto(valor);
        orden.setEstado("resultado_ingresado");
        ordenMedicaRepository.save(orden);
        log.info("Resultado ingresado: {} = {} {} (anormal={})", codigoExamen, valor, unidad, rl.getEsAnormal());
        return saved;
    }

    @Transactional
    public void validarOrden(Integer idOrden) {
        OrdenMedica orden = ordenMedicaRepository.findById(idOrden)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + idOrden));
        orden.setEstado("validado");
        ordenMedicaRepository.save(orden);
    }

    public Map<String, Object> getPanelData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("examenesPorArea", getExamenesAgrupados());
        data.put("ordenes", getOrdenesConResultados(null));
        data.put("pendientes", ordenMedicaRepository.countByTipoAndEstado("LABORATORIO", "solicitada"));
        return data;
    }
}
