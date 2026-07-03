package com.sigeclin.reportes.controller;

import com.sigeclin.reportes.service.ReporteExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private final ReporteExcelService reporteExcelService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("fechaHoy", LocalDate.now());
        return "reportes/dashboard";
    }

    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "TODOS") String servicio,
            @RequestParam(defaultValue = "TODOS") String tipoPersonal,
            @RequestParam(defaultValue = "EXCEL") String formato) {

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        if (formato.equalsIgnoreCase("PDF")) {
            byte[] pdfContent = reporteExcelService.generarReportePdf(fechaInicio, fechaFin, servicio, tipoPersonal);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Reporte_SIGECLIN_" + LocalDate.now() + ".pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } else {
            byte[] excelContent = reporteExcelService.generarReporteAtenciones(fechaInicio, fechaFin, servicio, tipoPersonal);
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Reporte_SIGECLIN_" + LocalDate.now() + ".xlsx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        }
    }
}
