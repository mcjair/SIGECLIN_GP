package com.sigeclin.clinico.controller;

import com.sigeclin.maestras.model.Medicamento;
import com.sigeclin.maestras.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicamentos")
@RequiredArgsConstructor
public class MedicamentoApiController {

    private final MedicamentoRepository medicamentoRepository;

    @GetMapping("/buscar")
    public ResponseEntity<List<Map<String, Object>>> buscar(@RequestParam String q) {
        List<Medicamento> list = medicamentoRepository.buscarPorNombre(q);
        List<Map<String, Object>> result = list.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("idMedicamento", m.getIdMedicamento());
            map.put("nombreGenerico", m.getNombreGenerico());
            map.put("concentracion", m.getConcentracion());
            map.put("presentacion", m.getPresentacion());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<Medicamento>> listarTodos() {
        return ResponseEntity.ok(medicamentoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Medicamento> crear(@RequestBody Medicamento med) {
        if (med.getActivo() == null) med.setActivo(true);
        return ResponseEntity.ok(medicamentoRepository.save(med));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicamento> actualizar(@PathVariable Integer id, @RequestBody Medicamento med) {
        return medicamentoRepository.findById(id).map(existente -> {
            existente.setNombreGenerico(med.getNombreGenerico());
            existente.setConcentracion(med.getConcentracion());
            existente.setPresentacion(med.getPresentacion());
            if (med.getActivo() != null) existente.setActivo(med.getActivo());
            return ResponseEntity.ok(medicamentoRepository.save(existente));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        return medicamentoRepository.findById(id).map(existente -> {
            medicamentoRepository.delete(existente);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
