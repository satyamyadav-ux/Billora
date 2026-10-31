package com.billora.core.enums;

public enum SubscriptionStatus {
    DRAFT("Draft", "badge bg-light text-secondary border"),
    QUOTATION("Quotation", "badge bg-primary-subtle text-primary border border-primary-subtle"),
    CONFIRMED("Confirmed", "badge bg-info-subtle text-info border border-info-subtle"),
    ACTIVE("Active", "badge bg-success-subtle text-success border border-success-subtle"),
    PAUSED("Paused", "badge bg-warning-subtle text-warning border border-warning-subtle"),
    CLOSED("Closed", "badge bg-secondary-subtle text-secondary border border-secondary-subtle"),
    CANCELLED("Cancelled", "badge bg-danger-subtle text-danger border border-danger-subtle"),
    TRIALING("Trialing", "badge bg-info-subtle text-info border border-info-subtle"),
    PAST_DUE("Past Due", "badge bg-warning-subtle text-warning border border-warning-subtle"),
    EXPIRED("Expired", "badge bg-dark-subtle text-dark border border-dark-subtle");

    private final String displayName;
    private final String badgeClass;

    SubscriptionStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
