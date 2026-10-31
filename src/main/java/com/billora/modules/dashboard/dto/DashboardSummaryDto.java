package com.billora.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private BigDecimal monthlyRecurringRevenue;
    private BigDecimal annualRecurringRevenue;
    private BigDecimal netRevenueThisMonth;
    private long totalActiveSubscriptions;
    private long totalCustomers;
    private double churnRatePercentage;
    private double mrrGrowthRatePercentage;
    private long pendingInvoicesCount;
    private BigDecimal pendingInvoicesAmount;

    private List<RecentSubscriptionDto> recentSubscriptions;
}
