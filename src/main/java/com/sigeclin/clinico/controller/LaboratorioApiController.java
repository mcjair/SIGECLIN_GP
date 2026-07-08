package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.service.LaboratorioService;
import com.sigeclin.maestras.model.Examen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/laboratorio")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')") // A01: Control de acceso por rol
public class LaboratorioApiController {

    private final LaboratorioService laboratorioService;

    @GetMapping("/ordenes")
    public ResponseEntity<List<Map<String, Object>>> listarOrdenes(
            @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(laboratorioService.getOrdenesConResultados(tipo));
    }

    @GetMapping("/examenes")
    public ResponseEntity<Map<String, List<Examen>>> listarExamenes() {
        return ResponseEntity.ok(laboratorioService.getExamenesAgrupados());
    }

    @GetMapping("/examenes/{area}")
    public ResponseEntity<List<Examen>> listarExamenesPorArea(@PathVariable String area) {
        return ResponseEntity.ok(laboratorioService.getExamenesPorArea(area));
    }

    @PostMapping("/resultado")
    public ResponseEntity<Map<String, Object>> ingresarResultado(@RequestBody Map<String, Object> body) {
        Integer idOrden = Integer.parseInt(body.get("idOrden").toString());
        String codigoExamen = (String) body.get("codigoExamen");
        String valor = (String) body.get("valor");
        String unidad = (String) body.get("unidad");
        Object rMinRaw = body.get("rangoMinimo");
        Object rMaxRaw = body.get("rangoMaximo");
        Double rangoMin = (rMinRaw != null && !"null".equals(rMinRaw.toString()))
            ? Double.parseDouble(rMinRaw.toString()) : null;
        Double rangoMax = (rMaxRaw != null && !"null".equals(rMaxRaw.toString()))
            ? Double.parseDouble(rMaxRaw.toString()) : null;

        var rl = laboratorioService.ingresarResultado(idOrden, codigoExamen, valor, rangoMin, rangoMax, unidad);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("idResultado", rl.getIdResultado());
        res.put("esAnormal", rl.getEsAnormal());
        res.put("mensaje", "Resultado ingresado correctamente");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/orden/{id}/validar")
    public ResponseEntity<Map<String, Object>> validarOrden(@PathVariable Integer id) {
        laboratorioService.validarOrden(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("mensaje", "Orden validada exitosamente");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/panel")
    public ResponseEntity<Map<String, Object>> getPanelData() {
        return ResponseEntity.ok(laboratorioService.getPanelData());
    }
}
