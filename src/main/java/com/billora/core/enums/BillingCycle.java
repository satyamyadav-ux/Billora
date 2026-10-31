package com.billora.core.enums;

public enum BillingCycle {
    MONTHLY("Monthly", 1),
    QUARTERLY("Quarterly", 3),
    SEMI_ANNUALLY("Semi-Annually", 6),
    ANNUALLY("Annually", 12),
    LIFETIME("Lifetime", 0);

    private final String label;
    private final int months;

    BillingCycle(String label, int months) {
        this.label = label;
        this.months = months;
    }

    public String getLabel() {
        return label;
    }

    public int getMonths() {
        return months;
    }
}
