package com.sigeclin.clinico.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LoteRequest {
    @NotNull
    private Integer idMedicamento;

    @NotBlank
    private String numeroLote;

    @NotNull
    private LocalDate fechaVencimiento;

    @NotNull @Min(1)
    private Integer cantidadInicial;
}
