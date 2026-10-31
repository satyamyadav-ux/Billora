package com.billora.modules.payment.service;

import com.billora.common.exception.ResourceNotFoundException;
import com.billora.core.enums.PaymentStatus;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.billing.repository.InvoiceRepository;
import com.billora.modules.billing.service.InvoiceService;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.service.CustomerService;
import com.billora.modules.payment.dto.PaymentRequestDto;
import com.billora.modules.payment.entity.Payment;
import com.billora.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final CustomerService customerService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments(PaymentStatus status) {
        if (status != null) {
            return paymentRepository.findByStatusOrderByPaymentDateDesc(status);
        }
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    @Transactional
    public Payment recordPayment(PaymentRequestDto dto) {
        Customer customer = customerService.getCustomerById(dto.getCustomerId());

        Invoice invoice = null;
        if (dto.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(dto.getInvoiceId()).orElse(null);
        }

        String transactionId = generateUniqueTransactionId();

        PaymentStatus status = dto.getStatus() != null ? dto.getStatus() : PaymentStatus.SUCCESS;

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .customer(customer)
                .invoice(invoice)
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .status(status)
                .paymentDate(LocalDateTime.now())
                .notes(dto.getNotes() != null ? dto.getNotes().trim() : null)
                .build();

        Payment saved = paymentRepository.save(payment);

        // If invoice linked and payment was successful, mark invoice as paid
        if (invoice != null && status == PaymentStatus.SUCCESS) {
            invoiceService.markAsPaid(invoice.getId());
        }

        log.info("Recorded payment {} of {} for customer {}", saved.getTransactionId(), saved.getAmount(), customer.getName());
        return saved;
    }

    @Transactional
    public Payment refundPayment(Long id) {
        Payment payment = getPaymentById(id);
        payment.setStatus(PaymentStatus.REFUNDED);
        log.info("Payment {} refunded", payment.getTransactionId());
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return paymentRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByStatus(PaymentStatus status) {
        return paymentRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSuccessful() {
        return paymentRepository.calculateTotalSuccessfulAmount();
    }

    private String generateUniqueTransactionId() {
        for (int i = 0; i < 100; i++) {
            int num = 100000 + RANDOM.nextInt(900000);
            String candidate = "TX-" + num;
            if (!paymentRepository.existsByTransactionId(candidate)) {
                return candidate;
            }
        }
        return "TX-" + System.currentTimeMillis();
    }
}
