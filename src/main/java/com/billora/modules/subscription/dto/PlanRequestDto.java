package com.billora.modules.subscription.dto;

import com.billora.core.enums.BillingCycle;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequestDto {

    private Long id;

    @NotBlank(message = "Plan name is required.")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters.")
    private String name;

    @Size(max = 60, message = "Code cannot exceed 60 characters.")
    private String code;

    private Long productId;

    @NotNull(message = "Plan price is required.")
    @DecimalMin(value = "0.00", message = "Price must be greater than or equal to 0.00.")
    private BigDecimal price;

    @NotNull(message = "Billing cycle is required.")
    private BillingCycle billingCycle;

    @NotNull(message = "Minimum quantity is required.")
    @Min(value = 1, message = "Minimum quantity must be at least 1.")
    @Builder.Default
    private int minQuantity = 1;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Min(value = 0, message = "Trial days cannot be negative.")
    @Builder.Default
    private int trialDays = 0;

    @Size(max = 100, message = "Highlight badge cannot exceed 100 characters.")
    private String highlight;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
    private String description;

    @Builder.Default
    private boolean active = true;
}
