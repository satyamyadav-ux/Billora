package com.billora.modules.product;

import com.billora.modules.product.entity.Product;
import com.billora.modules.product.entity.ProductVariant;
import com.billora.modules.product.repository.ProductRepository;
import com.billora.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Test
    @DisplayName("Staff can view products dashboard page")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldDisplayProductsPage() throws Exception {
        mockMvc.perform(get("/dashboard/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("productDto"));
    }

    @Test
    @DisplayName("Admin can create product with all required fields")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldCreateProductWithAllFields() throws Exception {
        long countBefore = productRepository.count();

        mockMvc.perform(post("/dashboard/products")
                        .with(csrf())
                        .param("name", "High-Performance Cloud VM")
                        .param("productType", "Infrastructure")
                        .param("salesPrice", "79.99")
                        .param("costPrice", "25.00")
                        .param("code", "CLOUD-VM-HP")
                        .param("description", "Dedicated vCPU with nvme storage")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(productRepository.count()).isEqualTo(countBefore + 1);

        Product created = productRepository.findByCodeIgnoreCase("CLOUD-VM-HP").orElseThrow();
        assertThat(created.getName()).isEqualTo("High-Performance Cloud VM");
        assertThat(created.getProductType()).isEqualTo("Infrastructure");
        assertThat(created.getSalesPrice()).isEqualByComparingTo("79.99");
        assertThat(created.getCostPrice()).isEqualByComparingTo("25.00");
        assertThat(created.getDescription()).contains("Dedicated vCPU");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("Admin can update existing product fields")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldUpdateProduct() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Legacy Product")
                .code("LEGACY-001")
                .productType("SaaS")
                .salesPrice(new BigDecimal("50.00"))
                .costPrice(new BigDecimal("10.00"))
                .description("Old version")
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/products")
                        .with(csrf())
                        .param("id", product.getId().toString())
                        .param("name", "Modernized Product v2")
                        .param("code", "LEGACY-001")
                        .param("productType", "Digital Service")
                        .param("salesPrice", "65.50")
                        .param("costPrice", "12.00")
                        .param("description", "Upgraded cloud architecture")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"))
                .andExpect(flash().attributeExists("successMessage"));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Modernized Product v2");
        assertThat(updated.getProductType()).isEqualTo("Digital Service");
        assertThat(updated.getSalesPrice()).isEqualByComparingTo("65.50");
        assertThat(updated.getCostPrice()).isEqualByComparingTo("12.00");
    }

    @Test
    @DisplayName("Admin can add product variant with additional pricing")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldAddProductVariantWithAdditionalPrice() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Database Cluster")
                .code("DB-CLUSTER")
                .productType("SaaS")
                .salesPrice(new BigDecimal("199.00"))
                .costPrice(new BigDecimal("50.00"))
                .description("Managed PostgreSQL")
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/products/" + product.getId() + "/variants")
                        .with(csrf())
                        .param("variantName", "High Memory 64GB Node")
                        .param("sku", "DB-CLUSTER-64GB")
                        .param("additionalPrice", "80.00")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"))
                .andExpect(flash().attributeExists("successMessage"));

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId());
        assertThat(variants).hasSize(1);
        ProductVariant variant = variants.get(0);
        assertThat(variant.getVariantName()).isEqualTo("High Memory 64GB Node");
        assertThat(variant.getSku()).isEqualTo("DB-CLUSTER-64GB");
        assertThat(variant.getAdditionalPrice()).isEqualByComparingTo("80.00");
        assertThat(variant.isActive()).isTrue();
    }

    @Test
    @DisplayName("Staff can delete product variant")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldDeleteProductVariant() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Security Audit Suite")
                .code("SEC-AUDIT")
                .productType("Service")
                .salesPrice(new BigDecimal("299.00"))
                .costPrice(BigDecimal.ZERO)
                .active(true)
                .build());

        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .variantName("PCI-DSS Compliance Addon")
                .sku("SEC-AUDIT-PCI")
                .additionalPrice(new BigDecimal("150.00"))
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/products/variants/" + variant.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(productVariantRepository.findById(variant.getId())).isEmpty();
    }

    @Test
    @DisplayName("Admin can toggle product active status")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldToggleProductStatus() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Toggle Test Product")
                .code("TOGGLE-PRD")
                .productType("SaaS")
                .salesPrice(new BigDecimal("10.00"))
                .active(true)
                .build());

        // Toggle to Inactive
        mockMvc.perform(post("/dashboard/products/" + product.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"));

        Product toggled = productRepository.findById(product.getId()).orElseThrow();
        assertThat(toggled.isActive()).isFalse();

        // Toggle back to Active
        mockMvc.perform(post("/dashboard/products/" + product.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"));

        toggled = productRepository.findById(product.getId()).orElseThrow();
        assertThat(toggled.isActive()).isTrue();
    }

    @Test
    @DisplayName("Admin can delete product permanently")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldDeleteProduct() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Disposable Product")
                .code("DISPOSABLE-PRD")
                .productType("Physical")
                .salesPrice(new BigDecimal("25.00"))
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/products/" + product.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    @DisplayName("Validation fails when required product fields are missing")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldRejectInvalidProduct() throws Exception {
        mockMvc.perform(post("/dashboard/products")
                        .with(csrf())
                        .param("name", "") // Blank name
                        .param("salesPrice", "-10.00")) // Negative price
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/products"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
