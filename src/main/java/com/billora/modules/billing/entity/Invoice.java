package com.billora.modules.billing.entity;

import com.billora.core.entity.BaseEntity;
import com.billora.core.enums.InvoiceStatus;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.subscription.entity.Subscription;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoices_status", columnList = "status"),
        @Index(name = "idx_invoices_customer", columnList = "customer_id")
})
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 60)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "billing_cycle", length = 30)
    private String billingCycle;

    @Column(name = "notes", length = 500)
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<InvoiceItem> items = new ArrayList<>();

    public void addItem(InvoiceItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setInvoice(this);
    }

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (InvoiceItem item : items) {
                if (item.getTotalPrice() != null) {
                    sum = sum.add(item.getTotalPrice());
                } else if (item.getUnitPrice() != null) {
                    BigDecimal line = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    item.setTotalPrice(line);
                    sum = sum.add(line);
                }
            }
        }
        this.subtotal = sum;

        BigDecimal discount = this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO;
        BigDecimal taxable = this.subtotal.subtract(discount);
        if (taxable.compareTo(BigDecimal.ZERO) < 0) {
            taxable = BigDecimal.ZERO;
        }

        BigDecimal rate = this.taxRate != null ? this.taxRate : BigDecimal.ZERO;
        this.taxAmount = taxable.multiply(rate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        this.totalAmount = taxable.add(this.taxAmount);
    }
}
