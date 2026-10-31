package com.billora.modules.auth.enums;

public enum Role {
    ADMIN("Administrator", "badge bg-danger-subtle text-danger"),
    STAFF("Staff", "badge bg-primary-subtle text-primary"),
    CUSTOMER("Customer", "badge bg-success-subtle text-success");

    private final String displayName;
    private final String badgeClass;

    Role(String displayName, String badgeClass) {
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
