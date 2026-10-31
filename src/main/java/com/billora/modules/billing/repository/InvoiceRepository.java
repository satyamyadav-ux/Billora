package com.billora.modules.billing.repository;

import com.billora.core.enums.InvoiceStatus;
import com.billora.modules.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findAllByOrderByCreatedAtDesc();

    List<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status);

    long countByStatus(InvoiceStatus status);

    boolean existsBySubscriptionIdAndIssueDate(Long subscriptionId, LocalDate issueDate);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = 'PAID'")
    BigDecimal calculateTotalPaidAmount();

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status IN ('DRAFT', 'CONFIRMED', 'OPEN')")
    BigDecimal calculatePendingCollectionAmount();
}
