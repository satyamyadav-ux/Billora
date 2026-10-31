package com.billora.modules.billing.entity;

import com.billora.core.entity.BaseEntity;
import com.billora.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice_items", indexes = {
        @Index(name = "idx_invoice_items_invoice", columnList = "invoice_id")
})
public class InvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private int quantity = 1;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;
}
