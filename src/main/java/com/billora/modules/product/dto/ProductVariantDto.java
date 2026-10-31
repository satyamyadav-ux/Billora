package com.billora.modules.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {

    private Long id;

    private Long productId;

    @NotBlank(message = "Variant name is required.")
    @Size(min = 1, max = 120, message = "Variant name cannot exceed 120 characters.")
    private String variantName;

    @Size(max = 60, message = "SKU cannot exceed 60 characters.")
    private String sku;

    @NotNull(message = "Additional price is required.")
    @DecimalMin(value = "0.00", message = "Additional price cannot be negative.")
    @Builder.Default
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Builder.Default
    private boolean active = true;
}
