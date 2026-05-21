package com.sigeclin.filiacion.controller;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.TipoDocumento;
import com.sigeclin.filiacion.repository.TipoDocumentoRepository;
import com.sigeclin.filiacion.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admission")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    @GetMapping("/registro")
    public String showRegistroForm(@RequestParam(required = false) String search, Model model) {
        List<TipoDocumento> tiposDoc = tipoDocumentoRepository.findAll();
        model.addAttribute("tiposDoc", tiposDoc);
        
        Paciente paciente = new Paciente();
        paciente.setTipoDocumento(new TipoDocumento());
        if (search != null && !search.isEmpty()) {
            pacienteService.buscarPorDniOHC(search).ifPresent(p -> {
                model.addAttribute("pacienteExistente", true);
                // Copiamos datos al objeto del formulario
                paciente.setIdPersona(p.getIdPersona());
                paciente.setNumeroDocumento(p.getNumeroDocumento());
                paciente.setNombres(p.getNombres());
                paciente.setApellidoPaterno(p.getApellidoPaterno());
                paciente.setApellidoMaterno(p.getApellidoMaterno());
                paciente.setFechaNacimiento(p.getFechaNacimiento());
                paciente.setSexo(p.getSexo());
                paciente.setTipoDocumento(p.getTipoDocumento());
                paciente.setDireccion(p.getDireccion());
                paciente.setReferenciaDireccion(p.getReferenciaDireccion());
                paciente.setNumeroHistoriaClinica(p.getNumeroHistoriaClinica());
                paciente.setTelefonoPrincipal(p.getTelefonoPrincipal());
                paciente.setTelefonoSecundario(p.getTelefonoSecundario());
                paciente.setCorreoElectronico(p.getCorreoElectronico());
            });
        }
        
        model.addAttribute("paciente", paciente);
        return "admission/registro";
    }

    @GetMapping("/buscar")
    public String buscarPaciente(@RequestParam String query) {
        return "redirect:/admission/registro?search=" + query;
    }

    @GetMapping("/api/buscar/{documento}")
    @ResponseBody
    public ResponseEntity<?> buscarPacienteApi(@PathVariable String documento) {
        try {
            return pacienteService.buscarPorDniOHC(documento)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/guardar")
    public String registrarPaciente(@ModelAttribute Paciente paciente, @RequestParam(required = false) String servicio, RedirectAttributes redirectAttributes) {
        try {
            if (servicio != null && !servicio.isEmpty()) {
                paciente.setServicioSolicitado(servicio);
            }
            Paciente guardado = pacienteService.registrarPaciente(paciente);
            String hc = guardado.getNumeroHistoriaClinica();
            System.out.println("PacienteController - Guardado con éxito. HC: " + hc);
            
            redirectAttributes.addFlashAttribute("success", "Paciente " + guardado.getNombres() + " registrado con éxito. HC: " + hc);
            
            // Regresamos al formulario de admisión asegurando que los flash attributes se procesen
            return "redirect:/admission/registro?saved=true";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admission/registro?error=true&msg=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
