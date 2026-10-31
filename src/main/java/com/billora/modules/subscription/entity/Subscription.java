package com.billora.modules.subscription.entity;

import com.billora.core.entity.BaseEntity;
import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscriptions_code", columnList = "subscription_code", unique = true),
        @Index(name = "idx_subscriptions_status", columnList = "status"),
        @Index(name = "idx_subscriptions_customer", columnList = "customer_id"),
        @Index(name = "idx_subscriptions_product", columnList = "product_id")
})
public class Subscription extends BaseEntity {

    @Column(name = "subscription_code", nullable = false, unique = true, length = 50)
    private String subscriptionCode;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private BillingCycle billingCycle;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "payment_terms", length = 60)
    private String paymentTerms = "Due on Receipt";

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Helper to compute total amount automatically: quantity * unitPrice
     */
    public void calculateTotalAmount() {
        if (unitPrice == null) {
            unitPrice = (plan != null && plan.getPrice() != null) ? plan.getPrice() : BigDecimal.ZERO;
        }
        int qty = quantity > 0 ? quantity : 1;
        this.amount = unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    /**
     * Helper to compute next billing date from current date and billing cycle.
     */
    public static LocalDate calculateNextBillingDate(LocalDate fromDate, BillingCycle cycle) {
        if (fromDate == null) {
            fromDate = LocalDate.now();
        }
        if (cycle == null) {
            return fromDate.plusMonths(1);
        }
        return switch (cycle) {
            case DAILY -> fromDate.plusDays(1);
            case WEEKLY -> fromDate.plusWeeks(1);
            case MONTHLY -> fromDate.plusMonths(1);
            case QUARTERLY -> fromDate.plusMonths(3);
            case SEMI_ANNUALLY -> fromDate.plusMonths(6);
            case YEARLY, ANNUALLY -> fromDate.plusYears(1);
            case LIFETIME -> null;
        };
    }
}
