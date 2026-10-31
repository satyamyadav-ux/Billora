package com.billora.modules.subscription;

import com.billora.core.enums.BillingCycle;
import com.billora.core.enums.SubscriptionStatus;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.repository.CustomerRepository;
import com.billora.modules.subscription.entity.Plan;
import com.billora.modules.subscription.entity.Subscription;
import com.billora.modules.subscription.repository.PlanRepository;
import com.billora.modules.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;
    private Plan testPlan;

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .name("Test Customer Inc.")
                .email("test.customer." + System.currentTimeMillis() + "@example.com")
                .company("Test Co")
                .active(true)
                .build());

        testPlan = planRepository.save(Plan.builder()
                .name("Growth Pro Test")
                .code("GROWTH_TEST_" + System.currentTimeMillis())
                .description("Test description")
                .price(new BigDecimal("199.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .trialDays(7)
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    @DisplayName("GET /dashboard/subscriptions returns 200 with subscription model")
    void testListSubscriptions() throws Exception {
        mockMvc.perform(get("/dashboard/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/subscriptions"))
                .andExpect(model().attributeExists("subscriptions"))
                .andExpect(model().attributeExists("plans"))
                .andExpect(model().attributeExists("customers"));
    }

    @Test
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    @DisplayName("POST /dashboard/subscriptions creates a new subscription in the database")
    void testCreateSubscriptionWithExistingCustomer() throws Exception {
        long initialCount = subscriptionRepository.count();

        mockMvc.perform(post("/dashboard/subscriptions")
                        .with(csrf())
                        .param("customerId", String.valueOf(testCustomer.getId()))
                        .param("planId", String.valueOf(testPlan.getId()))
                        .param("billingCycle", "MONTHLY")
                        .param("customAmount", "199.00")
                        .param("startDate", LocalDate.now().toString())
                        .param("notes", "New test enterprise client"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/subscriptions"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(subscriptionRepository.count()).isEqualTo(initialCount + 1);

        List<Subscription> subs = subscriptionRepository.findAllByOrderByCreatedAtDesc();
        Subscription latest = subs.get(0);
        assertThat(latest.getCustomer().getId()).isEqualTo(testCustomer.getId());
        assertThat(latest.getPlan().getId()).isEqualTo(testPlan.getId());
        assertThat(latest.getAmount()).isEqualByComparingTo(new BigDecimal("199.00"));
        assertThat(latest.getSubscriptionCode()).startsWith("SUB-");
        assertThat(latest.getNextBillingDate()).isNotNull();
    }

    @Test
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    @DisplayName("POST /dashboard/subscriptions with quick-add customer creates both customer and subscription")
    void testCreateSubscriptionWithQuickAddCustomer() throws Exception {
        long initialCustomerCount = customerRepository.count();
        long initialSubCount = subscriptionRepository.count();

        String newEmail = "inline.quick." + System.currentTimeMillis() + "@acme.org";

        mockMvc.perform(post("/dashboard/subscriptions")
                        .with(csrf())
                        .param("newCustomerName", "Inline Acme Corp")
                        .param("newCustomerEmail", newEmail)
                        .param("newCustomerCompany", "Acme Holdings")
                        .param("planId", String.valueOf(testPlan.getId()))
                        .param("billingCycle", "ANNUALLY")
                        .param("customAmount", "1500.00")
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/subscriptions"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(customerRepository.count()).isEqualTo(initialCustomerCount + 1);
        assertThat(subscriptionRepository.count()).isEqualTo(initialSubCount + 1);

        Customer createdCust = customerRepository.findByEmailIgnoreCase(newEmail).orElseThrow();
        assertThat(createdCust.getName()).isEqualTo("Inline Acme Corp");
    }

    @Test
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    @DisplayName("POST /dashboard/subscriptions/{id}/cancel cancels an active subscription")
    void testCancelSubscription() throws Exception {
        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .subscriptionCode("SUB-TEST-CANCEL")
                .customer(testCustomer)
                .plan(testPlan)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .amount(new BigDecimal("199.00"))
                .startDate(LocalDate.now())
                .nextBillingDate(LocalDate.now().plusMonths(1))
                .build());

        mockMvc.perform(post("/dashboard/subscriptions/" + sub.getId() + "/cancel")
                        .with(csrf())
                        .param("reason", "Requested refund"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/subscriptions"))
                .andExpect(flash().attributeExists("successMessage"));

        Subscription updated = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(updated.getCancelledAt()).isNotNull();
    }
}
