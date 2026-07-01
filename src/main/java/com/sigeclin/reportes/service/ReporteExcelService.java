package com.sigeclin.reportes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteExcelService {

    private final JdbcTemplate jdbcTemplate;

    public byte[] generarReporteAtenciones(LocalDate fechaInicio, LocalDate fechaFin, String servicio, String tipoPersonal) {
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    c.id_consulta, " +
            "    TO_CHAR(c.fecha_hora_inicio, 'YYYY-MM-DD HH24:MI') as fecha_atencion, " +
            "    pac_per.numero_documento as dni_paciente, " +
            "    pac_per.nombres || ' ' || pac_per.apellido_paterno || ' ' || COALESCE(pac_per.apellido_materno, '') as paciente, " +
            "    med_per.nombres || ' ' || med_per.apellido_paterno as medico, " +
            "    esp.descripcion as servicio, " +
            "    COALESCE(tri_per.nombres || ' ' || tri_per.apellido_paterno, 'NO REGISTRADO') as personal_triaje, " +
            "    COALESCE(caj_per.nombres || ' ' || caj_per.apellido_paterno, 'NO REGISTRADO') as personal_caja, " +
            "    COALESCE(p.monto, 0) as ganancia, " +
            "    COALESCE(p.tipo_pago, 'NO REGISTRADO') as tipo_pago " +
            "FROM clinico.consulta c " +
            "JOIN filiacion.persona pac_per ON c.id_paciente = pac_per.id_persona " +
            "JOIN filiacion.persona med_per ON c.id_personal = med_per.id_persona " +
            "JOIN maestras.especialidad esp ON c.id_especialidad = esp.id_especialidad " +
            "LEFT JOIN clinico.triaje t ON c.id_triaje = t.id_triaje " +
            "LEFT JOIN filiacion.persona tri_per ON t.id_usuario = tri_per.id_persona " +
            "LEFT JOIN clinico.pago_log p ON p.id_paciente = c.id_paciente AND CAST(p.fecha_pago AS DATE) = CAST(c.fecha_hora_inicio AS DATE) " +
            "LEFT JOIN filiacion.persona caj_per ON p.id_usuario = caj_per.id_persona " +
            "WHERE CAST(c.fecha_hora_inicio AS DATE) BETWEEN ? AND ? "
        );

        if (servicio != null && !servicio.isEmpty() && !servicio.equals("TODOS")) {
            sql.append(" AND esp.descripcion = '").append(servicio).append("' ");
        }

        sql.append(" ORDER BY c.fecha_hora_inicio DESC");

        List<Map<String, Object>> resultados = jdbcTemplate.queryForList(
            sql.toString(), 
            java.sql.Date.valueOf(fechaInicio), 
            java.sql.Date.valueOf(fechaFin)
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte SIGECLIN");

            // ==========================================
            // DEFINICIÓN DE ESTILOS AVANZADOS
            // ==========================================
            
            // 1. Estilo Título Principal (Rojo Oscuro)
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.DARK_RED.getIndex());
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 2. Estilo Subtítulo (Cursiva Gris)
            CellStyle subtitleStyle = workbook.createCellStyle();
            Font subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleStyle.setFont(subtitleFont);
            
            // 3. Super Cabecera 1 (Gris claro - Datos Paciente)
            CellStyle superHeader1 = workbook.createCellStyle();
            superHeader1.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            superHeader1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            superHeader1.setAlignment(HorizontalAlignment.CENTER);
            superHeader1.setVerticalAlignment(VerticalAlignment.CENTER);
            superHeader1.setBorderTop(BorderStyle.THIN);
            superHeader1.setBorderBottom(BorderStyle.THIN);
            superHeader1.setBorderLeft(BorderStyle.THIN);
            superHeader1.setBorderRight(BorderStyle.THIN);
            Font sh1Font = workbook.createFont();
            sh1Font.setBold(true);
            sh1Font.setColor(IndexedColors.BLACK.getIndex());
            superHeader1.setFont(sh1Font);

            // 4. Super Cabecera 2 (Azul Minsa - Clínico)
            CellStyle superHeader2 = workbook.createCellStyle();
            superHeader2.setFillForegroundColor(IndexedColors.STEEL_BLUE.getIndex());
            superHeader2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            superHeader2.setAlignment(HorizontalAlignment.CENTER);
            superHeader2.setVerticalAlignment(VerticalAlignment.CENTER);
            superHeader2.setBorderTop(BorderStyle.THIN);
            superHeader2.setBorderBottom(BorderStyle.THIN);
            superHeader2.setBorderLeft(BorderStyle.THIN);
            superHeader2.setBorderRight(BorderStyle.THIN);
            Font sh2Font = workbook.createFont();
            sh2Font.setBold(true);
            sh2Font.setColor(IndexedColors.WHITE.getIndex());
            superHeader2.setFont(sh2Font);

            // 5. Super Cabecera 3 (Turquesa Oscuro - Finanzas)
            CellStyle superHeader3 = workbook.createCellStyle();
            superHeader3.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            superHeader3.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            superHeader3.setAlignment(HorizontalAlignment.CENTER);
            superHeader3.setVerticalAlignment(VerticalAlignment.CENTER);
            superHeader3.setBorderTop(BorderStyle.THIN);
            superHeader3.setBorderBottom(BorderStyle.THIN);
            superHeader3.setBorderLeft(BorderStyle.THIN);
            superHeader3.setBorderRight(BorderStyle.THIN);
            superHeader3.setFont(sh2Font); // White text

            // 6. Cabeceras Específicas (Fila 5)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            Font hFont = workbook.createFont();
            hFont.setBold(true);
            headerStyle.setFont(hFont);
            
            // 7. Celdas de Datos Normales
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 8. Celdas de Moneda
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(dataStyle);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("\"S/\"#,##0.00"));

            // ==========================================
            // CREACIÓN DE FILAS Y CONTENIDO
            // ==========================================

            // Fila 0: Título Principal
            Row rowTitle = sheet.createRow(0);
            rowTitle.setHeightInPoints(25);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("REPORTE GERENCIAL DE ATENCIONES Y FINANZAS - SIGECLIN");
            cellTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 9));

            // Fila 1: Subtítulo (Metadatos)
            Row rowSub = sheet.createRow(1);
            Cell cellSub = rowSub.createCell(0);
            String fechaGeneracion = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm:ss a"));
            cellSub.setCellValue("Generado el: " + fechaGeneracion + "  |  Registros: " + resultados.size());
            cellSub.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 9));

            // Fila 3: Super Cabeceras
            Row superHeaderRow = sheet.createRow(3);
            superHeaderRow.setHeightInPoints(25);
            
            // Bloque 1: Datos de Paciente (A-D -> 0-3)
            Cell shCell1 = superHeaderRow.createCell(0);
            shCell1.setCellValue("DATOS DEL PACIENTE Y ATENCIÓN");
            shCell1.setCellStyle(superHeader1);
            for(int i=1; i<=3; i++) { Cell c = superHeaderRow.createCell(i); c.setCellStyle(superHeader1); }
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, 3));

            // Bloque 2: Datos Clínicos (E-G -> 4-6)
            Cell shCell2 = superHeaderRow.createCell(4);
            shCell2.setCellValue("DATOS CLÍNICOS Y PERSONAL");
            shCell2.setCellStyle(superHeader2);
            for(int i=5; i<=6; i++) { Cell c = superHeaderRow.createCell(i); c.setCellStyle(superHeader2); }
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 4, 6));

            // Bloque 3: Datos Financieros (H-J -> 7-9)
            Cell shCell3 = superHeaderRow.createCell(7);
            shCell3.setCellValue("DATOS FINANCIEROS (CAJA)");
            shCell3.setCellStyle(superHeader3);
            for(int i=8; i<=9; i++) { Cell c = superHeaderRow.createCell(i); c.setCellStyle(superHeader3); }
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 7, 9));

            // Fila 4: Cabeceras Específicas
            String[] columns = {"ID Atención", "Fecha / Hora", "DNI Paciente", "Paciente", "Especialidad / Servicio", "Médico Tratante", "Personal Triaje", "Personal Caja", "Tipo Pago", "Monto (Ganancia)"};
            Row headerRow = sheet.createRow(4);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ==========================================
            // INYECCIÓN DE DATOS
            // ==========================================
            int rowIdx = 5;
            BigDecimal totalGanancias = BigDecimal.ZERO;

            for (Map<String, Object> rowMap : resultados) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0); c0.setCellValue(String.valueOf(rowMap.get("id_consulta"))); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(String.valueOf(rowMap.get("fecha_atencion"))); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue("DNI: " + String.valueOf(rowMap.get("dni_paciente"))); c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(String.valueOf(rowMap.get("paciente")).toUpperCase()); c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(String.valueOf(rowMap.get("servicio")).toUpperCase()); c4.setCellStyle(dataStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(String.valueOf(rowMap.get("medico")).toUpperCase()); c5.setCellStyle(dataStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(String.valueOf(rowMap.get("personal_triaje")).toUpperCase()); c6.setCellStyle(dataStyle);
                Cell c7 = row.createCell(7); c7.setCellValue(String.valueOf(rowMap.get("personal_caja")).toUpperCase()); c7.setCellStyle(dataStyle);
                Cell c8 = row.createCell(8); c8.setCellValue(String.valueOf(rowMap.get("tipo_pago")).toUpperCase()); c8.setCellStyle(dataStyle);
                
                Object rawMonto = rowMap.get("ganancia");
                BigDecimal monto = BigDecimal.ZERO;
                if (rawMonto instanceof BigDecimal) {
                    monto = (BigDecimal) rawMonto;
                } else if (rawMonto instanceof Number) {
                    monto = BigDecimal.valueOf(((Number) rawMonto).doubleValue());
                }
                
                Cell c9 = row.createCell(9);
                c9.setCellValue(monto.doubleValue());
                c9.setCellStyle(currencyStyle);
                
                totalGanancias = totalGanancias.add(monto);
            }

            // Fila de Totales Generales
            Row totalRow = sheet.createRow(rowIdx + 1);
            Cell totalLabelCell = totalRow.createCell(8);
            totalLabelCell.setCellValue("TOTAL RECAUDADO:");
            CellStyle totalLabelStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalLabelCell.setCellStyle(totalLabelStyle);

            Cell totalValueCell = totalRow.createCell(9);
            totalValueCell.setCellValue(totalGanancias.doubleValue());
            
            CellStyle totalCurrencyStyle = workbook.createCellStyle();
            totalCurrencyStyle.setDataFormat(format.getFormat("\"S/\"#,##0.00"));
            totalCurrencyStyle.setFont(totalFont);
            totalCurrencyStyle.setBorderTop(BorderStyle.DOUBLE);
            totalCurrencyStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalCurrencyStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            totalCurrencyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalValueCell.setCellStyle(totalCurrencyStyle);

            // Auto-ajustar todas las columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                // Darle un margen extra a las columnas
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando Excel de atenciones: ", e);
            throw new RuntimeException("Error al generar el reporte Excel");
        }
    }
}
