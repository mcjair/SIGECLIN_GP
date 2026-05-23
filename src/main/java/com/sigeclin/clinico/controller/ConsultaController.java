package com.sigeclin.clinico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.clinico.dto.ApiResponse;
import com.sigeclin.clinico.dto.ConsultaRequest;
import com.sigeclin.clinico.repository.AlergiaPacienteRepository;
import com.sigeclin.clinico.service.IConsultaService;
import com.sigeclin.filiacion.service.IPacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.sigeclin.clinico.repository.TriajeRepository;
import com.sigeclin.clinico.repository.ConsultaRepository;
import org.springframework.http.ResponseEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/consulta")
@RequiredArgsConstructor
public class ConsultaController {

    private final IConsultaService consultaService;
    private final AlergiaPacienteRepository alergiaRepository;
    private final IPacienteService pacienteService;
    private final TriajeRepository triajeRepository;
    private final ConsultaRepository consultaRepository;
    private final com.sigeclin.maestras.service.ICie10Service cie10Service;
    private final com.sigeclin.filiacion.repository.PersonalRepository personalRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS);
    
    // --- Endpoints de API (REST) ---

    @GetMapping("/api/cie10/search")
    @ResponseBody
    public List<com.sigeclin.maestras.model.Cie10> searchCie10(@RequestParam String q, @RequestParam(required = false) String servicio) {
        return cie10Service.search(q, servicio);
    }

    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerDetalleConsulta(@PathVariable Integer id) {
        try {
            return consultaRepository.findById(id)
                    .map(c -> {
                        // Forzar carga de colecciones para la serialización
                        if (c.getDiagnosticos() != null) c.getDiagnosticos().size();
                        if (c.getRecetas() != null) {
                            c.getRecetas().forEach(r -> {
                                if (r.getDetalles() != null) r.getDetalles().size();
                            });
                        }
                        // Forzar carga de relaciones LAZY
                        if (c.getMedico() != null) c.getMedico().getNombres();
                        if (c.getPaciente() != null) c.getPaciente().getNombres();
                        if (c.getTriaje() != null) c.getTriaje().getPresionArterialSistolica();
                        
                        return ResponseEntity.ok(c);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // --- Endpoints de Navegación (MVC) ---

    @GetMapping("/modulo/{nombreModulo}")
    public String listarColaModulo(@PathVariable String nombreModulo, Model model) {
        try {
            // Normalizar el nombre del módulo para evitar problemas de tildes o mayúsculas en la URL
            String moduloNormalizado = nombreModulo.toUpperCase();
            if (moduloNormalizado.equals("ENFERMERIA")) moduloNormalizado = "ENFERMERÍA";
            if (moduloNormalizado.equals("ODONTOLOGIA")) moduloNormalizado = "ODONTOLOGÍA";
            if (moduloNormalizado.equals("PSICOLOGIA")) moduloNormalizado = "PSICOLOGÍA";
            if (moduloNormalizado.equals("NUTRICION")) moduloNormalizado = "NUTRICIÓN";
            if (moduloNormalizado.equals("MEDICINA_GENERAL")) moduloNormalizado = "MEDICINA GENERAL";

            model.addAttribute("modulo", moduloNormalizado);
            model.addAttribute("moduloJson", moduloNormalizado);
            // Obtener triajes que están en espera
            List<Triaje> triajes = consultaService.obtenerPacientesEnEsperaPorModulo(moduloNormalizado);
            model.addAttribute("pacientes", triajes != null ? triajes : new ArrayList<>());
            return "clinico/consulta_cola";
        } catch (Exception e) {
            log.error("Error al cargar cola del módulo: {}", e.getMessage(), e);
            return "redirect:/dashboard";
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/atender/{idTriaje}")
    public String atenderPaciente(@PathVariable Integer idTriaje, Model model) {
        try {
            Triaje triaje = triajeRepository.findById(idTriaje)
                    .orElseThrow(() -> new RuntimeException("Triaje no encontrado con ID: " + idTriaje));
            
            com.sigeclin.filiacion.model.Paciente paciente = triaje.getPaciente();
            if (paciente != null) {
                // Forzar carga de datos (Hibernate lazy load bypass si es necesario)
                paciente.getNombres();
                
                model.addAttribute("pacienteSeleccionado", paciente);
                model.addAttribute("paciente", paciente);
                model.addAttribute("edad", calcularEdad(paciente.getFechaNacimiento()));
                
                List<Consulta> historial = consultaService.obtenerHistorialPaciente(paciente.getIdPersona());
                model.addAttribute("historial", historial != null ? historial : new ArrayList<>());
                
                List<AlergiaPaciente> alergias = alergiaRepository.findByPacienteIdPersonaAndActivaTrue(paciente.getIdPersona());
                model.addAttribute("alergias", alergias != null ? alergias : new ArrayList<>());

                // Obtener médico logueado (Personal)
                String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
                com.sigeclin.filiacion.model.Personal medicoLogueado = personalRepository.findByUsuarioUsername(username).orElse(null);
                model.addAttribute("medicoLogueado", medicoLogueado);

                try {
                    // Serialización segura para JS
                    model.addAttribute("historialJson", objectMapper.writeValueAsString(historial != null ? historial : new ArrayList<>()));
                    model.addAttribute("pacienteJson", objectMapper.writeValueAsString(paciente));
                    model.addAttribute("triajeJson", objectMapper.writeValueAsString(triaje));
                    model.addAttribute("medicoJson", objectMapper.writeValueAsString(medicoLogueado));
                } catch (Exception e) {
                    log.error("Error serializando datos: {}", e.getMessage());
                    model.addAttribute("historialJson", "[]");
                    model.addAttribute("pacienteJson", "{}");
                    model.addAttribute("triajeJson", "{}");
                    model.addAttribute("medicoJson", "null");
                }
            }
            
            model.addAttribute("triaje", triaje);
            return "clinico/consulta_espera";
        } catch (Exception e) {
            log.error("Error en atenderPaciente: {}", e.getMessage(), e);
            return "redirect:/consulta/modulo/CONSULTA";
        }
    }

    private String calcularEdad(LocalDate fechaNac) {
        if (fechaNac == null) return "0 Años, 0 Meses, 0 Días";
        Period p = Period.between(fechaNac, LocalDate.now());
        return p.getYears() + " Años, " + p.getMonths() + " Meses, " + p.getDays() + " Días";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> guardarAtencion(@RequestBody ConsultaRequest request) {
        try {
            if (request.getTriajeId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("ID de triaje ausente."));
            }
            log.debug("Guardando atención para triajeId: {}", request.getTriajeId());

            consultaService.guardarConsultaCompleta(request.getTriajeId(), objectMapper.convertValue(request, Map.class));

            Triaje triaje = triajeRepository.findById(request.getTriajeId()).orElse(null);
            if (triaje != null && triaje.getPaciente() != null) {
                pacienteService.actualizarEstado(triaje.getPaciente().getIdPersona(), "ATENDIDO");
            }

            return ResponseEntity.ok(ApiResponse.ok("Atención registrada y finalizada correctamente."));
        } catch (Exception e) {
            log.error("Error al guardar atención: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/receta/preview")
    public String previewReceta() {
        return "clinico/receta_impresion";
    }

    @GetMapping("/referencia/preview")
    public String previewReferencia() {
        return "clinico/referencia_impresion";
    }
    @GetMapping("/certificado/preview")
    public String previewCertificado() {
        return "clinico/certificado_medico";
    }
}
