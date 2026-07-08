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

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Controller
@RequestMapping("/personal")
public class PersonalController {

    private static final String FLASH_ERROR = "error";
    private static final String FLASH_SUCCESS = "success";
    private static final String REDIRECT_LISTA = "redirect:/personal/lista";

    private final IPersonalService personalService;
    private final JdbcTemplate jdbcTemplate;
    private final com.sigeclin.filiacion.repository.TipoDocumentoRepository tipoDocumentoRepository;

    @Autowired
    public PersonalController(IPersonalService personalService,
                              JdbcTemplate jdbcTemplate,
                              @Autowired(required = false) com.sigeclin.filiacion.repository.TipoDocumentoRepository tipoDocumentoRepository) {
        this.personalService = personalService;
        this.jdbcTemplate = jdbcTemplate;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.setAllowedFields("idPersona", "numeroColegiatura", "tipoPersonal", "especialidad", 
            "nombres", "apellidoPaterno", "apellidoMaterno", "sexo", "numeroDocumento", "fechaNacimiento");
    }

    @GetMapping("/lista")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO_GENERAL', 'ENFERMERIA')")
    public String listarPersonal(Model model) {
        List<Personal> personal = personalService.listarTodos();
        model.addAttribute("personalList", personal);
        
        List<Map<String, Object>> tipos = jdbcTemplate.queryForList("SELECT id_tipo_personal, descripcion FROM maestras.tipo_personal");
        model.addAttribute("tiposPersonal", tipos);
        
        Map<Integer, String> mapTipos = tipos.stream().collect(Collectors.toMap(
            row -> ((Number) row.get("id_tipo_personal")).intValue(),
            row -> (String) row.get("descripcion")
        ));
        model.addAttribute("mapTipos", mapTipos);
        
        return "filiacion/personal_lista";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarPersonal(@Valid @ModelAttribute("personal") PersonalDto personalDto, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute(FLASH_ERROR, "Datos invalidos: " + result.getAllErrors().get(0).getDefaultMessage());
            return REDIRECT_LISTA;
        }
        try {
            Personal personal = new Personal();
            personal.setIdPersona(personalDto.getIdPersona());
            if (personalDto.getTipoDocumento() != null && personalDto.getTipoDocumento().getIdTipoDocumento() != null) {
                com.sigeclin.filiacion.model.TipoDocumento td = tipoDocumentoRepository.findById(personalDto.getTipoDocumento().getIdTipoDocumento()).orElse(null);
                personal.setTipoDocumento(td);
            } else {
                com.sigeclin.filiacion.model.TipoDocumento td = tipoDocumentoRepository.findById(1).orElse(null);
                personal.setTipoDocumento(td);
            }
            personal.setNumeroDocumento(personalDto.getNumeroDocumento());
            personal.setNombres(personalDto.getNombres());
            personal.setApellidoPaterno(personalDto.getApellidoPaterno());
            personal.setApellidoMaterno(personalDto.getApellidoMaterno());
            personal.setFechaNacimiento(personalDto.getFechaNacimiento());
            personal.setSexo(personalDto.getSexo());
            personal.setTelefonoPrincipal(personalDto.getTelefonoPrincipal());
            personal.setCorreoElectronico(personalDto.getCorreoElectronico());
            personal.setDireccion(personalDto.getDireccion());
            personal.setIdTipoPersonal(personalDto.getIdTipoPersonal());
            personal.setIdEspecialidad(personalDto.getIdEspecialidad());
            personal.setNumeroColegiatura(personalDto.getNumeroColegiatura());
            personal.setFechaIngreso(personalDto.getFechaIngreso() != null ? personalDto.getFechaIngreso() : java.time.LocalDate.now(java.time.ZoneId.systemDefault()));
            personal.setFechaCese(personalDto.getFechaCese());
            if (personalDto.getEstadoLaboral() != null) {
                personal.setEstadoLaboral(personalDto.getEstadoLaboral());
            }

            if (personal.getNumeroColegiatura() != null && personal.getNumeroColegiatura().trim().isEmpty()) {
                personal.setNumeroColegiatura(null);
            }
            boolean isNew = personal.getIdPersona() == null;
            Personal saved = personalService.guardar(personal);
            
            if (isNew) {
                String generatedUser = personalService.generarUsuario(saved.getIdPersona());
                if (generatedUser != null) {
                    ra.addFlashAttribute(FLASH_SUCCESS, "Personal creado exitosamente. Credenciales de acceso generadas: Usuario: [" + generatedUser + "] | Clave: [Su N° de Documento (DNI)]");
                } else {
                    ra.addFlashAttribute(FLASH_SUCCESS, "Personal guardado correctamente.");
                }
            } else {
                ra.addFlashAttribute(FLASH_SUCCESS, "Personal guardado correctamente.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute(FLASH_ERROR, "Error al guardar: " + e.getMessage());
        }
        return REDIRECT_LISTA;
    }

    @PostMapping("/eliminar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String eliminarPersonal(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            personalService.eliminar(id);
            ra.addFlashAttribute(FLASH_SUCCESS, "Personal desactivado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute(FLASH_ERROR, "Error al eliminar: " + e.getMessage());
        }
        return REDIRECT_LISTA;
    }

    @PostMapping("/toggle-estado/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleEstado(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            personalService.toggleEstado(id);
            ra.addFlashAttribute(FLASH_SUCCESS, "Estado del personal actualizado.");
        } catch (Exception e) {
            ra.addFlashAttribute(FLASH_ERROR, "Error al cambiar estado: " + e.getMessage());
        }
        return REDIRECT_LISTA;
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO_GENERAL', 'ENFERMERIA')")
    public Personal obtenerPersonal(@PathVariable Integer id) {
        return personalService.buscarPorId(id);
    }

    @lombok.Getter
    @lombok.Setter
    public static class PersonalDto {
        private Integer idPersona;
        private TipoDocumentoDto tipoDocumento;
        private String numeroDocumento;
        private String nombres;
        private String apellidoPaterno;
        private String apellidoMaterno;
        private java.time.LocalDate fechaNacimiento;
        private String sexo;
        private String telefonoPrincipal;
        private String correoElectronico;
        private String direccion;
        private Integer idTipoPersonal;
        private Integer idEspecialidad;
        private String numeroColegiatura;
        private java.time.LocalDate fechaIngreso;
        private java.time.LocalDate fechaCese;
        private String estadoLaboral;
    }

    @lombok.Getter
    @lombok.Setter
    public static class TipoDocumentoDto {
        private Integer idTipoDocumento;
    }
}
