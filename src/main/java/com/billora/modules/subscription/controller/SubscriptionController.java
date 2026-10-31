package com.billora.modules.subscription.controller;

import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.customer.service.CustomerService;
import com.billora.modules.product.service.ProductService;
import com.billora.modules.subscription.dto.SubscriptionFormDto;
import com.billora.modules.subscription.entity.Subscription;
import com.billora.modules.subscription.service.PlanService;
import com.billora.modules.subscription.service.SubscriptionService;
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
@RequestMapping("/dashboard/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PlanService planService;
    private final ProductService productService;
    private final CustomerService customerService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.currency-symbol:$}")
    private String currencySymbol;

    @GetMapping
    public String listSubscriptions(
            @RequestParam(name = "status", required = false) String statusFilter,
            Model model) {

        SubscriptionStatus filterStatus = null;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                filterStatus = SubscriptionStatus.valueOf(statusFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions(filterStatus);

        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("activePage", "subscriptions");
        model.addAttribute("subscriptions", subscriptions);

        // Counts for tabs
        model.addAttribute("totalCount", subscriptionService.countAll());
        model.addAttribute("draftCount", subscriptionService.countByStatus(SubscriptionStatus.DRAFT));
        model.addAttribute("quotationCount", subscriptionService.countByStatus(SubscriptionStatus.QUOTATION));
        model.addAttribute("confirmedCount", subscriptionService.countByStatus(SubscriptionStatus.CONFIRMED));
        model.addAttribute("activeCount", subscriptionService.countByStatus(SubscriptionStatus.ACTIVE));
        model.addAttribute("pausedCount", subscriptionService.countByStatus(SubscriptionStatus.PAUSED));
        model.addAttribute("closedCount", subscriptionService.countByStatus(SubscriptionStatus.CLOSED));
        model.addAttribute("cancelledCount", subscriptionService.countByStatus(SubscriptionStatus.CANCELLED));

        model.addAttribute("currentFilter", filterStatus != null ? filterStatus.name() : "ALL");

        // Data for create / edit modal
        model.addAttribute("plans", planService.getAllActivePlans());
        model.addAttribute("products", productService.getActiveProducts());
        model.addAttribute("customers", customerService.getAllActiveCustomers());
        model.addAttribute("billingCycles", BillingCycle.values());
        model.addAttribute("lifecycleStatuses", List.of(
                SubscriptionStatus.DRAFT,
                SubscriptionStatus.QUOTATION,
                SubscriptionStatus.CONFIRMED,
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.PAUSED,
                SubscriptionStatus.CLOSED,
                SubscriptionStatus.CANCELLED
        ));

        if (!model.containsAttribute("subscriptionDto")) {
            model.addAttribute("subscriptionDto", new SubscriptionFormDto());
        }

        return "dashboard/subscriptions";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String saveSubscription(
            @Valid @ModelAttribute("subscriptionDto") SubscriptionFormDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subscriptionDto", bindingResult);
            redirectAttributes.addFlashAttribute("subscriptionDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the errors in the subscription form.");
            redirectAttributes.addFlashAttribute("openModal", true);
            return "redirect:/dashboard/subscriptions";
        }

        try {
            if (dto.getId() == null) {
                Subscription created = subscriptionService.createSubscription(dto);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Subscription '" + created.getSubscriptionCode() + "' created successfully with status " + created.getStatus().getDisplayName() + "!");
            } else {
                Subscription updated = subscriptionService.updateSubscription(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Subscription '" + updated.getSubscriptionCode() + "' updated successfully!");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("subscriptionDto", dto);
            redirectAttributes.addFlashAttribute("openModal", true);
        }

        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/lifecycle")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String transitionLifecycle(
            @PathVariable("id") Long id,
            @RequestParam("status") SubscriptionStatus targetStatus,
            RedirectAttributes redirectAttributes) {

        try {
            Subscription updated = subscriptionService.transitionLifecycle(id, targetStatus);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription '" + updated.getSubscriptionCode() + "' moved to " + targetStatus.getDisplayName() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + ex.getMessage());
        }

        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String pauseSubscription(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Subscription paused = subscriptionService.pauseSubscription(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription '" + paused.getSubscriptionCode() + "' has been paused.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error pausing subscription: " + ex.getMessage());
        }
        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String resumeSubscription(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Subscription resumed = subscriptionService.resumeSubscription(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription '" + resumed.getSubscriptionCode() + "' is now active.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error resuming subscription: " + ex.getMessage());
        }
        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String renewSubscription(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Subscription renewed = subscriptionService.renewSubscription(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription '" + renewed.getSubscriptionCode() + "' renewed. Next billing date: " + renewed.getNextBillingDate() + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error renewing subscription: " + ex.getMessage());
        }
        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String closeSubscription(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Subscription closed = subscriptionService.closeSubscription(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription '" + closed.getSubscriptionCode() + "' closed early.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error closing subscription: " + ex.getMessage());
        }
        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String cancelSubscription(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false, defaultValue = "Cancelled by user") String reason,
            RedirectAttributes redirectAttributes) {

        try {
            Subscription cancelled = subscriptionService.cancelSubscription(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Subscription '" + cancelled.getSubscriptionCode() + "' has been cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling subscription: " + ex.getMessage());
        }

        return "redirect:/dashboard/subscriptions";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") SubscriptionStatus status,
            RedirectAttributes redirectAttributes) {
        return transitionLifecycle(id, status, redirectAttributes);
    }
}
