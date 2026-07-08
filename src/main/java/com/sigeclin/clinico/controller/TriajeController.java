package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.service.ITriajeService;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.filiacion.service.IPacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Controller
@RequestMapping("/triaje")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ENFERMERIA', 'ADMIN')") // A01: Control de acceso por rol
public class TriajeController {

    private final ITriajeService triajeService;
    private final IPacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;

    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("paciente.idPersona", "temperatura", "presionArterialSistolica", 
            "presionArterialDiastolica", "frecuenciaCardiaca", "frecuenciaRespiratoria", "saturacionOxigeno", 
            "pesoKg", "tallaCm", "servicioDestino", "observaciones", "clasificacionUrgencia");
    }

    @GetMapping("/nuevo")
    public String mostrarListaEspera(@RequestParam(required = false) String hc, Model model) {
        if (hc != null && !hc.isEmpty()) {
            Optional<Paciente> paciente = pacienteService.buscarPorDniOHC(hc);
            if (paciente.isPresent()) {
                return "redirect:/triaje/registrar/" + paciente.get().getIdPersona();
            }
        }
        
        // Cargar lista de pacientes que vienen de Caja (Pendientes de Triaje)
        model.addAttribute("pacientesPendientes", pacienteService.obtenerPendientesTriaje());
        
        return "clinico/triaje_busqueda";
    }

    private static final String FLASH_ERROR = "error";
    private static final String FLASH_SUCCESS = "success";
    private static final String REDIRECT_NUEVO = "redirect:/triaje/nuevo";

    @Transactional(readOnly = true)
    @GetMapping("/registrar/{idPaciente}")
    public String mostrarFormulario(@PathVariable Integer idPaciente, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Triaje - Cargando formulario para Paciente ID: {}", idPaciente);
        
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorId(idPaciente);
        if (pacienteOpt.isEmpty()) {
            log.warn("Triaje - Paciente NO encontrado: {}", idPaciente);
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Paciente no encontrado.");
            return REDIRECT_NUEVO;
        }

        Paciente p = pacienteOpt.get();
        // Forzar carga de datos básicos para evitar LazyInitializationException en la vista
        if (p.getNombres() != null) {
            log.debug("Triaje - Inicializando paciente: {}", p.getApellidoPaterno());
        }
        
        Triaje triaje = new Triaje();
        triaje.setPaciente(p);
        
        model.addAttribute("triaje", triaje);
        model.addAttribute("paciente", p);
        model.addAttribute("edad", p.getEdadCompleta());
        
        log.debug("Triaje - Formulario cargado con éxito para: {}", p.getNombres());
        return "clinico/triaje_registro";
    }

    private String normalizarServicioDestino(String sd, Paciente paciente) {
        if (sd == null || sd.isEmpty()) {
            sd = paciente.getServicioSolicitado();
        }
        if (sd != null) {
            sd = sd.toUpperCase();
            return switch (sd) {
                case "ENFERMERIA" -> "ENFERMERÍA";
                case "ODONTOLOGIA" -> "ODONTOLOGÍA";
                case "PSICOLOGIA" -> "PSICOLOGÍA";
                case "NUTRICION" -> "NUTRICIÓN";
                default -> sd;
            };
        }
        return "MEDICINA GENERAL";
    }

    private Triaje mapearDtoAEntidad(TriajeDto triajeDto, Paciente paciente, Usuario usuario) {
        Triaje triaje = new Triaje();
        triaje.setIdTriaje(triajeDto.getIdTriaje());
        triaje.setPaciente(paciente);
        triaje.setPresionArterialSistolica(triajeDto.getPresionArterialSistolica());
        triaje.setPresionArterialDiastolica(triajeDto.getPresionArterialDiastolica());
        triaje.setFrecuenciaCardiaca(triajeDto.getFrecuenciaCardiaca());
        triaje.setFrecuenciaRespiratoria(triajeDto.getFrecuenciaRespiratoria());
        triaje.setTemperatura(triajeDto.getTemperatura());
        triaje.setSaturacionOxigeno(triajeDto.getSaturacionOxigeno());
        triaje.setPesoKg(triajeDto.getPesoKg());
        triaje.setTallaCm(triajeDto.getTallaCm());
        triaje.setClasificacionUrgencia(triajeDto.getClasificacionUrgencia() != null ? triajeDto.getClasificacionUrgencia().toLowerCase() : null);
        triaje.setObservaciones(triajeDto.getObservaciones());
        triaje.setUsuario(usuario);
        triaje.setServicioDestino(normalizarServicioDestino(triajeDto.getServicioDestino(), paciente));
        return triaje;
    }

    private String manejarErroresValidacion(TriajeDto triajeDto, BindingResult bindingResult, org.springframework.ui.Model model) {
        String msg = bindingResult.getAllErrors().stream()
                .map(org.springframework.validation.ObjectError::getDefaultMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validación en los datos ingresados.");
        log.warn("Validación fallida en Triaje: {}", msg);
        
        if (triajeDto.getPaciente() != null && triajeDto.getPaciente().getIdPersona() != null) {
            Paciente p = pacienteService.buscarPorId(triajeDto.getPaciente().getIdPersona()).orElse(null);
            model.addAttribute("paciente", p);
            if (p != null) {
                model.addAttribute("edad", p.getEdadCompleta());
            }
        }
        model.addAttribute("errorObj", msg);
        return "clinico/triaje_registro";
    }

    @PostMapping("/guardar")
    public String guardarTriaje(@Valid @ModelAttribute("triaje") TriajeDto triajeDto, BindingResult bindingResult,
                                Authentication authentication, org.springframework.ui.Model model, RedirectAttributes redirectAttributes) {
        log.info(">>> [SIGECLIN] Iniciando registro de Triaje...");
        if (bindingResult.hasErrors()) {
            return manejarErroresValidacion(triajeDto, bindingResult, model);
        }

        try {
            if (triajeDto.getPaciente() == null || triajeDto.getPaciente().getIdPersona() == null) {
                throw new IllegalArgumentException("ID de Paciente no proporcionado");
            }

            Paciente paciente = pacienteService.buscarPorId(triajeDto.getPaciente().getIdPersona())
                    .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));

            Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            Triaje triaje = mapearDtoAEntidad(triajeDto, paciente, usuario);

            log.info(">>> [SIGECLIN] Guardando entidad Triaje...");
            triajeService.guardarTriaje(triaje);
            
            pacienteService.actualizarEstado(paciente.getIdPersona(), "PENDIENTE_CONSULTA");

            redirectAttributes.addFlashAttribute(FLASH_SUCCESS, "Triaje de " + paciente.getNombres() + " registrado correctamente. Derivado a: " + triaje.getServicioDestino());
            return REDIRECT_NUEVO;
        } catch (Exception e) {
            log.error(">>> [SIGECLIN] ERROR AL GUARDAR TRIAJE: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(FLASH_ERROR, "Error crítico al registrar el triaje: " + e.getMessage());
            return REDIRECT_NUEVO;
        }
    }

    @lombok.Getter
    @lombok.Setter
    public static class TriajeDto {
        private Integer idTriaje;
        private PacienteDto paciente;
        private Integer presionArterialSistolica;
        private Integer presionArterialDiastolica;
        private Integer frecuenciaCardiaca;
        private Integer frecuenciaRespiratoria;
        private java.math.BigDecimal temperatura;
        private Integer saturacionOxigeno;
        private java.math.BigDecimal pesoKg;
        private java.math.BigDecimal tallaCm;
        private String clasificacionUrgencia;
        private String servicioDestino;
        private String observaciones;
    }

    @lombok.Getter
    @lombok.Setter
    public static class PacienteDto {
        private Integer idPersona;
    }


    @GetMapping("/buscar")
    public String buscarPaciente(@RequestParam String query, Model model) {
        // Buscamos por DNI o por Número de HC
        Optional<Paciente> paciente = pacienteService.buscarPorDniOHC(query);
        if (paciente.isPresent()) {
            return "redirect:/triaje/registrar/" + paciente.get().getIdPersona();
        } else {
            model.addAttribute(FLASH_ERROR, "No se encontró ningún paciente con ese DNI o Historia Clínica.");
            return "clinico/triaje_busqueda";
        }
    }
}
