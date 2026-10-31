package com.billora.modules.payment.dto;

import com.billora.core.enums.PaymentMethod;
import com.billora.core.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDto {

    @NotNull(message = "Please select a customer.")
    private Long customerId;

    private Long invoiceId;

    @NotNull(message = "Payment amount is required.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.00.")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required.")
    private PaymentMethod paymentMethod;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.SUCCESS;

    private String notes;
}
