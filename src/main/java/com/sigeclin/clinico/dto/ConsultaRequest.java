package com.sigeclin.clinico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ConsultaRequest {
    @NotNull(message = "El ID del triaje es obligatorio")
    private Integer triajeId;
    private String motivo;
    private String anamnesis;
    private String examenFisico;
    private String planTratamiento;
    private String proximoControl;
    private String tipoSalida;
    private List<Map<String, Object>> diagnosticos;
    private List<Map<String, Object>> medicamentos;
    private List<Map<String, Object>> examenes;
}
