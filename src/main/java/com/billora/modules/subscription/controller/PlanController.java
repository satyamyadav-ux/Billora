package com.billora.modules.subscription.controller;

import com.billora.core.enums.BillingCycle;
import com.billora.modules.product.service.ProductService;
import com.billora.modules.subscription.dto.PlanRequestDto;
import com.billora.modules.subscription.entity.Plan;
import com.billora.modules.subscription.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final ProductService productService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.currency-symbol:$}")
    private String currencySymbol;

    @GetMapping
    public String listPlans(Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("activePage", "plans");
        model.addAttribute("plans", planService.getAllPlans());
        model.addAttribute("products", productService.getActiveProducts());
        
        // Highlight the 4 required cycles: Daily, Weekly, Monthly, Yearly
        List<BillingCycle> cycles = List.of(
                BillingCycle.DAILY,
                BillingCycle.WEEKLY,
                BillingCycle.MONTHLY,
                BillingCycle.YEARLY
        );
        model.addAttribute("billingCycles", cycles);

        if (!model.containsAttribute("planDto")) {
            model.addAttribute("planDto", new PlanRequestDto());
        }

        return "dashboard/plans";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String savePlan(
            @Valid @ModelAttribute("planDto") PlanRequestDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.planDto", bindingResult);
            redirectAttributes.addFlashAttribute("planDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors in the plan form.");
            redirectAttributes.addFlashAttribute("openModal", true);
            return "redirect:/dashboard/plans";
        }

        try {
            Plan saved = planService.savePlan(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Plan '" + saved.getName() + "' saved successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("planDto", dto);
            redirectAttributes.addFlashAttribute("openModal", true);
        }

        return "redirect:/dashboard/plans";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String togglePlan(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Plan toggled = planService.togglePlanStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Plan '" + toggled.getName() + "' status changed to " + (toggled.isActive() ? "Active" : "Inactive") + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating plan status: " + ex.getMessage());
        }
        return "redirect:/dashboard/plans";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletePlan(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            planService.deletePlan(id);
            redirectAttributes.addFlashAttribute("successMessage", "Plan deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete plan: " + ex.getMessage());
        }
        return "redirect:/dashboard/plans";
    }
}
