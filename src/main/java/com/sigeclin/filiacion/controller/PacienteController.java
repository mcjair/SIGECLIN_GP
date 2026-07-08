package com.sigeclin.filiacion.controller;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.TipoDocumento;
import com.sigeclin.filiacion.repository.TipoDocumentoRepository;
import com.sigeclin.filiacion.service.IPacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Controller
@RequestMapping("/admission")
@RequiredArgsConstructor
public class PacienteController {

    private final IPacienteService pacienteService;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("idPersona", "tipoDocumento.idTipoDocumento", "numeroDocumento", 
            "nombres", "apellidoPaterno", "apellidoMaterno", "fechaNacimiento", "sexo", 
            "direccion", "referenciaDireccion", "numeroHistoriaClinica", "telefonoPrincipal", 
            "telefonoSecundario", "correoElectronico", "servicioSolicitado");
    }

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
    public ResponseEntity<Paciente> buscarPacienteApi(@PathVariable String documento) {
        try {
            return pacienteService.buscarPorDniOHC(documento)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            log.error("Error al buscar paciente por documento {}: {}", documento, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/guardar")
    public String registrarPaciente(@Valid @ModelAttribute("paciente") PacienteDto pacienteDto, BindingResult bindingResult,
                                    @RequestParam(required = false) String servicio,
                                    org.springframework.ui.Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tiposDoc", tipoDocumentoRepository.findAll());
            String msg = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Error de validación en los datos ingresados.");
            
            model.addAttribute("errorObj", msg);
            return "admission/registro";
        }
        try {
            Paciente paciente = new Paciente();
            paciente.setIdPersona(pacienteDto.getIdPersona());
            if (pacienteDto.getTipoDocumento() != null && pacienteDto.getTipoDocumento().getIdTipoDocumento() != null) {
                TipoDocumento td = tipoDocumentoRepository.findById(pacienteDto.getTipoDocumento().getIdTipoDocumento()).orElse(null);
                paciente.setTipoDocumento(td);
            }
            paciente.setNumeroDocumento(pacienteDto.getNumeroDocumento());
            paciente.setNombres(pacienteDto.getNombres());
            paciente.setApellidoPaterno(pacienteDto.getApellidoPaterno());
            paciente.setApellidoMaterno(pacienteDto.getApellidoMaterno());
            paciente.setFechaNacimiento(pacienteDto.getFechaNacimiento());
            paciente.setSexo(pacienteDto.getSexo());
            paciente.setTelefonoPrincipal(pacienteDto.getTelefonoPrincipal());
            paciente.setCorreoElectronico(pacienteDto.getCorreoElectronico());
            paciente.setDireccion(pacienteDto.getDireccion());
            paciente.setGrupoSanguineo(pacienteDto.getGrupoSanguineo());
            paciente.setFactorRh(pacienteDto.getFactorRh());
            paciente.setContactoEmergenciaNombre(pacienteDto.getContactoEmergenciaNombre());
            paciente.setContactoEmergenciaTelefono(pacienteDto.getContactoEmergenciaTelefono());
            paciente.setEstadoCivil(pacienteDto.getEstadoCivil());
            paciente.setOcupacion(pacienteDto.getOcupacion());
            paciente.setReferenciaDireccion(pacienteDto.getReferenciaDireccion());

            if (servicio != null && !servicio.isEmpty()) {
                paciente.setServicioSolicitado(servicio);
            } else if (pacienteDto.getServicioSolicitado() != null) {
                paciente.setServicioSolicitado(pacienteDto.getServicioSolicitado());
            }

            Paciente guardado = pacienteService.registrarPaciente(paciente);
            String hc = guardado.getNumeroHistoriaClinica();
            log.debug("Paciente registrado con éxito. HC: {}", hc);
            
            redirectAttributes.addFlashAttribute("success", "Paciente " + guardado.getNombres() + " registrado con éxito. HC: " + hc);
            
            return "redirect:/admission/registro?saved=true";
        } catch (Exception e) {
            log.error("Error al registrar paciente", e);
            return "redirect:/admission/registro?error=true&msg=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @lombok.Getter
    @lombok.Setter
    public static class PacienteDto {
        private Integer idPersona;
        private TipoDocumentoDto tipoDocumento;
        private String numeroDocumento;
        private String nombres;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private java.time.LocalDate fechaNacimiento;
        private String sexo;
        private String telefonoPrincipal;
        private String correoElectronico;
        private String direccion;
        private String grupoSanguineo;
        private String factorRh;
        private String contactoEmergenciaNombre;
        private String contactoEmergenciaTelefono;
        private String estadoCivil;
        private String ocupacion;
        private String referenciaDireccion;
        private String servicioSolicitado;
    }

    @lombok.Getter
    @lombok.Setter
    public static class TipoDocumentoDto {
        private Integer idTipoDocumento;
    }
}
