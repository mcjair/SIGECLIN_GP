package com.sigeclin.clinico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sigeclin.clinico.model.Consulta;
import com.sigeclin.clinico.model.Triaje;
import com.sigeclin.clinico.model.AlergiaPaciente;
import com.sigeclin.clinico.repository.AlergiaRepository;
import com.sigeclin.clinico.service.ConsultaService;
import com.sigeclin.filiacion.service.PacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ConsultaService consultaService;
    private final AlergiaRepository alergiaRepository;
    private final PacienteService pacienteService;
    private final TriajeRepository triajeRepository;
    private final ConsultaRepository consultaRepository;
    private final com.sigeclin.maestras.service.Cie10Service cie10Service;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS);
    
    // --- Endpoints de API (REST) ---

    @GetMapping("/api/cie10/search")
    @ResponseBody
    public List<com.sigeclin.maestras.model.Cie10> searchCie10(@RequestParam String q) {
        return cie10Service.search(q);
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
            // Obtener triajes que están en espera
            List<Triaje> triajes = consultaService.obtenerPacientesEnEsperaPorModulo(moduloNormalizado);
            model.addAttribute("pacientes", triajes != null ? triajes : new ArrayList<>());
            return "clinico/consulta_cola";
        } catch (Exception e) {
            e.printStackTrace();
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

                try {
                    // Serialización segura para JS
                    model.addAttribute("historialJson", objectMapper.writeValueAsString(historial != null ? historial : new ArrayList<>()));
                    model.addAttribute("pacienteJson", objectMapper.writeValueAsString(paciente));
                    model.addAttribute("triajeJson", objectMapper.writeValueAsString(triaje));
                } catch (Exception e) {
                    log.error("Error serializando datos: {}", e.getMessage());
                    model.addAttribute("historialJson", "[]");
                    model.addAttribute("pacienteJson", "{}");
                    model.addAttribute("triajeJson", "{}");
                }
            }
            
            model.addAttribute("triaje", triaje);
            return "clinico/consulta_espera";
        } catch (Exception e) {
            System.err.println("Error en atenderPaciente: " + e.getMessage());
            e.printStackTrace();
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
    public Map<String, Object> guardarAtencion(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("Guardando atención: " + payload);
            
            Object triajeIdObj = payload.get("triajeId");
            if (triajeIdObj == null) throw new RuntimeException("ID de triaje ausente.");
            
            Integer triajeId = Integer.parseInt(triajeIdObj.toString());
            
            // GUARDAR LA ATENCIÓN REAL EN BD
            consultaService.guardarConsultaCompleta(triajeId, payload);
            
            // ACTUALIZAR ESTADO DEL PACIENTE A 'ATENDIDO'
            Triaje triaje = triajeRepository.findById(triajeId).orElse(null);
            if (triaje != null && triaje.getPaciente() != null) {
                pacienteService.actualizarEstado(triaje.getPaciente().getIdPersona(), "ATENDIDO");
            }
            
            response.put("success", true);
            response.put("message", "Atención registrada y finalizada correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        return response;
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
