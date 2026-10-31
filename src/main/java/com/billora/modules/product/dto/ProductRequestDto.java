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
public class ProductRequestDto {

    private Long id;

    @NotBlank(message = "Product name is required.")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters.")
    private String name;

    @Size(max = 60, message = "Code must be at most 60 characters.")
    private String code;

    @NotBlank(message = "Product type is required.")
    @Size(max = 60, message = "Product type cannot exceed 60 characters.")
    @Builder.Default
    private String productType = "SaaS";

    @NotNull(message = "Sales price is required.")
    @DecimalMin(value = "0.00", message = "Sales price must be greater than or equal to 0.00.")
    @Builder.Default
    private BigDecimal salesPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Cost price must be greater than or equal to 0.00.")
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Size(max = 80, message = "Category cannot exceed 80 characters.")
    private String category;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
    private String description;

    @Builder.Default
    private boolean active = true;

    // Optional initial variant during creation
    private String initialVariantName;
    private BigDecimal initialVariantAdditionalPrice;
}
