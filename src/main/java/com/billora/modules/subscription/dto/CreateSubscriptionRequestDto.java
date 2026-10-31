package com.billora.modules.subscription.dto;

import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionRequestDto {

    private Long id;

    private Long customerId;

    // Optional quick-add customer fields if existing customerId not selected
    private String newCustomerName;
    private String newCustomerEmail;
    private String newCustomerCompany;

    private Long productId;

    @NotNull(message = "Please select a pricing plan")
    private Long planId;

    @Builder.Default
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    @DecimalMin(value = "0.00", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    private BillingCycle billingCycle;

    private BigDecimal customAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Builder.Default
    private String paymentTerms = "Due on Receipt";

    private SubscriptionStatus status;

    private String notes;
}
