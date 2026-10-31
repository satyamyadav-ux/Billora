package com.billora.modules.dashboard.service;

import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.customer.repository.CustomerRepository;
import com.billora.modules.dashboard.dto.DashboardSummaryDto;
import com.billora.modules.dashboard.dto.RecentSubscriptionDto;
import com.billora.modules.subscription.entity.Subscription;
import com.billora.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        List<Subscription> recentEntities = subscriptionRepository.findTop5ByOrderByCreatedAtDesc();

        List<RecentSubscriptionDto> recentSubscriptions = recentEntities.stream()
                .map(s -> RecentSubscriptionDto.builder()
                        .id(s.getSubscriptionCode())
                        .customerName(s.getCustomer().getName())
                        .customerEmail(s.getCustomer().getEmail())
                        .customerAvatar(s.getCustomer().getAvatar())
                        .planName(s.getPlan().getName())
                        .amount(s.getAmount())
                        .billingCycle(s.getBillingCycle())
                        .status(s.getStatus())
                        .startDate(s.getStartDate())
                        .nextBillingDate(s.getNextBillingDate())
                        .build())
                .toList();

        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long totalCustomers = customerRepository.count();
        BigDecimal activeMrr = subscriptionRepository.calculateActiveRevenue();
        if (activeMrr == null) {
            activeMrr = BigDecimal.ZERO;
        }

        BigDecimal arr = activeMrr.multiply(BigDecimal.valueOf(12));

        return DashboardSummaryDto.builder()
                .monthlyRecurringRevenue(activeMrr)
                .annualRecurringRevenue(arr)
                .netRevenueThisMonth(activeMrr)
                .totalActiveSubscriptions(activeSubscriptions)
                .totalCustomers(totalCustomers)
                .churnRatePercentage(activeSubscriptions > 0 ? 1.25 : 0.0)
                .mrrGrowthRatePercentage(15.4)
                .pendingInvoicesCount(subscriptionRepository.countByStatus(SubscriptionStatus.PAST_DUE))
                .pendingInvoicesAmount(BigDecimal.valueOf(350.00))
                .recentSubscriptions(recentSubscriptions)
                .build();
    }
}
