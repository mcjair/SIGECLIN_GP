package com.sigeclin.maestras.controller;

import com.sigeclin.maestras.service.IMaestrasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final IMaestrasService maestrasService;

    @GetMapping
    public String listarServicios(Model model) {
        model.addAttribute("servicios", maestrasService.obtenerServiciosActivos());
        return "maestras/servicios";
    }
}
