package com.billora.core.enums;

public enum PaymentMethod {
    CREDIT_CARD("Credit / Debit Card"),
    BANK_TRANSFER("Bank Transfer / Wire"),
    UPI("UPI / Instant Transfer"),
    PAYPAL("PayPal"),
    CHECK("Company Check");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
