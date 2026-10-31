package com.billora.core.enums;

public enum SubscriptionStatus {
    TRIALING("Trialing", "badge bg-info-subtle text-info"),
    ACTIVE("Active", "badge bg-success-subtle text-success"),
    PAST_DUE("Past Due", "badge bg-warning-subtle text-warning"),
    PAUSED("Paused", "badge bg-secondary-subtle text-secondary"),
    CANCELLED("Cancelled", "badge bg-danger-subtle text-danger"),
    EXPIRED("Expired", "badge bg-dark-subtle text-dark");

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
