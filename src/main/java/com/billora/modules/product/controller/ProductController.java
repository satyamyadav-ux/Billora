package com.billora.modules.product.controller;

import com.billora.modules.product.dto.ProductRequestDto;
import com.billora.modules.product.dto.ProductVariantDto;
import com.billora.modules.product.entity.Product;
import com.billora.modules.product.entity.ProductVariant;
import com.billora.modules.product.service.ProductService;
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
@RequestMapping("/dashboard/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Value("${billora.app.name:Billora}")
    private String appName;

    @Value("${billora.app.currency-symbol:$}")
    private String currencySymbol;

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("activePage", "products");
        model.addAttribute("products", productService.getAllProducts());
        if (!model.containsAttribute("productDto")) {
            model.addAttribute("productDto", new ProductRequestDto());
        }
        if (!model.containsAttribute("variantDto")) {
            model.addAttribute("variantDto", new ProductVariantDto());
        }
        return "dashboard/products";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String saveProduct(
            @Valid @ModelAttribute("productDto") ProductRequestDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productDto", bindingResult);
            redirectAttributes.addFlashAttribute("productDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix errors in the product form.");
            redirectAttributes.addFlashAttribute("openModal", true);
            return "redirect:/dashboard/products";
        }

        try {
            Product saved = productService.saveProduct(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + saved.getName() + "' saved successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("productDto", dto);
            redirectAttributes.addFlashAttribute("openModal", true);
        }

        return "redirect:/dashboard/products";
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String addVariant(
            @PathVariable("id") Long productId,
            @Valid @ModelAttribute("variantDto") ProductVariantDto variantDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid variant data. Name is required.");
            return "redirect:/dashboard/products";
        }

        try {
            ProductVariant variant = productService.addProductVariant(productId, variantDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Variant '" + variant.getVariantName() + "' added successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding variant: " + ex.getMessage());
        }

        return "redirect:/dashboard/products";
    }

    @PostMapping("/variants/{variantId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String deleteVariant(@PathVariable("variantId") Long variantId, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductVariant(variantId);
            redirectAttributes.addFlashAttribute("successMessage", "Variant removed successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting variant: " + ex.getMessage());
        }
        return "redirect:/dashboard/products";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String toggleStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Product toggled = productService.toggleProductStatus(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + toggled.getName() + "' status changed to " + (toggled.isActive() ? "Active" : "Inactive") + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product status: " + ex.getMessage());
        }
        return "redirect:/dashboard/products";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product removed successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete product: " + ex.getMessage());
        }
        return "redirect:/dashboard/products";
    }
}
