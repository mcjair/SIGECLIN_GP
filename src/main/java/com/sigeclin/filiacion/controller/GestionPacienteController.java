package com.sigeclin.filiacion.controller;

import com.sigeclin.filiacion.service.IPacienteService;
import com.sigeclin.clinico.service.IConsultaService;
import com.sigeclin.clinico.model.Consulta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

@Slf4j
@Controller
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class GestionPacienteController {

    private final IPacienteService pacienteService;
    private final IConsultaService consultaService;

    private String resolverFiltroRol(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }
        String roleStr = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("ADMIN");
        
        if ("ADMIN".equals(roleStr)) {
            return null;
        }

        return switch (roleStr) {
            case "MEDICO_GENERAL" -> "MEDICINA GENERAL";
            case "ENFERMERIA" -> "ENFERMERÍA";
            case "ODONTOLOGIA" -> "ODONTOLOGÍA";
            case "PSICOLOGIA" -> "PSICOLOGÍA";
            case "NUTRICION" -> "NUTRICIÓN";
            default -> roleStr;
        };
    }

    @GetMapping("/lista")
    public String listarPacientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            org.springframework.security.core.Authentication authentication,
            Model model) {
        
        String rolFiltro = resolverFiltroRol(authentication);

        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        Page<com.sigeclin.filiacion.model.Paciente> pacientesPage = pacienteService.obtenerTodosPaginado(search, rolFiltro, pageable);
        
        model.addAttribute("pacientesPage", pacientesPage);
        model.addAttribute("pacientes", pacientesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pacientesPage.getTotalPages());
        model.addAttribute("totalItems", pacientesPage.getTotalElements());
        model.addAttribute("search", search != null ? search : "");
        
        return "filiacion/pacientes_lista";
    }

    private org.apache.poi.ss.usermodel.CellStyle crearEstiloCabecera(org.apache.poi.ss.usermodel.Workbook workbook) {
        org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.INDIGO.getIndex());
        headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        return headerStyle;
    }

    private void escribirFilaPaciente(org.apache.poi.ss.usermodel.Row row, com.sigeclin.filiacion.model.Paciente p) {
        row.createCell(0).setCellValue(p.getNumeroHistoriaClinica() != null ? p.getNumeroHistoriaClinica() : "S/H");
        row.createCell(1).setCellValue(p.getNumeroDocumento() != null ? p.getNumeroDocumento() : "---");
        row.createCell(2).setCellValue(p.getApellidoPaterno() != null ? p.getApellidoPaterno() : "");
        row.createCell(3).setCellValue(p.getApellidoMaterno() != null ? p.getApellidoMaterno() : "");
        row.createCell(4).setCellValue(p.getNombres() != null ? p.getNombres() : "");
        row.createCell(5).setCellValue(p.getEdadCompleta() != null ? p.getEdadCompleta() : "---");
        row.createCell(6).setCellValue(p.getSexo() != null ? p.getSexo() : "");
        row.createCell(7).setCellValue(p.getTelefonoPrincipal() != null ? p.getTelefonoPrincipal() : "");
        row.createCell(8).setCellValue(p.getCorreoElectronico() != null ? p.getCorreoElectronico() : "");
    }

    @GetMapping("/export/excel")
    public void exportarAExcel(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=pacientes_sigeclin.xlsx");

        String rolFiltro = resolverFiltroRol(authentication);
        java.util.List<com.sigeclin.filiacion.model.Paciente> pacientes = pacienteService.obtenerTodos(rolFiltro);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Pacientes");
            org.apache.poi.ss.usermodel.CellStyle headerStyle = crearEstiloCabecera(workbook);

            // Headers
            String[] headers = {"HC", "DNI / N° Doc", "Apellido Paterno", "Apellido Materno", "Nombres", "Edad", "Sexo", "Teléfono", "Correo Electrónico"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (com.sigeclin.filiacion.model.Paciente p : pacientes) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                escribirFilaPaciente(row, p);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/api/historial/{id}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorial(@PathVariable Integer id, org.springframework.security.core.Authentication authentication) {
        String rolFiltro = resolverFiltroRol(authentication);

        List<Map<String, Object>> response = consultaService.obtenerHistorialPacienteDto(id, rolFiltro);
        return ResponseEntity.ok(response);
    }
}
