package com.billora.modules.subscription.service;

import com.billora.common.exception.BusinessException;
import com.billora.common.exception.ResourceNotFoundException;
import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.service.CustomerService;
import com.billora.modules.product.entity.Product;
import com.billora.modules.product.service.ProductService;
import com.billora.modules.subscription.dto.CreateSubscriptionRequestDto;
import com.billora.modules.subscription.dto.SubscriptionFormDto;
import com.billora.modules.subscription.entity.Plan;
import com.billora.modules.subscription.entity.Subscription;
import com.billora.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final PlanService planService;
    private final ProductService productService;

    @Transactional
    public Subscription createSubscription(SubscriptionFormDto dto) {
        // 1. Resolve Customer
        Customer customer = resolveCustomer(dto.getCustomerId(), dto.getNewCustomerName(), dto.getNewCustomerEmail(), dto.getNewCustomerCompany());

        // 2. Resolve Plan
        Plan plan = planService.getPlanById(dto.getPlanId());

        // 3. Resolve Product
        Product product = null;
        if (dto.getProductId() != null) {
            product = productService.getProductById(dto.getProductId());
        } else if (plan.getProduct() != null) {
            product = plan.getProduct();
        }

        // 4. Quantity & Unit Price
        int quantity = dto.getQuantity() > 0 ? dto.getQuantity() : 1;
        BigDecimal unitPrice = dto.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            unitPrice = plan.getPrice() != null ? plan.getPrice() : BigDecimal.ZERO;
        }

        // 5. Calculate Subscription Amount automatically
        BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // 6. Dates & Cycle
        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now();
        LocalDate endDate = dto.getEndDate();
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Subscription End Date cannot be before Start Date.");
        }

        BillingCycle cycle = dto.getBillingCycle() != null ? dto.getBillingCycle() : plan.getBillingCycle();

        // 7. Status (Default to ACTIVE or requested)
        SubscriptionStatus status = dto.getStatus() != null ? dto.getStatus() : SubscriptionStatus.ACTIVE;

        // 8. Next billing date
        LocalDate nextBillingDate = Subscription.calculateNextBillingDate(startDate, cycle);

        String code = generateUniqueSubscriptionCode();

        Subscription subscription = Subscription.builder()
                .subscriptionCode(code)
                .customer(customer)
                .product(product)
                .plan(plan)
                .status(status)
                .billingCycle(cycle)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .amount(amount)
                .startDate(startDate)
                .endDate(endDate)
                .paymentTerms(dto.getPaymentTerms() != null && !dto.getPaymentTerms().isBlank() ? dto.getPaymentTerms().trim() : "Due on Receipt")
                .nextBillingDate(nextBillingDate)
                .notes(dto.getNotes() != null && !dto.getNotes().isBlank() ? dto.getNotes().trim() : null)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Created subscription {} for customer {} with status {}",
                saved.getSubscriptionCode(), customer.getName(), saved.getStatus());
        return saved;
    }

    @Transactional
    public Subscription createSubscription(CreateSubscriptionRequestDto dto) {
        SubscriptionFormDto formDto = SubscriptionFormDto.builder()
                .customerId(dto.getCustomerId())
                .newCustomerName(dto.getNewCustomerName())
                .newCustomerEmail(dto.getNewCustomerEmail())
                .newCustomerCompany(dto.getNewCustomerCompany())
                .productId(dto.getProductId())
                .planId(dto.getPlanId())
                .quantity(dto.getQuantity() > 0 ? dto.getQuantity() : 1)
                .unitPrice(dto.getUnitPrice())
                .startDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now())
                .endDate(dto.getEndDate())
                .paymentTerms(dto.getPaymentTerms() != null ? dto.getPaymentTerms() : "Due on Receipt")
                .billingCycle(dto.getBillingCycle())
                .status(dto.getStatus() != null ? dto.getStatus() : SubscriptionStatus.ACTIVE)
                .notes(dto.getNotes())
                .build();

        if (dto.getCustomAmount() != null && dto.getCustomAmount().compareTo(BigDecimal.ZERO) >= 0) {
            formDto.setUnitPrice(dto.getCustomAmount());
        }

        return createSubscription(formDto);
    }

    @Transactional
    public Subscription updateSubscription(Long id, SubscriptionFormDto dto) {
        Subscription subscription = getSubscriptionById(id);

        if (dto.getCustomerId() != null) {
            subscription.setCustomer(customerService.getCustomerById(dto.getCustomerId()));
        }

        if (dto.getPlanId() != null) {
            Plan plan = planService.getPlanById(dto.getPlanId());
            subscription.setPlan(plan);
            if (dto.getProductId() == null && plan.getProduct() != null) {
                subscription.setProduct(plan.getProduct());
            }
        }

        if (dto.getProductId() != null) {
            subscription.setProduct(productService.getProductById(dto.getProductId()));
        }

        int quantity = dto.getQuantity() > 0 ? dto.getQuantity() : 1;
        subscription.setQuantity(quantity);

        BigDecimal unitPrice = dto.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            unitPrice = subscription.getPlan().getPrice();
        }
        subscription.setUnitPrice(unitPrice);
        subscription.setAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));

        if (dto.getStartDate() != null) {
            subscription.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            if (subscription.getStartDate() != null && dto.getEndDate().isBefore(subscription.getStartDate())) {
                throw new BusinessException("End Date cannot be before Start Date.");
            }
            subscription.setEndDate(dto.getEndDate());
        } else {
            subscription.setEndDate(null);
        }

        if (dto.getPaymentTerms() != null && !dto.getPaymentTerms().isBlank()) {
            subscription.setPaymentTerms(dto.getPaymentTerms().trim());
        }

        if (dto.getBillingCycle() != null) {
            subscription.setBillingCycle(dto.getBillingCycle());
        }

        if (dto.getStatus() != null) {
            subscription.setStatus(dto.getStatus());
        }

        if (dto.getNotes() != null) {
            subscription.setNotes(dto.getNotes().trim());
        }

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription transitionLifecycle(Long id, SubscriptionStatus targetStatus) {
        Subscription subscription = getSubscriptionById(id);
        SubscriptionStatus current = subscription.getStatus();

        // Validate valid transitions if needed
        subscription.setStatus(targetStatus);

        if (targetStatus == SubscriptionStatus.ACTIVE) {
            if (subscription.getNextBillingDate() == null) {
                subscription.setNextBillingDate(Subscription.calculateNextBillingDate(LocalDate.now(), subscription.getBillingCycle()));
            }
        } else if (targetStatus == SubscriptionStatus.CLOSED) {
            subscription.setClosedAt(LocalDateTime.now());
            subscription.setNextBillingDate(null);
        }

        log.info("Subscription {} transitioned from {} to {}", subscription.getSubscriptionCode(), current, targetStatus);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription pauseSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription.setPausedAt(LocalDateTime.now());
        log.info("Subscription {} paused", subscription.getSubscriptionCode());
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription resumeSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPausedAt(null);
        subscription.setNextBillingDate(Subscription.calculateNextBillingDate(LocalDate.now(), subscription.getBillingCycle()));
        log.info("Subscription {} resumed", subscription.getSubscriptionCode());
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription renewSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // Extend nextBillingDate and endDate
        LocalDate baseDate = subscription.getNextBillingDate() != null ? subscription.getNextBillingDate() : LocalDate.now();
        subscription.setNextBillingDate(Subscription.calculateNextBillingDate(baseDate, subscription.getBillingCycle()));

        if (subscription.getEndDate() != null) {
            subscription.setEndDate(Subscription.calculateNextBillingDate(subscription.getEndDate(), subscription.getBillingCycle()));
        }

        log.info("Subscription {} renewed. Next billing date: {}", subscription.getSubscriptionCode(), subscription.getNextBillingDate());
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription closeSubscription(Long id) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.CLOSED);
        subscription.setClosedAt(LocalDateTime.now());
        subscription.setNextBillingDate(null);
        log.info("Subscription {} closed early", subscription.getSubscriptionCode());
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription cancelSubscription(Long id, String reason) {
        Subscription subscription = getSubscriptionById(id);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setCancellationReason(reason != null && !reason.isBlank() ? reason.trim() : "Cancelled by user");
        subscription.setNextBillingDate(null);
        log.info("Subscription {} cancelled: {}", subscription.getSubscriptionCode(), subscription.getCancellationReason());
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription updateStatus(Long id, SubscriptionStatus newStatus) {
        return transitionLifecycle(id, newStatus);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getAllSubscriptions(SubscriptionStatus status) {
        if (status != null) {
            return subscriptionRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return subscriptionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptions() {
        return subscriptionRepository.findByStatusOrderByCreatedAtDesc(SubscriptionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return subscriptionRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByStatus(SubscriptionStatus status) {
        return subscriptionRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getRecentSubscriptions() {
        return subscriptionRepository.findTop5ByOrderByCreatedAtDesc();
    }

    private Customer resolveCustomer(Long customerId, String name, String email, String company) {
        if (customerId != null) {
            return customerService.getCustomerById(customerId);
        } else if (email != null && !email.isBlank()) {
            return customerService.getOrCreateCustomer(name, email, company);
        } else {
            throw new BusinessException("Please select an existing customer or provide customer name and email.");
        }
    }

    private String generateUniqueSubscriptionCode() {
        for (int i = 0; i < 100; i++) {
            int num = 1000 + RANDOM.nextInt(9000);
            String candidate = "SUB-" + num;
            if (!subscriptionRepository.existsBySubscriptionCode(candidate)) {
                return candidate;
            }
        }
        return "SUB-" + (System.currentTimeMillis() % 100000);
    }
}
