package com.billora.modules.dashboard.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.tagline:Subscription & Recurring Billing Management}")
    private String appTagline;

    @Value("${billora.app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("appTagline", appTagline);
        model.addAttribute("appVersion", appVersion);
        return "index";
    }
}
