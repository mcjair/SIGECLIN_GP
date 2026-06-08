package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.service.IHistoriaClinicaService;
import com.sigeclin.clinico.service.IAuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/clinico/historia")
@RequiredArgsConstructor
public class HistoriaClinicaController {

    private final IHistoriaClinicaService historiaClinicaService;
    private final IAuditoriaService auditoriaService;

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
