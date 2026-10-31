package com.billora.modules.billing.service;

import com.billora.common.exception.BusinessException;
import com.billora.common.exception.ResourceNotFoundException;
import com.billora.core.enums.InvoiceStatus;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.billing.dto.ManualInvoiceRequestDto;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.billing.entity.InvoiceItem;
import com.billora.modules.billing.repository.InvoiceRepository;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.service.CustomerService;
import com.billora.modules.subscription.entity.Subscription;
import com.billora.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvoiceRepository invoiceRepository;
    private final CustomerService customerService;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices(InvoiceStatus status) {
        if (status != null) {
            return invoiceRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return invoiceRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    @Transactional
    public Invoice createManualInvoice(ManualInvoiceRequestDto dto) {
        Customer customer = customerService.getCustomerById(dto.getCustomerId());

        Subscription subscription = null;
        if (dto.getSubscriptionId() != null) {
            subscription = subscriptionRepository.findById(dto.getSubscriptionId()).orElse(null);
        }

        LocalDate issueDate = dto.getIssueDate() != null ? dto.getIssueDate() : LocalDate.now();
        LocalDate dueDate = dto.getDueDate() != null ? dto.getDueDate() : issueDate.plusDays(30);

        if (dueDate.isBefore(issueDate)) {
            throw new BusinessException("Invoice Due Date cannot be before Issue Date.");
        }

        String invoiceNumber = generateUniqueInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .subscription(subscription)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .taxRate(dto.getTaxRate() != null ? dto.getTaxRate() : BigDecimal.ZERO)
                .discountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO)
                .status(dto.getStatus() != null ? dto.getStatus() : InvoiceStatus.CONFIRMED)
                .notes(dto.getNotes() != null ? dto.getNotes().trim() : null)
                .build();

        // Line Item
        int qty = dto.getQuantity() > 0 ? dto.getQuantity() : 1;
        BigDecimal unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

        InvoiceItem item = InvoiceItem.builder()
                .description(dto.getItemDescription() != null ? dto.getItemDescription().trim() : "Custom Service Item")
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalPrice(lineTotal)
                .build();

        invoice.addItem(item);
        invoice.recalculateTotals();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created manual invoice {} for customer {}", saved.getInvoiceNumber(), customer.getName());
        return saved;
    }

    @Transactional
    public Invoice updateManualInvoice(Long id, ManualInvoiceRequestDto dto) {
        Invoice invoice = getInvoiceById(id);

        if (dto.getCustomerId() != null) {
            invoice.setCustomer(customerService.getCustomerById(dto.getCustomerId()));
        }

        if (dto.getIssueDate() != null) {
            invoice.setIssueDate(dto.getIssueDate());
        }

        if (dto.getDueDate() != null) {
            if (invoice.getIssueDate() != null && dto.getDueDate().isBefore(invoice.getIssueDate())) {
                throw new BusinessException("Due Date cannot be before Issue Date.");
            }
            invoice.setDueDate(dto.getDueDate());
        }

        invoice.setTaxRate(dto.getTaxRate() != null ? dto.getTaxRate() : BigDecimal.ZERO);
        invoice.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : BigDecimal.ZERO);
        if (dto.getStatus() != null) {
            invoice.setStatus(dto.getStatus());
        }
        if (dto.getNotes() != null) {
            invoice.setNotes(dto.getNotes().trim());
        }

        // Update single primary item or add
        if (!invoice.getItems().isEmpty()) {
            InvoiceItem item = invoice.getItems().get(0);
            item.setDescription(dto.getItemDescription());
            item.setQuantity(dto.getQuantity() > 0 ? dto.getQuantity() : 1);
            item.setUnitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO);
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            int qty = dto.getQuantity() > 0 ? dto.getQuantity() : 1;
            BigDecimal unitPrice = dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO;
            invoice.addItem(InvoiceItem.builder()
                    .description(dto.getItemDescription())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .totalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)))
                    .build());
        }

        invoice.recalculateTotals();
        return invoiceRepository.save(invoice);
    }

    /**
     * Automatically generate invoice for an active subscription.
     * Prevents duplicate invoices for the same subscription on the same issue date.
     */
    @Transactional
    public Invoice generateSubscriptionInvoice(Subscription subscription) {
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException("Can only generate recurring invoices for ACTIVE subscriptions.");
        }

        LocalDate today = LocalDate.now();

        // Check duplicate invoice prevention
        if (invoiceRepository.existsBySubscriptionIdAndIssueDate(subscription.getId(), today)) {
            log.warn("Invoice already generated today for subscription {}", subscription.getSubscriptionCode());
            throw new BusinessException("An invoice for subscription " + subscription.getSubscriptionCode() + " has already been generated today.");
        }

        String invoiceNumber = generateUniqueInvoiceNumber();
        LocalDate dueDate = today.plusDays(15);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .customer(subscription.getCustomer())
                .subscription(subscription)
                .issueDate(today)
                .dueDate(dueDate)
                .billingCycle(subscription.getBillingCycle() != null ? subscription.getBillingCycle().name() : "MONTHLY")
                .status(InvoiceStatus.CONFIRMED)
                .notes("Automated recurring billing for " + subscription.getPlan().getName() + " (" + subscription.getSubscriptionCode() + ")")
                .build();

        int qty = subscription.getQuantity() > 0 ? subscription.getQuantity() : 1;
        BigDecimal unitPrice = subscription.getUnitPrice() != null ? subscription.getUnitPrice() : subscription.getAmount();

        InvoiceItem item = InvoiceItem.builder()
                .product(subscription.getProduct())
                .description("Subscription: " + subscription.getPlan().getName() + " - " + subscription.getBillingCycle().getLabel())
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)))
                .build();

        invoice.addItem(item);
        invoice.recalculateTotals();

        // Advance subscription next billing date
        subscription.setNextBillingDate(Subscription.calculateNextBillingDate(today, subscription.getBillingCycle()));
        subscriptionRepository.save(subscription);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Generated recurring invoice {} for subscription {}", saved.getInvoiceNumber(), subscription.getSubscriptionCode());
        return saved;
    }

    @Transactional
    public int generateAllDueInvoices() {
        LocalDate today = LocalDate.now();
        List<Subscription> activeSubs = subscriptionRepository.findByStatusOrderByCreatedAtDesc(SubscriptionStatus.ACTIVE);
        int generatedCount = 0;

        for (Subscription sub : activeSubs) {
            if (sub.getNextBillingDate() == null || !sub.getNextBillingDate().isAfter(today)) {
                if (!invoiceRepository.existsBySubscriptionIdAndIssueDate(sub.getId(), today)) {
                    try {
                        generateSubscriptionInvoice(sub);
                        generatedCount++;
                    } catch (Exception ex) {
                        log.error("Failed to generate invoice for subscription {}: {}", sub.getSubscriptionCode(), ex.getMessage());
                    }
                }
            }
        }
        return generatedCount;
    }

    @Transactional
    public Invoice confirmInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(InvoiceStatus.CONFIRMED);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markAsPaid(Long id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice cancelInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return invoiceRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByStatus(InvoiceStatus status) {
        return invoiceRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPaid() {
        return invoiceRepository.calculateTotalPaidAmount();
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePendingCollection() {
        return invoiceRepository.calculatePendingCollectionAmount();
    }

    private String generateUniqueInvoiceNumber() {
        for (int i = 0; i < 100; i++) {
            int num = 1000 + RANDOM.nextInt(9000);
            String candidate = "INV-" + num;
            if (!invoiceRepository.existsByInvoiceNumber(candidate)) {
                return candidate;
            }
        }
        return "INV-" + (System.currentTimeMillis() % 100000);
    }
}
