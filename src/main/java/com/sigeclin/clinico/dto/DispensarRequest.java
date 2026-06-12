package com.sigeclin.clinico.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DispensarRequest {
    @NotNull
    private Integer idDetalleReceta;

    @NotNull
    private Integer idLote;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    private String observaciones;
}
