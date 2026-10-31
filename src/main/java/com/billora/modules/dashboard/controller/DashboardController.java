package com.billora.modules.dashboard.controller;

import com.billora.modules.dashboard.dto.DashboardSummaryDto;
import com.billora.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.currency-symbol:$}")
    private String currencySymbol;

    @GetMapping
    public String dashboard(Model model) {
        DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("summary", summary);
        model.addAttribute("activePage", "dashboard");
        return "dashboard/index";
    }


    @GetMapping("/analytics")
    public String analytics(Model model) {
        DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("summary", summary);
        model.addAttribute("activePage", "analytics");
        return "dashboard/analytics";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("activePage", "settings");
        return "dashboard/settings";
    }
}
