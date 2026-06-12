package com.sigeclin.clinico.service;

import org.springframework.ui.Model;
import java.util.List;
import java.util.Map;

public interface IApoyoDiagnosticoService {

    void cargarOrdenesLaboratorio(Model model);

    void cargarRecetasFarmacia(Model model);
}
