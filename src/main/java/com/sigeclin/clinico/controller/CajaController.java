package com.sigeclin.clinico.controller;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.filiacion.service.IPacienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/caja")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CAJA', 'ADMIN')") // A01: Solo los roles ADMIN y CAJA gestionan caja
public class CajaController {

    private final IPacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/pago")
    public String mostrarPago(@RequestParam(required = false) String hc, 
                             @RequestParam(required = false) String servicio, 
                             Model model) {
        
        log.debug("Buscando paciente con HC/DNI: {} y Servicio: {}", hc, servicio);
        
        if (hc != null && !hc.isEmpty()) {
            Optional<Paciente> paciente = pacienteService.buscarPorDniOHC(hc);
            if (paciente.isPresent()) {
                Paciente p = paciente.get();
                model.addAttribute("paciente", p);
                if (servicio == null || servicio.isEmpty()) {
                    servicio = p.getServicioSolicitado();
                }
                log.debug("Paciente encontrado: {} - Servicio: {}", p.getNombres(), servicio);
            } else {
                log.debug("Paciente NO encontrado");
            }
        }
        
        if (servicio != null && !servicio.isEmpty()) {
            model.addAttribute("servicioSeleccionado", servicio);
        }
        
        // Cargar lista de pacientes recientes para facilitar el cobro manual
        model.addAttribute("pacientesPendientes", pacienteService.obtenerPacientesRecientes());
        
        return "clinico/caja_pago";
    }

    @GetMapping("/imprimir")
    public String imprimirVoucher(@RequestParam Integer idPaciente, 
                                 @RequestParam(required = false) String servicio, 
                                 @RequestParam(defaultValue = "50.00") java.math.BigDecimal monto,
                                 @RequestParam(defaultValue = "EFECTIVO") String tipoPago,
                                 Model model) {
        pacienteService.buscarPorId(idPaciente).ifPresent(p -> {
            model.addAttribute("paciente", p);
            model.addAttribute("servicio", servicio);
            model.addAttribute("monto", monto);
            model.addAttribute("tipoPago", tipoPago);
        });
        return "clinico/voucher_impresion";
    }

    private static final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    @PostMapping("/pagar")
    public String procesarPago(@RequestParam Integer idPaciente, 
                               @RequestParam String hc, 
                               @RequestParam(required = false) String servicio, 
                               @RequestParam(defaultValue = "50.00") java.math.BigDecimal monto,
                               @RequestParam(defaultValue = "EFECTIVO") String tipoPago,
                               @RequestParam(required = false) String concepto,
                               Authentication authentication,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        log.info("Procesando pago para Paciente ID: {} - HC: {}", idPaciente, hc);
        
        try {
            // Obtener el usuario autenticado
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Generar concepto y número de comprobante
            String conceptoFinal;
            if (concepto != null && !concepto.isEmpty()) {
                conceptoFinal = concepto;
            } else {
                String serv = (servicio != null) ? servicio : "Consulta";
                conceptoFinal = "Atención en " + serv;
            }

            String numeroComprobante = "C-" + LocalDate.now(java.time.ZoneId.systemDefault()).getYear() + "-" + String.format("%06d", secureRandom.nextInt(1000000));

            // Insertar el registro de pago en pago_log
            jdbcTemplate.update(
                "INSERT INTO clinico.pago_log (id_paciente, id_usuario, monto, tipo_pago, concepto, numero_comprobante) VALUES (?, ?, ?, ?, ?, ?)",
                idPaciente, usuario.getIdPersona(), monto, tipoPago.toUpperCase(), conceptoFinal, numeroComprobante
            );
            
            // Cambiar estado del paciente para que aparezca en Triaje
            pacienteService.actualizarEstado(idPaciente, "PENDIENTE_TRIAJE");
            
            redirectAttributes.addFlashAttribute("success", "Pago procesado correctamente. El paciente ha sido derivado a Triaje.");
        } catch (Exception e) {
            log.error("Error al procesar el pago para paciente ID {}: {}", idPaciente, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al procesar el pago: " + e.getMessage());
        }
        
        return "redirect:/caja/pago"; 
    }
}
