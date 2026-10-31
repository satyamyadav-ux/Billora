package com.billora.modules.dashboard.dto;

import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentSubscriptionDto {
    private String id;
    private String customerName;
    private String customerEmail;
    private String customerAvatar;
    private String planName;
    private BigDecimal amount;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate nextBillingDate;
}
