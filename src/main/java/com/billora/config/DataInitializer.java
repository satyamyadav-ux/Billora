package com.billora.config;

import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.auth.service.AuthService;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.repository.CustomerRepository;
import com.billora.modules.product.entity.Product;
import com.billora.modules.product.entity.ProductVariant;
import com.billora.modules.product.repository.ProductRepository;
import com.billora.modules.subscription.entity.Plan;
import com.billora.modules.subscription.entity.Subscription;
import com.billora.modules.subscription.repository.PlanRepository;
import com.billora.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.billora.core.enums.InvoiceStatus;
import com.billora.core.enums.PaymentMethod;
import com.billora.core.enums.PaymentStatus;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.billing.entity.InvoiceItem;
import com.billora.modules.billing.repository.InvoiceRepository;
import com.billora.modules.payment.entity.Payment;
import com.billora.modules.payment.repository.PaymentRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AuthService authService;
    private final ProductRepository productRepository;
    private final PlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed demo users
        authService.seedDefaultUsers();

        // 1.5 Seed demo products and variants
        Product cloudSuite = null;
        Product devOpsPlatform = null;
        if (productRepository.count() == 0) {
            cloudSuite = Product.builder()
                    .name("Cloud Billing Suite")
                    .code("BILL-SUITE")
                    .productType("SaaS")
                    .salesPrice(new BigDecimal("149.00"))
                    .costPrice(new BigDecimal("45.00"))
                    .category("SaaS Platform")
                    .description("End-to-end automated subscription and billing cloud engine.")
                    .active(true)
                    .build();

            ProductVariant v1 = ProductVariant.builder()
                    .variantName("Single-Region Node")
                    .sku("BILL-SUITE-V1")
                    .additionalPrice(BigDecimal.ZERO)
                    .active(true)
                    .build();

            ProductVariant v2 = ProductVariant.builder()
                    .variantName("Multi-Region High Availability")
                    .sku("BILL-SUITE-V2")
                    .additionalPrice(new BigDecimal("99.00"))
                    .active(true)
                    .build();

            cloudSuite.addVariant(v1);
            cloudSuite.addVariant(v2);

            devOpsPlatform = Product.builder()
                    .name("DevOps Gateway")
                    .code("DEVOPS-GW")
                    .productType("Digital Service")
                    .salesPrice(new BigDecimal("89.00"))
                    .costPrice(new BigDecimal("20.00"))
                    .category("Developer Tools")
                    .description("CI/CD pipeline webhook integration and analytics bridge.")
                    .active(true)
                    .build();

            productRepository.saveAll(List.of(cloudSuite, devOpsPlatform));
            log.info("Seeded demo products with variants.");
        } else {
            List<Product> prods = productRepository.findAll();
            if (!prods.isEmpty()) {
                cloudSuite = prods.get(0);
            }
        }

        // 2. Seed pricing plans
        if (planRepository.count() == 0) {
            Plan starter = Plan.builder()
                    .name("Starter Team")
                    .code("STARTER")
                    .product(cloudSuite)
                    .description("Essential tools for growing teams up to 5 members.")
                    .price(new BigDecimal("99.00"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .minQuantity(1)
                    .startDate(LocalDate.now().minusMonths(3))
                    .trialDays(14)
                    .active(true)
                    .highlight("Popular for Startups")
                    .build();

            Plan pro = Plan.builder()
                    .name("Pro Growth")
                    .code("PRO")
                    .product(cloudSuite)
                    .description("Advanced billing, recurring invoices, and custom tax rules.")
                    .price(new BigDecimal("289.00"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .minQuantity(1)
                    .startDate(LocalDate.now().minusMonths(2))
                    .trialDays(7)
                    .active(true)
                    .highlight("Best Value")
                    .build();

            Plan scale = Plan.builder()
                    .name("Scale Cloud")
                    .code("SCALE")
                    .product(cloudSuite)
                    .description("High-volume transaction processing and multi-currency billing.")
                    .price(new BigDecimal("499.00"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .minQuantity(2)
                    .startDate(LocalDate.now().minusMonths(1))
                    .trialDays(0)
                    .active(true)
                    .build();

            Plan enterprise = Plan.builder()
                    .name("Enterprise Tier")
                    .code("ENTERPRISE")
                    .product(cloudSuite)
                    .description("Dedicated infrastructure, custom SLAs, and priority support.")
                    .price(new BigDecimal("1249.00"))
                    .billingCycle(BillingCycle.YEARLY)
                    .minQuantity(5)
                    .startDate(LocalDate.now().minusMonths(6))
                    .trialDays(0)
                    .active(true)
                    .build();


            planRepository.saveAll(List.of(starter, pro, scale, enterprise));
            log.info("Seeded 4 default subscription plans.");
        }

        // 3. Seed demo customers
        if (customerRepository.count() == 0) {
            Customer c1 = Customer.builder()
                    .name("Acme Technologies")
                    .email("billing@acmetech.io")
                    .company("Acme Global Inc.")
                    .phone("+1 555-0192")
                    .address("100 Innovation Way, San Francisco, CA")
                    .active(true)
                    .build();

            Customer c2 = Customer.builder()
                    .name("Nova Global Media")
                    .email("admin@novamedia.co")
                    .company("Nova Networks")
                    .phone("+1 555-0143")
                    .address("45 Broad Street, New York, NY")
                    .active(true)
                    .build();

            Customer c3 = Customer.builder()
                    .name("Hyperion Labs")
                    .email("finance@hyperionlabs.ai")
                    .company("Hyperion AI LLC")
                    .phone("+1 555-0188")
                    .address("12 Research Blvd, Boston, MA")
                    .active(true)
                    .build();

            Customer c4 = Customer.builder()
                    .name("Pulse Health AI")
                    .email("contact@pulsehealth.org")
                    .company("Pulse Health Systems")
                    .phone("+1 555-0111")
                    .address("78 MediPark Dr, Chicago, IL")
                    .active(true)
                    .build();

            Customer c5 = Customer.builder()
                    .name("Zenith Robotics")
                    .email("ops@zenithrobotics.tech")
                    .company("Zenith Tech Group")
                    .phone("+1 555-0155")
                    .address("500 Silicon Vista, Austin, TX")
                    .active(true)
                    .build();

            customerRepository.saveAll(List.of(c1, c2, c3, c4, c5));
            log.info("Seeded 5 demo customers.");
        }

        // 4. Seed initial subscriptions
        if (subscriptionRepository.count() == 0) {
            List<Plan> plans = planRepository.findAll();
            List<Customer> customers = customerRepository.findAll();

            if (!plans.isEmpty() && customers.size() >= 5) {
                Plan enterprise = plans.stream().filter(p -> p.getCode().equals("ENTERPRISE")).findFirst().orElse(plans.get(0));
                Plan pro = plans.stream().filter(p -> p.getCode().equals("PRO")).findFirst().orElse(plans.get(0));
                Plan scale = plans.stream().filter(p -> p.getCode().equals("SCALE")).findFirst().orElse(plans.get(0));
                Plan starter = plans.stream().filter(p -> p.getCode().equals("STARTER")).findFirst().orElse(plans.get(0));

                Subscription s1 = Subscription.builder()
                        .subscriptionCode("SUB-8492")
                        .customer(customers.get(0))
                        .plan(enterprise)
                        .status(SubscriptionStatus.ACTIVE)
                        .billingCycle(enterprise.getBillingCycle())
                        .amount(enterprise.getPrice())
                        .startDate(LocalDate.now().minusDays(14))
                        .nextBillingDate(LocalDate.now().plusMonths(11).plusDays(16))
                        .notes("Annual contract signed")
                        .build();

                Subscription s2 = Subscription.builder()
                        .subscriptionCode("SUB-8491")
                        .customer(customers.get(1))
                        .plan(pro)
                        .status(SubscriptionStatus.ACTIVE)
                        .billingCycle(BillingCycle.MONTHLY)
                        .amount(pro.getPrice())
                        .startDate(LocalDate.now().minusDays(5))
                        .nextBillingDate(LocalDate.now().plusDays(25))
                        .notes("Auto-renewal enabled")
                        .build();

                Subscription s3 = Subscription.builder()
                        .subscriptionCode("SUB-8490")
                        .customer(customers.get(2))
                        .plan(scale)
                        .status(SubscriptionStatus.TRIALING)
                        .billingCycle(BillingCycle.MONTHLY)
                        .amount(scale.getPrice())
                        .startDate(LocalDate.now().minusDays(2))
                        .nextBillingDate(LocalDate.now().plusDays(12))
                        .trialEndDate(LocalDate.now().plusDays(12))
                        .notes("Evaluating scale tier")
                        .build();

                Subscription s4 = Subscription.builder()
                        .subscriptionCode("SUB-8489")
                        .customer(customers.get(3))
                        .plan(starter)
                        .status(SubscriptionStatus.PAST_DUE)
                        .billingCycle(BillingCycle.MONTHLY)
                        .amount(starter.getPrice())
                        .startDate(LocalDate.now().minusMonths(2))
                        .nextBillingDate(LocalDate.now().minusDays(3))
                        .notes("Payment retry pending")
                        .build();

                Subscription s5 = Subscription.builder()
                        .subscriptionCode("SUB-8488")
                        .customer(customers.get(4))
                        .plan(enterprise)
                        .status(SubscriptionStatus.ACTIVE)
                        .billingCycle(BillingCycle.QUARTERLY)
                        .amount(new BigDecimal("3500.00"))
                        .startDate(LocalDate.now().minusMonths(1))
                        .nextBillingDate(LocalDate.now().plusMonths(2))
                        .notes("Quarterly payments")
                        .build();

                subscriptionRepository.saveAll(List.of(s1, s2, s3, s4, s5));
                log.info("Seeded 5 demo subscriptions.");

                // 5. Seed initial demo invoices & payments
                if (invoiceRepository.count() == 0) {
                    Invoice inv1 = Invoice.builder()
                            .invoiceNumber("INV-1001")
                            .customer(customers.get(0))
                            .subscription(s1)
                            .issueDate(LocalDate.now().minusDays(14))
                            .dueDate(LocalDate.now().plusDays(16))
                            .subtotal(new BigDecimal("1249.00"))
                            .taxRate(new BigDecimal("10.00"))
                            .taxAmount(new BigDecimal("124.90"))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("1373.90"))
                            .status(InvoiceStatus.PAID)
                            .paidAt(LocalDateTime.now().minusDays(10))
                            .billingCycle("YEARLY")
                            .notes("Annual subscription enterprise tier")
                            .build();

                    inv1.addItem(InvoiceItem.builder()
                            .product(cloudSuite)
                            .description("Enterprise Tier Cloud Billing Suite (Annual)")
                            .quantity(1)
                            .unitPrice(new BigDecimal("1249.00"))
                            .totalPrice(new BigDecimal("1249.00"))
                            .build());

                    Invoice inv2 = Invoice.builder()
                            .invoiceNumber("INV-1002")
                            .customer(customers.get(1))
                            .subscription(s2)
                            .issueDate(LocalDate.now().minusDays(5))
                            .dueDate(LocalDate.now().plusDays(25))
                            .subtotal(new BigDecimal("289.00"))
                            .taxRate(BigDecimal.ZERO)
                            .taxAmount(BigDecimal.ZERO)
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(new BigDecimal("289.00"))
                            .status(InvoiceStatus.CONFIRMED)
                            .billingCycle("MONTHLY")
                            .notes("Monthly recurring growth plan")
                            .build();

                    inv2.addItem(InvoiceItem.builder()
                            .product(cloudSuite)
                            .description("Pro Growth Subscription (Monthly)")
                            .quantity(1)
                            .unitPrice(new BigDecimal("289.00"))
                            .totalPrice(new BigDecimal("289.00"))
                            .build());

                    invoiceRepository.saveAll(List.of(inv1, inv2));
                    log.info("Seeded demo invoices.");

                    // Seed payments
                    if (paymentRepository.count() == 0) {
                        Payment p1 = Payment.builder()
                                .transactionId("TX-849201")
                                .customer(customers.get(0))
                                .invoice(inv1)
                                .amount(new BigDecimal("1373.90"))
                                .paymentMethod(PaymentMethod.CREDIT_CARD)
                                .status(PaymentStatus.SUCCESS)
                                .paymentDate(LocalDateTime.now().minusDays(10))
                                .notes("Stripe card payment settlement")
                                .build();

                        paymentRepository.save(p1);
                        log.info("Seeded demo payments.");
                    }
                }
            }
        }
    }
}
