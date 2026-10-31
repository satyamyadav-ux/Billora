package com.billora.core.enums;

public enum InvoiceStatus {
    DRAFT("Draft", "badge bg-light text-secondary border"),
    CONFIRMED("Confirmed", "badge bg-primary-subtle text-primary border border-primary-subtle"),
    PAID("Paid", "badge bg-success-subtle text-success border border-success-subtle"),
    OVERDUE("Overdue", "badge bg-warning-subtle text-warning border border-warning-subtle"),
    CANCELLED("Cancelled", "badge bg-danger-subtle text-danger border border-danger-subtle"),
    OPEN("Open", "badge bg-info-subtle text-info border border-info-subtle"),
    UNCOLLECTIBLE("Uncollectible", "badge bg-danger-subtle text-danger border border-danger-subtle"),
    VOID("Void", "badge bg-dark-subtle text-dark border border-dark-subtle");

    private final String label;
    private final String badgeClass;

    InvoiceStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
