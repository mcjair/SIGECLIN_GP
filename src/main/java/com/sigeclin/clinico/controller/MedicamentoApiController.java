package com.sigeclin.clinico.controller;

import com.sigeclin.maestras.model.Medicamento;
import com.sigeclin.maestras.repository.MedicamentoRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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

    @Getter
    @Setter
    public static class MedicamentoDto {
        private String nombreGenerico;
        private String concentracion;
        private String presentacion;
        private Boolean activo;
    }

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
    public ResponseEntity<Medicamento> crear(@RequestBody MedicamentoDto medDto) {
        Medicamento med = new Medicamento();
        med.setNombreGenerico(medDto.getNombreGenerico());
        med.setConcentracion(medDto.getConcentracion());
        med.setPresentacion(medDto.getPresentacion());
        med.setActivo(medDto.getActivo() == null || medDto.getActivo());
        return ResponseEntity.ok(medicamentoRepository.save(med));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicamento> actualizar(@PathVariable Integer id, @RequestBody MedicamentoDto medDto) {
        return medicamentoRepository.findById(id).map(existente -> {
            existente.setNombreGenerico(medDto.getNombreGenerico());
            existente.setConcentracion(medDto.getConcentracion());
            existente.setPresentacion(medDto.getPresentacion());
            if (medDto.getActivo() != null) existente.setActivo(medDto.getActivo());
            return ResponseEntity.ok(medicamentoRepository.save(existente));
        }).orElseGet(() -> ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        return medicamentoRepository.findById(id).map(existente -> {
            medicamentoRepository.delete(existente);
            return ResponseEntity.ok().<Void>build();
        }).orElseGet(() -> ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build());
    }
}
