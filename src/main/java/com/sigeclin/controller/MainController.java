package com.sigeclin.controller;

import com.sigeclin.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final IDashboardService dashboardService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(org.springframework.ui.Model model) {
        dashboardService.cargarDatosDashboard(model);
        return "dashboard";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public Map<String, Object> getDashboardStats() {
        return dashboardService.getDashboardStats();
    }

}
