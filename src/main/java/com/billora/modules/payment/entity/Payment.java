package com.billora.modules.payment.entity;

import com.billora.core.entity.BaseEntity;
import com.billora.core.enums.PaymentMethod;
import com.billora.core.enums.PaymentStatus;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_tx", columnList = "transaction_id", unique = true),
        @Index(name = "idx_payments_customer", columnList = "customer_id"),
        @Index(name = "idx_payments_status", columnList = "status")
})
public class Payment extends BaseEntity {

    @Column(name = "transaction_id", nullable = false, unique = true, length = 60)
    private String transactionId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Builder.Default
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Column(name = "notes", length = 500)
    private String notes;
}
