package com.billora.modules.payment.controller;

import com.billora.core.enums.InvoiceStatus;
import com.billora.core.enums.PaymentMethod;
import com.billora.core.enums.PaymentStatus;
import com.billora.modules.billing.service.InvoiceService;
import com.billora.modules.customer.service.CustomerService;
import com.billora.modules.payment.dto.PaymentRequestDto;
import com.billora.modules.payment.entity.Payment;
import com.billora.modules.payment.service.PaymentService;
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
@RequestMapping("/dashboard/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CustomerService customerService;
    private final InvoiceService invoiceService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.currency-symbol:$}")
    private String currencySymbol;

    @GetMapping
    public String listPayments(
            @RequestParam(name = "status", required = false) String statusFilter,
            Model model) {

        PaymentStatus filterStatus = null;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            try {
                filterStatus = PaymentStatus.valueOf(statusFilter.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Payment> payments = paymentService.getAllPayments(filterStatus);

        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("activePage", "payments");
        model.addAttribute("payments", payments);

        // Summaries
        model.addAttribute("totalSuccessfulAmount", paymentService.calculateTotalSuccessful());
        model.addAttribute("totalCount", paymentService.countAll());
        model.addAttribute("successCount", paymentService.countByStatus(PaymentStatus.SUCCESS));
        model.addAttribute("pendingCount", paymentService.countByStatus(PaymentStatus.PENDING));
        model.addAttribute("refundedCount", paymentService.countByStatus(PaymentStatus.REFUNDED));
        model.addAttribute("failedCount", paymentService.countByStatus(PaymentStatus.FAILED));

        model.addAttribute("currentFilter", filterStatus != null ? filterStatus.name() : "ALL");

        // Form selection options
        model.addAttribute("customers", customerService.getAllActiveCustomers());
        model.addAttribute("unpaidInvoices", invoiceService.getAllInvoices(InvoiceStatus.CONFIRMED));
        model.addAttribute("paymentMethods", PaymentMethod.values());

        if (!model.containsAttribute("paymentDto")) {
            model.addAttribute("paymentDto", new PaymentRequestDto());
        }

        return "dashboard/payments";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String recordPayment(
            @Valid @ModelAttribute("paymentDto") PaymentRequestDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.paymentDto", bindingResult);
            redirectAttributes.addFlashAttribute("paymentDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix errors in the payment form.");
            redirectAttributes.addFlashAttribute("openModal", true);
            return "redirect:/dashboard/payments";
        }

        try {
            Payment saved = paymentService.recordPayment(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment '" + saved.getTransactionId() + "' recorded successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("paymentDto", dto);
            redirectAttributes.addFlashAttribute("openModal", true);
        }

        return "redirect:/dashboard/payments";
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public String refundPayment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Payment refunded = paymentService.refundPayment(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Transaction '" + refunded.getTransactionId() + "' has been refunded.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing refund: " + ex.getMessage());
        }
        return "redirect:/dashboard/payments";
    }
}
