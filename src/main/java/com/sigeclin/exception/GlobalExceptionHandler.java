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

    private static final String ATTR_ERROR_STATUS = "errorStatus";
    private static final String ATTR_ERROR_MSG = "errorMsg";
    private static final String VIEW_ERROR_PAGE = "error_page";

    @ExceptionHandler(AlergiaActivaException.class)
    public String handleAlergiaActiva(AlergiaActivaException e, RedirectAttributes ra) {
        log.warn("Alergia activa detectada: {}", e.getMessage());
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:/triaje/nuevo";
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public String handleAccessDenied(org.springframework.security.access.AccessDeniedException e, Model model) {
        log.warn("Acceso denegado: {}", e.getMessage());
        model.addAttribute(ATTR_ERROR_STATUS, "403");
        model.addAttribute(ATTR_ERROR_MSG, "Acceso Restringido. Su perfil de usuario no cuenta con los privilegios necesarios para acceder a esta sección. Si considera que se trata de un error, contacte al administrador de sistemas.");
        return VIEW_ERROR_PAGE;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model) {
        log.warn("Argumento invalido: {}", e.getMessage());
        model.addAttribute(ATTR_ERROR_STATUS, "400");
        model.addAttribute(ATTR_ERROR_MSG, e.getMessage());
        return VIEW_ERROR_PAGE;
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntime(RuntimeException e, Model model) {
        log.error("Error inesperado: {}", e.getMessage(), e);
        model.addAttribute(ATTR_ERROR_STATUS, "500");
        model.addAttribute(ATTR_ERROR_MSG, "Servicio Temporalmente No Disponible. Hemos detectado un inconveniente interno al procesar su solicitud. Por favor, vuelva a intentarlo en unos instantes o póngase en contacto con soporte técnico.");
        return VIEW_ERROR_PAGE;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception e, Model model) {
        log.error("Error interno del servidor: {}", e.getMessage(), e);
        model.addAttribute(ATTR_ERROR_STATUS, "500");
        model.addAttribute(ATTR_ERROR_MSG, "Servicio Temporalmente No Disponible. Hemos detectado un inconveniente interno al procesar su solicitud. Por favor, vuelva a intentarlo en unos instantes o póngase en contacto con soporte técnico.");
        return VIEW_ERROR_PAGE;
    }
}
