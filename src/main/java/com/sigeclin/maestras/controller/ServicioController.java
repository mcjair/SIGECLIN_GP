package com.sigeclin.maestras.controller;

import com.sigeclin.maestras.service.MaestrasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final MaestrasService maestrasService;

    @GetMapping
    public String listarServicios(Model model) {
        model.addAttribute("servicios", maestrasService.obtenerServiciosActivos());
        return "maestras/servicios";
    }
}
