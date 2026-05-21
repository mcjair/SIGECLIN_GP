package com.sigeclin.filiacion.controller;

import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/personal")
@RequiredArgsConstructor
public class PersonalController {

    private final PersonalRepository personalRepository;

    @GetMapping("/lista")
    public String listarPersonal(Model model) {
        List<Personal> personal = personalRepository.findAll();
        model.addAttribute("personalList", personal);
        return "filiacion/personal_lista";
    }
}
