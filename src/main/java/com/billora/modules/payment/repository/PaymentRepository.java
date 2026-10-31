package com.billora.modules.payment.repository;

import com.billora.core.enums.PaymentStatus;
import com.billora.modules.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    List<Payment> findAllByOrderByPaymentDateDesc();

    List<Payment> findByStatusOrderByPaymentDateDesc(PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal calculateTotalSuccessfulAmount();
}
