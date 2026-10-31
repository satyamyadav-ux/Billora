package com.billora.modules.customer.controller;

import com.billora.modules.customer.dto.CustomerRequestDto;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @GetMapping
    public String listCustomers(Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("activePage", "customers");
        model.addAttribute("customers", customerService.getAllCustomers());

        if (!model.containsAttribute("customerDto")) {
            model.addAttribute("customerDto", new CustomerRequestDto());
        }

        return "dashboard/customers";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String saveCustomer(
            @Valid @ModelAttribute("customerDto") CustomerRequestDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerDto", bindingResult);
            redirectAttributes.addFlashAttribute("customerDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix errors in the customer form.");
            redirectAttributes.addFlashAttribute("openModal", true);
            return "redirect:/dashboard/customers";
        }

        try {
            Customer saved = customerService.saveCustomer(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer '" + saved.getName() + "' saved successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("customerDto", dto);
            redirectAttributes.addFlashAttribute("openModal", true);
        }

        return "redirect:/dashboard/customers";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String toggleCustomerStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Customer toggled = customerService.toggleCustomerStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer '" + toggled.getName() + "' status changed to " + (toggled.isActive() ? "Active" : "Inactive") + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating customer status: " + ex.getMessage());
        }
        return "redirect:/dashboard/customers";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCustomer(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Customer deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete customer: " + ex.getMessage());
        }
        return "redirect:/dashboard/customers";
    }
}
