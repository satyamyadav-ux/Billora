package com.billora.core.enums;

public enum InvoiceStatus {
    DRAFT("Draft", "badge bg-secondary-subtle text-secondary"),
    OPEN("Open", "badge bg-warning-subtle text-warning"),
    PAID("Paid", "badge bg-success-subtle text-success"),
    UNCOLLECTIBLE("Uncollectible", "badge bg-danger-subtle text-danger"),
    VOID("Void", "badge bg-dark-subtle text-dark");

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
