package com.billora.modules.billing.dto;

import com.billora.core.enums.InvoiceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ManualInvoiceRequestDto {

    private Long id;

    @NotNull(message = "Please select a customer.")
    private Long customerId;

    private Long subscriptionId;

    @NotNull(message = "Invoice date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;

    @NotNull(message = "Due date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @NotBlank(message = "Line item description is required.")
    private String itemDescription;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    @Builder.Default
    private int quantity = 1;

    @NotNull(message = "Unit price is required.")
    @DecimalMin(value = "0.00", message = "Unit price cannot be negative.")
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Tax rate cannot be negative.")
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative.")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.CONFIRMED;

    private String notes;
}
