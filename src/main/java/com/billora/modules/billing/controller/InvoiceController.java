package com.billora.modules.billing.controller;

import com.billora.core.enums.InvoiceStatus;
import com.billora.modules.billing.dto.ManualInvoiceRequestDto;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.billing.service.InvoiceService;
import com.billora.modules.customer.service.CustomerService;
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

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/dashboard/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final SubscriptionService subscriptionService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.currency-symbol:$}")
    private String currencySymbol;

    @GetMapping
    public String listInvoices(
            @RequestParam(name = "status", required = false) String statusFilter,
            Model model) {

        InvoiceStatus filterStatus = null;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                filterStatus = InvoiceStatus.valueOf(statusFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Invoice> invoices = invoiceService.getAllInvoices(filterStatus);

        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("activePage", "invoices");
        model.addAttribute("invoices", invoices);

        // Financial KPI summaries
        model.addAttribute("totalPaidAmount", invoiceService.calculateTotalPaid());
        model.addAttribute("pendingCollectionAmount", invoiceService.calculatePendingCollection());

        // Status counts
        model.addAttribute("totalCount", invoiceService.countAll());
        model.addAttribute("draftCount", invoiceService.countByStatus(InvoiceStatus.DRAFT));
        model.addAttribute("confirmedCount", invoiceService.countByStatus(InvoiceStatus.CONFIRMED));
        model.addAttribute("paidCount", invoiceService.countByStatus(InvoiceStatus.PAID));
        model.addAttribute("overdueCount", invoiceService.countByStatus(InvoiceStatus.OVERDUE));
        model.addAttribute("cancelledCount", invoiceService.countByStatus(InvoiceStatus.CANCELLED));

        model.addAttribute("currentFilter", filterStatus != null ? filterStatus.name() : "ALL");

        // Form selections
        model.addAttribute("customers", customerService.getAllActiveCustomers());
        model.addAttribute("subscriptions", subscriptionService.getActiveSubscriptions());

        if (!model.containsAttribute("invoiceDto")) {
            ManualInvoiceRequestDto dto = new ManualInvoiceRequestDto();
            dto.setIssueDate(LocalDate.now());
            dto.setDueDate(LocalDate.now().plusDays(30));
            model.addAttribute("invoiceDto", dto);
        }

        return "dashboard/invoices";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String saveManualInvoice(
            @Valid @ModelAttribute("invoiceDto") ManualInvoiceRequestDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.invoiceDto", bindingResult);
            redirectAttributes.addFlashAttribute("invoiceDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix errors in the invoice form.");
            redirectAttributes.addFlashAttribute("openModal", true);
            return "redirect:/dashboard/invoices";
        }

        try {
            if (dto.getId() == null) {
                Invoice created = invoiceService.createManualInvoice(dto);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Invoice '" + created.getInvoiceNumber() + "' created successfully!");
            } else {
                Invoice updated = invoiceService.updateManualInvoice(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Invoice '" + updated.getInvoiceNumber() + "' updated successfully!");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("invoiceDto", dto);
            redirectAttributes.addFlashAttribute("openModal", true);
        }

        return "redirect:/dashboard/invoices";
    }

    @PostMapping("/generate-due")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String generateDueInvoices(RedirectAttributes redirectAttributes) {
        try {
            int count = invoiceService.generateAllDueInvoices();
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Generated " + count + " recurring invoice(s) for active subscriptions.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "All active subscriptions are currently up to date. No new invoices needed.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error generating invoices: " + ex.getMessage());
        }
        return "redirect:/dashboard/invoices";
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String confirmInvoice(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Invoice confirmed = invoiceService.confirmInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice '" + confirmed.getInvoiceNumber() + "' confirmed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error confirming invoice: " + ex.getMessage());
        }
        return "redirect:/dashboard/invoices";
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String markAsPaid(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Invoice paid = invoiceService.markAsPaid(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice '" + paid.getInvoiceNumber() + "' marked as PAID.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating payment status: " + ex.getMessage());
        }
        return "redirect:/dashboard/invoices";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String cancelInvoice(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Invoice cancelled = invoiceService.cancelInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice '" + cancelled.getInvoiceNumber() + "' cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling invoice: " + ex.getMessage());
        }
        return "redirect:/dashboard/invoices";
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("invoice", invoice);
        model.addAttribute("activePage", "invoices");
        return "dashboard/invoice-view";
    }

    @GetMapping("/{id}/print")
    public String printInvoice(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("invoice", invoice);
        return "dashboard/invoice-print";
    }
}
