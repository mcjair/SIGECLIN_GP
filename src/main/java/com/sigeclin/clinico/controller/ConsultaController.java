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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.sigeclin.clinico.repository.TriajeRepository;
import com.sigeclin.clinico.repository.ConsultaRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/consulta")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MEDICO_GENERAL','ENFERMERIA','OBSTETRICIA','ODONTOLOGIA','PSICOLOGIA','NUTRICION','ADMIN')") // A01
public class ConsultaController {

    private final IConsultaService consultaService;
    private final AlergiaPacienteRepository alergiaRepository;
    private final IPacienteService pacienteService;
    private final TriajeRepository triajeRepository;
    private final ConsultaRepository consultaRepository;
    private final com.sigeclin.maestras.service.ICie10Service cie10Service;
    private final com.sigeclin.filiacion.repository.PersonalRepository personalRepository;
    private final com.sigeclin.filiacion.repository.UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // --- Endpoints de API (REST) ---

    @GetMapping("/api/cie10/search")
    @ResponseBody
    public List<com.sigeclin.maestras.model.Cie10> searchCie10(@RequestParam String q,
            @RequestParam(required = false) String servicio) {
        return cie10Service.search(q, servicio);
    }

    private Map<String, Object> construirMapMedico(com.sigeclin.clinico.model.Consulta c) {
        Map<String, Object> medico = new HashMap<>();
        if (c.getMedico() != null) {
            medico.put("nombres", c.getMedico().getNombres());
            medico.put("apellidoPaterno", c.getMedico().getApellidoPaterno());
            medico.put("apellidoMaterno", c.getMedico().getApellidoMaterno());
            medico.put("numeroColegiatura", c.getMedico().getNumeroColegiatura());
        }
        return medico;
    }

    private Map<String, Object> construirMapPaciente(com.sigeclin.clinico.model.Consulta c) {
        Map<String, Object> paciente = new HashMap<>();
        if (c.getPaciente() != null) {
            paciente.put("nombres", c.getPaciente().getNombres());
            paciente.put("apellidoPaterno", c.getPaciente().getApellidoPaterno());
            paciente.put("apellidoMaterno", c.getPaciente().getApellidoMaterno());
            paciente.put("numeroDocumento", c.getPaciente().getNumeroDocumento());
            paciente.put("numeroHistoriaClinica", c.getPaciente().getNumeroHistoriaClinica());
            paciente.put("sexo", c.getPaciente().getSexo());
            if (c.getPaciente().getFechaNacimiento() != null) {
                java.time.Period p = java.time.Period.between(c.getPaciente().getFechaNacimiento(), java.time.LocalDate.now(java.time.ZoneId.systemDefault()));
                paciente.put("edad", p.getYears() + " Años");
            }
        }
        return paciente;
    }

    private Map<String, Object> construirMapTriaje(com.sigeclin.clinico.model.Consulta c) {
        Map<String, Object> triaje = new HashMap<>();
        if (c.getTriaje() != null) {
            triaje.put("idTriaje", c.getTriaje().getIdTriaje());
            triaje.put("presionArterialSistolica", c.getTriaje().getPresionArterialSistolica());
            triaje.put("presionArterialDiastolica", c.getTriaje().getPresionArterialDiastolica());
            triaje.put("temperatura", c.getTriaje().getTemperatura());
            triaje.put("frecuenciaCardiaca", c.getTriaje().getFrecuenciaCardiaca());
            triaje.put("saturacionOxigeno", c.getTriaje().getSaturacionOxigeno());
            triaje.put("clasificacionUrgencia", c.getTriaje().getClasificacionUrgencia());
        }
        return triaje;
    }

    private List<Map<String, Object>> construirListaDiagnosticos(com.sigeclin.clinico.model.Consulta c) {
        List<Map<String, Object>> diags = new ArrayList<>();
        if (c.getDiagnosticos() != null) {
            c.getDiagnosticos().forEach(d -> {
                Map<String, Object> diag = new HashMap<>();
                Map<String, Object> cie10 = new HashMap<>();
                if (d.getCie10() != null) {
                    cie10.put("codigo", d.getCie10().getCodigo());
                    cie10.put("descripcion", d.getCie10().getDescripcion());
                }
                diag.put("cie10", cie10);
                diags.add(diag);
            });
        }
        return diags;
    }

    private List<Map<String, Object>> construirListaRecetas(com.sigeclin.clinico.model.Consulta c) {
        List<Map<String, Object>> recetas = new ArrayList<>();
        if (c.getRecetas() != null) {
            c.getRecetas().forEach(r -> {
                Map<String, Object> receta = new HashMap<>();
                List<Map<String, Object>> detalles = new ArrayList<>();
                if (r.getDetalles() != null) {
                    r.getDetalles().forEach(det -> {
                        Map<String, Object> detalle = new HashMap<>();
                        detalle.put("dosis", det.getDosis());
                        detalle.put("frecuencia", det.getFrecuencia());
                        detalle.put("duracionDias", det.getDuracionDias());
                        detalle.put("cantidad", det.getCantidadTotal());
                        detalle.put("cantidadTotal", det.getCantidadTotal());
                        Map<String, Object> med = new HashMap<>();
                        if (det.getMedicamento() != null) {
                            med.put("nombreGenerico", det.getMedicamento().getNombreGenerico());
                        }
                        detalle.put("medicamento", med);
                        detalles.add(detalle);
                    });
                }
                receta.put("detalles", detalles);
                recetas.add(receta);
            });
        }
        return recetas;
    }

    @GetMapping("/api/detalle/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Object> obtenerDetalleConsulta(@PathVariable Integer id) {
        try {
            return consultaRepository.findById(id)
                    .map(c -> {
                        Map<String, Object> resp = new HashMap<>();
                        resp.put("idConsulta", c.getIdConsulta());
                        resp.put("estado", c.getEstado());
                        resp.put("fechaHoraInicio", c.getFechaHoraInicio());
                        resp.put("motivoConsulta", c.getMotivoConsulta());
                        resp.put("anamnesis", c.getAnamnesis());
                        resp.put("examenFisico", c.getExamenFisico());
                        resp.put("planTratamiento", c.getPlanTratamiento());

                        resp.put("medico", construirMapMedico(c));
                        resp.put("paciente", construirMapPaciente(c));
                        resp.put("triaje", construirMapTriaje(c));
                        resp.put("diagnosticos", construirListaDiagnosticos(c));
                        resp.put("recetas", construirListaRecetas(c));

                        return ResponseEntity.ok((Object) resp);
                    })
                    .orElseGet(() -> ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // --- Endpoints de Navegación (MVC) ---

    @GetMapping("/modulo/{nombreModulo}")
    public String listarColaModulo(@PathVariable String nombreModulo, Model model) {
        try {
            // Normalizar el nombre del módulo para evitar problemas de tildes o mayúsculas
            // en la URL
            String moduloNormalizado = nombreModulo.toUpperCase();
            if (moduloNormalizado.equals("ENFERMERIA"))
                moduloNormalizado = "ENFERMERÍA";
            if (moduloNormalizado.equals("ODONTOLOGIA"))
                moduloNormalizado = "ODONTOLOGÍA";
            if (moduloNormalizado.equals("PSICOLOGIA"))
                moduloNormalizado = "PSICOLOGÍA";
            if (moduloNormalizado.equals("NUTRICION"))
                moduloNormalizado = "NUTRICIÓN";
            if (moduloNormalizado.equals("MEDICINA_GENERAL"))
                moduloNormalizado = "MEDICINA GENERAL";

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

    private void prepararAtributosMedico(com.sigeclin.filiacion.model.Personal medicoLogueado, Model model) {
        String medNombreCompleto = "Médico Tratante";
        String medCmp = "S/N";
        if (medicoLogueado != null) {
            StringBuilder sb = new StringBuilder("Dr(a). ").append(medicoLogueado.getNombres()).append(" ").append(medicoLogueado.getApellidoPaterno());
            if (medicoLogueado.getApellidoMaterno() != null) {
                sb.append(" ").append(medicoLogueado.getApellidoMaterno());
            }
            medNombreCompleto = sb.toString();
            if (medicoLogueado.getNumeroColegiatura() != null && !medicoLogueado.getNumeroColegiatura().isEmpty()) {
                medCmp = medicoLogueado.getNumeroColegiatura();
            }
        }
        model.addAttribute("medicoNombre", medNombreCompleto);
        model.addAttribute("medicoCmp", medCmp);
    }

    private void prepararAtributosJson(com.sigeclin.filiacion.model.Paciente paciente, Triaje triaje, 
                                        List<Consulta> historial, com.sigeclin.filiacion.model.Personal medicoLogueado, Model model) {
        try {
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

                List<AlergiaPaciente> alergias = alergiaRepository
                        .findByPacienteIdPersonaAndActivaTrue(paciente.getIdPersona());
                model.addAttribute("alergias", alergias != null ? alergias : new ArrayList<>());

                // Obtener médico logueado (Personal)
                String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication().getName();
                com.sigeclin.filiacion.model.Personal medicoLogueado = personalRepository.findByUsuarioUsername(username).orElse(null);
                model.addAttribute("medicoLogueado", medicoLogueado);

                prepararAtributosMedico(medicoLogueado, model);
                prepararAtributosJson(paciente, triaje, historial, medicoLogueado, model);
            }

            model.addAttribute("triaje", triaje);
            return "clinico/consulta_espera";
        } catch (Exception e) {
            log.error("Error en atenderPaciente: {}", e.getMessage(), e);
            return "redirect:/consulta/modulo/CONSULTA";
        }
    }

    private String calcularEdad(LocalDate fechaNac) {
        if (fechaNac == null)
            return "0 Años, 0 Meses, 0 Días";
        Period p = Period.between(fechaNac, LocalDate.now(java.time.ZoneId.systemDefault()));
        return p.getYears() + " Años, " + p.getMonths() + " Meses, " + p.getDays() + " Días";
    }

    @PostMapping("/guardar")
    @ResponseBody
    public ResponseEntity<ApiResponse<Integer>> guardarAtencion(@RequestBody ConsultaRequest request) {
        try {
            if (request.getTriajeId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("ID de triaje ausente."));
            }
            log.debug("Guardando atención para triajeId: {}", request.getTriajeId());

            Consulta saved = consultaService.guardarConsultaCompleta(request.getTriajeId(),
                    objectMapper.convertValue(request, Map.class));

            Triaje triaje = triajeRepository.findById(request.getTriajeId()).orElse(null);
            if (triaje != null && triaje.getPaciente() != null) {
                pacienteService.actualizarEstado(triaje.getPaciente().getIdPersona(), "ATENDIDO");
            }

            return ResponseEntity.ok(ApiResponse.ok("Atención registrada y finalizada correctamente.", saved.getIdConsulta()));
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
