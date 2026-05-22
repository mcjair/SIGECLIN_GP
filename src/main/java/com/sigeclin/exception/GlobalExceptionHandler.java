package com.sigeclin.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AlergiaActivaException.class)
    public String handleAlergiaActiva(AlergiaActivaException e, RedirectAttributes ra) {
        log.warn("Alergia activa detectada: {}", e.getMessage());
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:/triaje/nuevo";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, RedirectAttributes ra) {
        log.warn("Argumento invalido: {}", e.getMessage());
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:/dashboard";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntime(RuntimeException e, RedirectAttributes ra) {
        log.error("Error inesperado: {}", e.getMessage(), e);
        ra.addFlashAttribute("error", "Ocurrio un error inesperado: " + e.getMessage());
        return "redirect:/dashboard";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception e, Model model) {
        log.error("Error interno del servidor: {}", e.getMessage(), e);
        model.addAttribute("error", "Error interno del servidor. Contacte al administrador.");
        return "error";
    }
}
