package com.billora.core.enums;

public enum PaymentStatus {
    PENDING("Pending", "badge bg-warning-subtle text-warning"),
    SUCCESS("Successful", "badge bg-success-subtle text-success"),
    FAILED("Failed", "badge bg-danger-subtle text-danger"),
    REFUNDED("Refunded", "badge bg-info-subtle text-info");

    private final String label;
    private final String badgeClass;

    PaymentStatus(String label, String badgeClass) {
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
