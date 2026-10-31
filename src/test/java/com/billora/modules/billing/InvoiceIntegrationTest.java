package com.billora.modules.billing;

import com.billora.core.enums.InvoiceStatus;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.billing.repository.InvoiceRepository;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.repository.CustomerRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class InvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .name("Invoice Test Customer")
                .email("inv.test." + System.currentTimeMillis() + "@example.com")
                .company("Invoice Co")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("Staff can view invoices page")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldDisplayInvoicesPage() throws Exception {
        mockMvc.perform(get("/dashboard/invoices"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/invoices"))
                .andExpect(model().attributeExists("invoices"))
                .andExpect(model().attributeExists("invoiceDto"));
    }

    @Test
    @DisplayName("Admin can create manual invoice with tax and line item calculation")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldCreateManualInvoice() throws Exception {
        long countBefore = invoiceRepository.count();

        mockMvc.perform(post("/dashboard/invoices")
                        .with(csrf())
                        .param("customerId", testCustomer.getId().toString())
                        .param("issueDate", LocalDate.now().toString())
                        .param("dueDate", LocalDate.now().plusDays(30).toString())
                        .param("itemDescription", "Cloud Architecture Audit")
                        .param("quantity", "2")
                        .param("unitPrice", "100.00")
                        .param("taxRate", "10.00")
                        .param("discountAmount", "20.00")
                        .param("status", "CONFIRMED")
                        .param("notes", "Net 30 terms"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/invoices"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(invoiceRepository.count()).isEqualTo(countBefore + 1);

        Invoice created = invoiceRepository.findAllByOrderByCreatedAtDesc().get(0);
        assertThat(created.getCustomer().getId()).isEqualTo(testCustomer.getId());
        // Subtotal: 2 * 100 = 200. Taxable: 200 - 20 = 180. Tax: 10% of 180 = 18. Total: 198.00.
        assertThat(created.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(created.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(created.getTaxAmount()).isEqualByComparingTo("18.00");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("198.00");
        assertThat(created.getStatus()).isEqualTo(InvoiceStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Admin can confirm, mark as paid, and cancel invoices")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldUpdateInvoiceStatuses() throws Exception {
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-TEST-LIFECYCLE")
                .customer(testCustomer)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(15))
                .subtotal(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .status(InvoiceStatus.DRAFT)
                .build());

        // 1. Confirm
        mockMvc.perform(post("/dashboard/invoices/" + invoice.getId() + "/confirm").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/invoices"));

        Invoice confirmed = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(InvoiceStatus.CONFIRMED);

        // 2. Mark Paid
        mockMvc.perform(post("/dashboard/invoices/" + invoice.getId() + "/pay").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/invoices"));

        Invoice paid = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getPaidAt()).isNotNull();

        // 3. Cancel
        mockMvc.perform(post("/dashboard/invoices/" + invoice.getId() + "/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/invoices"));

        Invoice cancelled = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    @DisplayName("User can view printable invoice details page")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldDisplayInvoiceViewPage() throws Exception {
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-TEST-VIEW")
                .customer(testCustomer)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .subtotal(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("500.00"))
                .status(InvoiceStatus.CONFIRMED)
                .build());

        mockMvc.perform(get("/dashboard/invoices/" + invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/invoice-view"))
                .andExpect(model().attributeExists("invoice"));
    }
}
