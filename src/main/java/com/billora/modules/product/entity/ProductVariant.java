package com.billora.modules.product.entity;

import com.billora.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variant_product", columnList = "product_id"),
        @Index(name = "idx_variant_sku", columnList = "sku")
})
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "variant_name", nullable = false, length = 120)
    private String variantName;

    @Column(name = "sku", length = 60)
    private String sku;

    @Builder.Default
    @Column(name = "additional_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
