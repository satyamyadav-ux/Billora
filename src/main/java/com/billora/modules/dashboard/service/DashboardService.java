package com.billora.modules.dashboard.service;

import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.dashboard.dto.DashboardSummaryDto;
import com.billora.modules.dashboard.dto.RecentSubscriptionDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    public DashboardSummaryDto getDashboardSummary() {
        List<RecentSubscriptionDto> recentSubscriptions = List.of(
                RecentSubscriptionDto.builder()
                        .id("SUB-8492")
                        .customerName("Acme Technologies")
                        .customerEmail("billing@acmetech.io")
                        .customerAvatar("AT")
                        .planName("Enterprise Tier")
                        .amount(new BigDecimal("1249.00"))
                        .billingCycle(BillingCycle.ANNUALLY)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(LocalDate.now().minusDays(14))
                        .nextBillingDate(LocalDate.now().plusMonths(11).plusDays(16))
                        .build(),
                RecentSubscriptionDto.builder()
                        .id("SUB-8491")
                        .customerName("Nova Global Media")
                        .customerEmail("admin@novamedia.co")
                        .customerAvatar("NG")
                        .planName("Pro Growth")
                        .amount(new BigDecimal("289.00"))
                        .billingCycle(BillingCycle.MONTHLY)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(LocalDate.now().minusDays(5))
                        .nextBillingDate(LocalDate.now().plusDays(25))
                        .build(),
                RecentSubscriptionDto.builder()
                        .id("SUB-8490")
                        .customerName("Hyperion Labs")
                        .customerEmail("finance@hyperionlabs.ai")
                        .customerAvatar("HL")
                        .planName("Scale Cloud")
                        .amount(new BigDecimal("499.00"))
                        .billingCycle(BillingCycle.MONTHLY)
                        .status(SubscriptionStatus.TRIALING)
                        .startDate(LocalDate.now().minusDays(2))
                        .nextBillingDate(LocalDate.now().plusDays(12))
                        .build(),
                RecentSubscriptionDto.builder()
                        .id("SUB-8489")
                        .customerName("Pulse Health AI")
                        .customerEmail("contact@pulsehealth.org")
                        .customerAvatar("PH")
                        .planName("Starter Team")
                        .amount(new BigDecimal("99.00"))
                        .billingCycle(BillingCycle.MONTHLY)
                        .status(SubscriptionStatus.PAST_DUE)
                        .startDate(LocalDate.now().minusMonths(2))
                        .nextBillingDate(LocalDate.now().minusDays(3))
                        .build(),
                RecentSubscriptionDto.builder()
                        .id("SUB-8488")
                        .customerName("Zenith Robotics")
                        .customerEmail("ops@zenithrobotics.tech")
                        .customerAvatar("ZR")
                        .planName("Enterprise Tier")
                        .amount(new BigDecimal("1249.00"))
                        .billingCycle(BillingCycle.QUARTERLY)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(LocalDate.now().minusMonths(1))
                        .nextBillingDate(LocalDate.now().plusMonths(2))
                        .build()
        );

        return DashboardSummaryDto.builder()
                .monthlyRecurringRevenue(new BigDecimal("48290.00"))
                .annualRecurringRevenue(new BigDecimal("579480.00"))
                .netRevenueThisMonth(new BigDecimal("52140.00"))
                .totalActiveSubscriptions(1482L)
                .totalCustomers(1240L)
                .churnRatePercentage(1.42)
                .mrrGrowthRatePercentage(18.6)
                .pendingInvoicesCount(8L)
                .pendingInvoicesAmount(new BigDecimal("3420.00"))
                .recentSubscriptions(recentSubscriptions)
                .build();
    }
}
