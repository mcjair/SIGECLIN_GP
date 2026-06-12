package com.sigeclin.filiacion.controller;

import com.sigeclin.filiacion.model.Personal;
import com.sigeclin.filiacion.service.IPersonalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/personal")
@RequiredArgsConstructor
public class PersonalController {

    private final IPersonalService personalService;

    @GetMapping("/lista")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO_GENERAL', 'ENFERMERIA')")
    public String listarPersonal(Model model) {
        List<Personal> personal = personalService.listarTodos();
        model.addAttribute("personalList", personal);
        return "filiacion/personal_lista";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarPersonal(@Valid @ModelAttribute Personal personal, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Datos invalidos: " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/personal/lista";
        }
        try {
            personalService.guardar(personal);
            ra.addFlashAttribute("success", "Personal guardado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/personal/lista";
    }

    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarPersonal(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            personalService.eliminar(id);
            ra.addFlashAttribute("success", "Personal desactivado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/personal/lista";
    }

    @PostMapping("/toggle-estado/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            personalService.toggleEstado(id);
            ra.addFlashAttribute("success", "Estado del personal actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al cambiar estado: " + e.getMessage());
        }
        return "redirect:/personal/lista";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO_GENERAL', 'ENFERMERIA')")
    public Personal obtenerPersonal(@PathVariable Integer id) {
        return personalService.buscarPorId(id);
    }
}
