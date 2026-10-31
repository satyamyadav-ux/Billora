package com.billora.modules.subscription.repository;

import com.billora.modules.subscription.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Plan> findByActiveTrueOrderByPriceAsc();

    List<Plan> findAllByOrderByCreatedAtDesc();
}
