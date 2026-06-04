package com.sigeclin.clinico.model;

import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.maestras.model.Medicamento;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lote_medicamento", schema = "clinico", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_medicamento", "numero_lote"})
})
public class LoteMedicamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idLote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medicamento", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private String numeroLote;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    private Integer stockInicial;

    @Column(nullable = false)
    private Integer stockActual;

    private LocalDateTime fechaIngreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaIngreso == null) fechaIngreso = LocalDateTime.now();
    }
}
