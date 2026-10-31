package com.billora.modules.subscription.repository;

import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriptionCode(String subscriptionCode);

    boolean existsBySubscriptionCode(String subscriptionCode);

    List<Subscription> findAllByOrderByCreatedAtDesc();

    List<Subscription> findByStatusOrderByCreatedAtDesc(SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);

    long countByStatusIn(Collection<SubscriptionStatus> statuses);

    List<Subscription> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Subscription s WHERE s.status = 'ACTIVE'")
    BigDecimal calculateActiveRevenue();
}
