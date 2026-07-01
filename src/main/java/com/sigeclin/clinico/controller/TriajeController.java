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
public class TriajeController {

    private final ITriajeService triajeService;
    private final IPacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;

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

    @Transactional(readOnly = true)
    @GetMapping("/registrar/{idPaciente}")
    public String mostrarFormulario(@PathVariable Integer idPaciente, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Triaje - Cargando formulario para Paciente ID: {}", idPaciente);
        
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorId(idPaciente);
        if (pacienteOpt.isEmpty()) {
            log.warn("Triaje - Paciente NO encontrado: {}", idPaciente);
            redirectAttributes.addFlashAttribute("error", "Paciente no encontrado.");
            return "redirect:/triaje/nuevo";
        }

        Paciente p = pacienteOpt.get();
        // Forzar carga de datos básicos para evitar LazyInitializationException en la vista
        String dummy = p.getNombres(); 
        dummy = p.getApellidoPaterno();
        
        Triaje triaje = new Triaje();
        triaje.setPaciente(p);
        
        model.addAttribute("triaje", triaje);
        model.addAttribute("paciente", p);
        model.addAttribute("edad", p.getEdadCompleta());
        
        log.debug("Triaje - Formulario cargado con éxito para: {}", p.getNombres());
        return "clinico/triaje_registro";
    }

    @PostMapping("/guardar")
    public String guardarTriaje(@Valid @ModelAttribute Triaje triaje, BindingResult bindingResult,
                                Authentication authentication, org.springframework.ui.Model model, RedirectAttributes redirectAttributes) {
        log.info(">>> [SIGECLIN] Iniciando registro de Triaje...");
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Error de validación en los datos ingresados.");
            log.warn("Validación fallida en Triaje: {}", msg);
            
            // UX Perfection: Retenemos los datos del formulario (Estado)
            if (triaje.getPaciente() != null && triaje.getPaciente().getIdPersona() != null) {
                Paciente p = pacienteService.buscarPorId(triaje.getPaciente().getIdPersona()).orElse(triaje.getPaciente());
                model.addAttribute("paciente", p);
                model.addAttribute("edad", p.getEdadCompleta());
            }
            model.addAttribute("errorObj", msg);
            return "clinico/triaje_registro";
        }

        try {
            // Validar existencia de paciente
            if (triaje.getPaciente() == null || triaje.getPaciente().getIdPersona() == null) {
                throw new RuntimeException("ID de Paciente no proporcionado");
            }

            // Cargar paciente completo para asegurar consistencia y evitar LazyInit
            Paciente paciente = pacienteService.buscarPorId(triaje.getPaciente().getIdPersona())
                    .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
            triaje.setPaciente(paciente);

            // Asignar el usuario actual (quien realiza el triaje)
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            triaje.setUsuario(usuario);

            // Normalizar y validar el servicio de destino
            String sd = triaje.getServicioDestino();
            if (sd == null || sd.isEmpty()) {
                sd = paciente.getServicioSolicitado();
            }

            // Normalización final
            if (sd != null) {
                sd = sd.toUpperCase();
                if (sd.equals("ENFERMERIA")) sd = "ENFERMERÍA";
                if (sd.equals("ODONTOLOGIA")) sd = "ODONTOLOGÍA";
                if (sd.equals("PSICOLOGIA")) sd = "PSICOLOGÍA";
                if (sd.equals("NUTRICION")) sd = "NUTRICIÓN";
                triaje.setServicioDestino(sd);
            } else {
                triaje.setServicioDestino("MEDICINA GENERAL");
            }

            // Asegurar que la clasificación de urgencia esté en minúsculas (por el check constraint de la DB)
            if (triaje.getClasificacionUrgencia() != null) {
                triaje.setClasificacionUrgencia(triaje.getClasificacionUrgencia().toLowerCase());
            }

            // El servicio se encargará de evaluar y setear automáticamente las alertas clínicas (SRP)

            log.info(">>> [SIGECLIN] Guardando entidad Triaje...");
            triajeService.guardarTriaje(triaje);
            
            // Marcar paciente como listo para consulta
            pacienteService.actualizarEstado(paciente.getIdPersona(), "PENDIENTE_CONSULTA");

            redirectAttributes.addFlashAttribute("success", "Triaje de " + paciente.getNombres() + " registrado correctamente. Derivado a: " + triaje.getServicioDestino());
            return "redirect:/triaje/nuevo";
        } catch (Exception e) {
            log.error(">>> [SIGECLIN] ERROR AL GUARDAR TRIAJE: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error crítico al registrar el triaje: " + e.getMessage());
            return "redirect:/triaje/nuevo";
        }
    }

    @GetMapping("/buscar")
    public String buscarPaciente(@RequestParam String query, Model model) {
        // Buscamos por DNI o por Número de HC
        Optional<Paciente> paciente = pacienteService.buscarPorDniOHC(query);
        if (paciente.isPresent()) {
            return "redirect:/triaje/registrar/" + paciente.get().getIdPersona();
        } else {
            model.addAttribute("error", "No se encontró ningún paciente con ese DNI o Historia Clínica.");
            return "clinico/triaje_busqueda";
        }
    }
}
