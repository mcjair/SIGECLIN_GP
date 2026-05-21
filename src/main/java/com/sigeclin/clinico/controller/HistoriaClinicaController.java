package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.service.HistoriaClinicaService;
import com.sigeclin.clinico.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clinico/historia")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaClinicaService;
    private final AuditoriaService auditoriaService;

    @GetMapping("/{idPaciente}")
    public String verHistoriaClinica(@PathVariable Integer idPaciente, Model model) {
        Optional<Map<String, Object>> dataOpt = historiaClinicaService.obtenerHistoriaClinicaCompleta(idPaciente);
        if (dataOpt.isEmpty()) {
            return "redirect:/consulta/espera";
        }
        
        // Log auditing (RF56)
        auditoriaService.registrarAcceso("ACCESO_HISTORIA", "Acceso a historia clínica del paciente ID " + idPaciente, idPaciente);

        model.addAllAttributes(dataOpt.get());
        return "clinico/historia_3_columnas";
    }
}
