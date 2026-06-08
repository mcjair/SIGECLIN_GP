package com.sigeclin.service;

import org.springframework.ui.Model;
import java.util.Map;

public interface IDashboardService {

    void cargarDatosDashboard(Model model);

    Map<String, Object> getDashboardStats();
}
