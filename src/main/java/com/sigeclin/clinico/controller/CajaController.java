package com.sigeclin.clinico.controller;

import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/caja")
@RequiredArgsConstructor
public class CajaController {

    private final PacienteService pacienteService;

    @GetMapping("/pago")
    public String mostrarPago(@RequestParam(required = false) String hc, 
                             @RequestParam(required = false) String servicio, 
                             Model model) {
        
        System.out.println("Caja - Buscando paciente con HC/DNI: " + hc + " y Servicio: " + servicio);
        
        if (hc != null && !hc.isEmpty()) {
            Optional<Paciente> paciente = pacienteService.buscarPorDniOHC(hc);
            if (paciente.isPresent()) {
                Paciente p = paciente.get();
                model.addAttribute("paciente", p);
                // Si el servicio no viene en el param, usar el guardado en el paciente
                if (servicio == null || servicio.isEmpty()) {
                    servicio = p.getServicioSolicitado();
                }
                System.out.println("Caja - Paciente encontrado: " + p.getNombres() + " - Servicio: " + servicio);
            } else {
                System.out.println("Caja - Paciente NO encontrado");
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
                                 Model model) {
        pacienteService.buscarPorId(idPaciente).ifPresent(p -> {
            model.addAttribute("paciente", p);
            model.addAttribute("servicio", servicio);
        });
        return "clinico/voucher_impresion";
    }

    @PostMapping("/pagar")
    public String procesarPago(@RequestParam Integer idPaciente, 
                               @RequestParam String hc, 
                               @RequestParam(required = false) String servicio, 
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        System.out.println("Caja - Procesando pago para Paciente ID: " + idPaciente + " - HC: " + hc);
        
        // Cambiar estado del paciente para que aparezca en Triaje
        pacienteService.actualizarEstado(idPaciente, "PENDIENTE_TRIAJE");
        
        redirectAttributes.addFlashAttribute("success", "Pago procesado correctamente. El paciente ha sido derivado a Triaje.");
        
        return "redirect:/caja/pago"; 
    }
}
