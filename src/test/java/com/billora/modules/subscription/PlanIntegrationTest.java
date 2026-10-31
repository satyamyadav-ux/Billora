package com.billora.modules.subscription;

import com.billora.core.enums.BillingCycle;
import com.billora.modules.product.entity.Product;
import com.billora.modules.product.repository.ProductRepository;
import com.billora.modules.subscription.entity.Plan;
import com.billora.modules.subscription.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PlanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Staff can view plans dashboard page")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldDisplayPlansPage() throws Exception {
        mockMvc.perform(get("/dashboard/plans"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/plans"))
                .andExpect(model().attributeExists("plans"))
                .andExpect(model().attributeExists("planDto"))
                .andExpect(model().attributeExists("billingCycles"));
    }

    @ParameterizedTest
    @EnumSource(value = BillingCycle.class, names = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"})
    @DisplayName("Admin can create plans with Daily, Weekly, Monthly, and Yearly billing cycles")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldCreatePlanWithCycle(BillingCycle cycle) throws Exception {
        String code = "PLAN-" + cycle.name();

        mockMvc.perform(post("/dashboard/plans")
                        .with(csrf())
                        .param("name", cycle.getLabel() + " Pro Plan")
                        .param("code", code)
                        .param("price", "49.99")
                        .param("billingCycle", cycle.name())
                        .param("minQuantity", "2")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusYears(1).toString())
                        .param("trialDays", "7")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"))
                .andExpect(flash().attributeExists("successMessage"));

        Plan created = planRepository.findByCodeIgnoreCase(code).orElseThrow();
        assertThat(created.getBillingCycle()).isEqualTo(cycle);
        assertThat(created.getPrice()).isEqualByComparingTo("49.99");
        assertThat(created.getMinQuantity()).isEqualTo(2);
        assertThat(created.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(created.getEndDate()).isEqualTo(LocalDate.now().plusYears(1));
        assertThat(created.isActive()).isTrue();
    }

    @Test
    @DisplayName("Admin can link plan to a specific product")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldCreatePlanLinkedToProduct() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Analytics Engine")
                .code("ANALYTICS-ENG")
                .productType("SaaS")
                .salesPrice(new BigDecimal("100.00"))
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/plans")
                        .with(csrf())
                        .param("name", "Analytics Monthly Growth")
                        .param("code", "ANALYTICS-MTH")
                        .param("productId", product.getId().toString())
                        .param("price", "89.00")
                        .param("billingCycle", "MONTHLY")
                        .param("minQuantity", "1")
                        .param("startDate", LocalDate.now().toString())
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"));

        Plan plan = planRepository.findByCodeIgnoreCase("ANALYTICS-MTH").orElseThrow();
        assertThat(plan.getProduct()).isNotNull();
        assertThat(plan.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("Admin can update existing plan")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldUpdatePlan() throws Exception {
        Plan plan = planRepository.save(Plan.builder()
                .name("Original Plan")
                .code("ORIG-01")
                .price(new BigDecimal("29.00"))
                .billingCycle(BillingCycle.MONTHLY)
                .minQuantity(1)
                .startDate(LocalDate.now())
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/plans")
                        .with(csrf())
                        .param("id", plan.getId().toString())
                        .param("name", "Updated Enterprise Plan")
                        .param("code", "ORIG-01")
                        .param("price", "99.00")
                        .param("billingCycle", "YEARLY")
                        .param("minQuantity", "5")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusMonths(6).toString())
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"))
                .andExpect(flash().attributeExists("successMessage"));

        Plan updated = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Enterprise Plan");
        assertThat(updated.getPrice()).isEqualByComparingTo("99.00");
        assertThat(updated.getBillingCycle()).isEqualTo(BillingCycle.YEARLY);
        assertThat(updated.getMinQuantity()).isEqualTo(5);
        assertThat(updated.getEndDate()).isEqualTo(LocalDate.now().plusMonths(6));
    }

    @Test
    @DisplayName("Admin can toggle (enable/disable) plan status")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldTogglePlanActiveStatus() throws Exception {
        Plan plan = planRepository.save(Plan.builder()
                .name("Status Plan")
                .code("STATUS-PLN")
                .price(new BigDecimal("19.00"))
                .billingCycle(BillingCycle.DAILY)
                .minQuantity(1)
                .active(true)
                .build());

        // Disable
        mockMvc.perform(post("/dashboard/plans/" + plan.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"));

        Plan toggled = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(toggled.isActive()).isFalse();

        // Re-enable
        mockMvc.perform(post("/dashboard/plans/" + plan.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"));

        toggled = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(toggled.isActive()).isTrue();
    }

    @Test
    @DisplayName("Admin can delete plan")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldDeletePlan() throws Exception {
        Plan plan = planRepository.save(Plan.builder()
                .name("Deletable Plan")
                .code("DEL-PLN")
                .price(new BigDecimal("10.00"))
                .billingCycle(BillingCycle.WEEKLY)
                .minQuantity(1)
                .active(true)
                .build());

        mockMvc.perform(post("/dashboard/plans/" + plan.getId() + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(planRepository.findById(plan.getId())).isEmpty();
    }

    @Test
    @DisplayName("Validation fails when end date is before start date")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldRejectWhenEndDateBeforeStartDate() throws Exception {
        mockMvc.perform(post("/dashboard/plans")
                        .with(csrf())
                        .param("name", "Invalid Dates Plan")
                        .param("price", "20.00")
                        .param("billingCycle", "MONTHLY")
                        .param("minQuantity", "1")
                        .param("startDate", "2026-10-31")
                        .param("endDate", "2026-10-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/plans"))
                .andExpect(flash().attribute("errorMessage", "Plan End Date cannot be before Start Date."));
    }
}
