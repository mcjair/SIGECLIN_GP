package com.sigeclin.clinico.controller;

import com.sigeclin.clinico.service.IApoyoDiagnosticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/apoyo")
@RequiredArgsConstructor
public class ApoyoDiagnosticoController {

    private final IApoyoDiagnosticoService apoyoDiagnosticoService;

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
}
