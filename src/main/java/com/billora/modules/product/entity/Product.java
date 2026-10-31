package com.billora.modules.product.entity;

import com.billora.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_code", columnList = "code", unique = true),
        @Index(name = "idx_products_active", columnList = "active"),
        @Index(name = "idx_products_type", columnList = "product_type")
})
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Builder.Default
    @Column(name = "product_type", nullable = false, length = 60)
    private String productType = "SaaS";

    @Builder.Default
    @Column(name = "sales_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salesPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        if (variants == null) {
            variants = new ArrayList<>();
        }
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        if (variants != null) {
            variants.remove(variant);
            variant.setProduct(null);
        }
    }
}
