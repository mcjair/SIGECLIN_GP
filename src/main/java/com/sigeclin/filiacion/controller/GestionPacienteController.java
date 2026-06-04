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

    @GetMapping("/lista")
    public String listarPacientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            org.springframework.security.core.Authentication authentication,
            Model model) {
        
        String rolFiltro = null;
        if (authentication != null && authentication.getAuthorities() != null) {
            String roleStr = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("ADMIN");
            
            if (!roleStr.equals("ADMIN")) {
                rolFiltro = roleStr;
                if (rolFiltro.equals("MEDICO_GENERAL")) rolFiltro = "MEDICINA GENERAL";
                if (rolFiltro.equals("ENFERMERIA")) rolFiltro = "ENFERMERÍA";
                if (rolFiltro.equals("ODONTOLOGIA")) rolFiltro = "ODONTOLOGÍA";
                if (rolFiltro.equals("PSICOLOGIA")) rolFiltro = "PSICOLOGÍA";
                if (rolFiltro.equals("NUTRICION")) rolFiltro = "NUTRICIÓN";
            }
        }

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

    @GetMapping("/export/excel")
    public void exportarAExcel(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=pacientes_sigeclin.xlsx");

        String rolFiltro = null;
        if (authentication != null && authentication.getAuthorities() != null) {
            String roleStr = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("ADMIN");
            
            if (!roleStr.equals("ADMIN")) {
                rolFiltro = roleStr;
                if (rolFiltro.equals("MEDICO_GENERAL")) rolFiltro = "MEDICINA GENERAL";
                if (rolFiltro.equals("ENFERMERIA")) rolFiltro = "ENFERMERÍA";
                if (rolFiltro.equals("ODONTOLOGIA")) rolFiltro = "ODONTOLOGÍA";
                if (rolFiltro.equals("PSICOLOGIA")) rolFiltro = "PSICOLOGÍA";
                if (rolFiltro.equals("NUTRICION")) rolFiltro = "NUTRICIÓN";
            }
        }

        java.util.List<com.sigeclin.filiacion.model.Paciente> pacientes = pacienteService.obtenerTodos(rolFiltro);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Pacientes");

            // Header Style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

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

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/api/historial/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerHistorial(@PathVariable Integer id) {
        List<Consulta> historial = consultaService.obtenerHistorialPaciente(id);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        List<Map<String, Object>> response = historial.stream().limit(7).map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("fecha", c.getFechaHoraInicio() != null ? c.getFechaHoraInicio().format(formatter) : "S/F");
            
            String servicio = c.getTriaje() != null && c.getTriaje().getServicioDestino() != null ? c.getTriaje().getServicioDestino() : "MÉDICO GENERAL";
            map.put("servicio", servicio);
            
            String medico = c.getMedico() != null ? c.getMedico().getNombres() + " " + c.getMedico().getApellidoPaterno() : "Asignado";
            map.put("medico", "Dr(a). " + medico);
            
            map.put("motivo", c.getMotivoConsulta() != null ? c.getMotivoConsulta() : "Sin descripción");
            map.put("anamnesis", c.getAnamnesis() != null ? c.getAnamnesis() : "---");
            map.put("examen", c.getExamenFisico() != null ? c.getExamenFisico() : "---");
            map.put("plan", c.getPlanTratamiento() != null ? c.getPlanTratamiento() : "---");
            map.put("proximoControl", c.getProximoControl() != null ? c.getProximoControl().toString() : "---");
            
            // Datos precisos: Diagnóstico
            String dx = "POR DEFINIR";
            if (c.getDiagnosticos() != null && !c.getDiagnosticos().isEmpty()) {
                dx = c.getDiagnosticos().stream()
                        .map(d -> d.getCie10().getCodigo() + " - " + d.getCie10().getDescripcion())
                        .collect(Collectors.joining("; "));
            }
            map.put("diagnostico", dx);
            
            // Datos precisos: Signos Vitales
            if (c.getTriaje() != null) {
                map.put("pa", (c.getTriaje().getPresionArterialSistolica() != null ? c.getTriaje().getPresionArterialSistolica() : "--") + "/" + 
                              (c.getTriaje().getPresionArterialDiastolica() != null ? c.getTriaje().getPresionArterialDiastolica() : "--"));
                map.put("temp", c.getTriaje().getTemperatura() != null ? c.getTriaje().getTemperatura().toString() : "--");
                map.put("fc", c.getTriaje().getFrecuenciaCardiaca() != null ? c.getTriaje().getFrecuenciaCardiaca().toString() : "--");
                map.put("sat", c.getTriaje().getSaturacionOxigeno() != null ? c.getTriaje().getSaturacionOxigeno().toString() : "--");
            } else {
                map.put("pa", "--/--");
                map.put("temp", "--");
                map.put("fc", "--");
                map.put("sat", "--");
            }
            
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
}
