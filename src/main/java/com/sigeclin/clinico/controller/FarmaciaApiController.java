package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.dto.DispensarRequest;
import com.sigeclin.clinico.dto.LoteRequest;
import com.sigeclin.clinico.service.FarmaciaService;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/farmacia")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')") // A01: Control de acceso por rol
public class FarmaciaApiController {

    private final FarmaciaService farmaciaService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/recetas")
    public ResponseEntity<List<Map<String, Object>>> listarRecetasPendientes() {
        return ResponseEntity.ok(farmaciaService.getRecetasPendientes());
    }

    @GetMapping("/lotes/{idMedicamento}")
    public ResponseEntity<List<Map<String, Object>>> listarLotesDisponibles(
            @PathVariable Integer idMedicamento) {
        return ResponseEntity.ok(farmaciaService.getLotesDisponibles(idMedicamento));
    }

    @PostMapping("/dispensar")
    public ResponseEntity<Map<String, Object>> dispensar(@Valid @RequestBody DispensarRequest request,
                                       Authentication auth) {
        try {
            String username = auth != null ? auth.getName() : "admin";
            Integer idUsuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username))
                .getIdPersona();
            Map<String, Object> result = farmaciaService.dispensar(
                request.getIdDetalleReceta(),
                request.getIdLote(),
                request.getCantidad(),
                request.getObservaciones(),
                idUsuario
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("Error en dispensación: {}", e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<Map<String, Object>>> getAlertas() {
        return ResponseEntity.ok(farmaciaService.getAlertas());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Map<String, Object>>> getStock() {
        return ResponseEntity.ok(farmaciaService.getStockGeneral());
    }

    @GetMapping("/historial")
    public ResponseEntity<List<Map<String, Object>>> getHistorial() {
        return ResponseEntity.ok(farmaciaService.getHistorialDispensaciones());
    }

    @PostMapping("/lote")
    public ResponseEntity<Map<String, Object>> crearLote(@Valid @RequestBody LoteRequest request,
                                       Authentication auth) {
        try {
            String username = auth != null ? auth.getName() : "admin";
            Integer idUsuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username))
                .getIdPersona();
            Map<String, Object> result = farmaciaService.crearLote(request, idUsuario);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.warn("Error creando lote: {}", e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
