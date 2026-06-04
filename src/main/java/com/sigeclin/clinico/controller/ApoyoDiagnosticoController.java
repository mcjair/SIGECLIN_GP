package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.model.OrdenMedica;
import com.sigeclin.clinico.repository.OrdenMedicaRepository;
import com.sigeclin.clinico.service.IApoyoDiagnosticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/apoyo")
@RequiredArgsConstructor
public class ApoyoDiagnosticoController {

    private final IApoyoDiagnosticoService apoyoDiagnosticoService;
    private final OrdenMedicaRepository ordenMedicaRepository;

    @GetMapping("/laboratorio")
    public String laboratorio(Model model) {
        apoyoDiagnosticoService.cargarOrdenesLaboratorio(model);
        return "clinico/laboratorio_lista";
    }

    @GetMapping("/farmacia")
    public String farmacia(Model model) {
        apoyoDiagnosticoService.cargarRecetasFarmacia(model);
        return "clinico/farmacia_lista";
    }

    @GetMapping("/farmacia/catalogo")
    public String farmaciaCatalogo(Model model) {
        return "clinico/farmacia_catalogo";
    }

    @GetMapping("/laboratorio/informe/{idOrden}")
    public String informeLaboratorio(@PathVariable Integer idOrden, Model model) {
        ordenMedicaRepository.findById(idOrden).ifPresent(o -> model.addAttribute("orden", o));
        return "clinico/informe_laboratorio";
    }
}
