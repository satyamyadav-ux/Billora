package com.billora.modules.subscription.entity;

import com.billora.core.entity.BaseEntity;
import com.billora.core.enums.BillingCycle;
import com.billora.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "plans", indexes = {
        @Index(name = "idx_plans_code", columnList = "code", unique = true),
        @Index(name = "idx_plans_active", columnList = "active"),
        @Index(name = "idx_plans_cycle", columnList = "billing_cycle")
})
public class Plan extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private BillingCycle billingCycle;

    @Builder.Default
    @Column(name = "min_quantity", nullable = false)
    private int minQuantity = 1;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "trial_days", nullable = false)
    private int trialDays = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "highlight", length = 100)
    private String highlight;
}
