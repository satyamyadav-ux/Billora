package com.billora.modules.customer.entity;

import com.billora.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_email", columnList = "email", unique = true)
})
public class Customer extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "company", length = 150)
    private String company;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Compute 2-letter uppercase initials for avatar badge.
     */
    public String getAvatar() {
        if (name == null || name.isBlank()) {
            return "CU";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
