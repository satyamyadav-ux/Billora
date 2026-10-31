package com.billora.modules.payment;

import com.billora.core.enums.InvoiceStatus;
import com.billora.core.enums.PaymentMethod;
import com.billora.core.enums.PaymentStatus;
import com.billora.modules.billing.entity.Invoice;
import com.billora.modules.billing.repository.InvoiceRepository;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.repository.CustomerRepository;
import com.billora.modules.payment.entity.Payment;
import com.billora.modules.payment.repository.PaymentRepository;
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
public class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .name("Payment Customer Corp")
                .email("pay.test." + System.currentTimeMillis() + "@example.com")
                .company("Pay Corp")
                .active(true)
                .build());

        testInvoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber("INV-PAY-001")
                .customer(testCustomer)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .subtotal(new BigDecimal("150.00"))
                .totalAmount(new BigDecimal("150.00"))
                .status(InvoiceStatus.CONFIRMED)
                .build());
    }

    @Test
    @DisplayName("Staff can view payments dashboard page")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldDisplayPaymentsPage() throws Exception {
        mockMvc.perform(get("/dashboard/payments"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/payments"))
                .andExpect(model().attributeExists("payments"))
                .andExpect(model().attributeExists("paymentDto"));
    }

    @Test
    @DisplayName("Staff can record payment and auto-mark linked invoice as PAID")
    @WithMockUser(username = "staff@billora.com", roles = {"STAFF"})
    void shouldRecordPaymentAndMarkInvoicePaid() throws Exception {
        long countBefore = paymentRepository.count();

        mockMvc.perform(post("/dashboard/payments")
                        .with(csrf())
                        .param("customerId", testCustomer.getId().toString())
                        .param("invoiceId", testInvoice.getId().toString())
                        .param("amount", "150.00")
                        .param("paymentMethod", "CREDIT_CARD")
                        .param("status", "SUCCESS")
                        .param("notes", "Stripe payment #ch_12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/payments"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(paymentRepository.count()).isEqualTo(countBefore + 1);

        Payment payment = paymentRepository.findAllByOrderByPaymentDateDesc().get(0);
        assertThat(payment.getAmount()).isEqualByComparingTo("150.00");
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Verify linked invoice is now marked PAID
        Invoice updatedInvoice = invoiceRepository.findById(testInvoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(updatedInvoice.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("Admin can refund payment")
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    void shouldRefundPayment() throws Exception {
        Payment payment = paymentRepository.save(Payment.builder()
                .transactionId("TX-REFUND-001")
                .customer(testCustomer)
                .amount(new BigDecimal("99.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.SUCCESS)
                .build());

        mockMvc.perform(post("/dashboard/payments/" + payment.getId() + "/refund").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/payments"))
                .andExpect(flash().attributeExists("successMessage"));

        Payment refunded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
