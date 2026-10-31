package com.billora.modules.auth;

import com.billora.modules.auth.dto.RegisterRequestDto;
import com.billora.modules.auth.entity.User;
import com.billora.modules.auth.enums.Role;
import com.billora.modules.auth.repository.UserRepository;
import com.billora.modules.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService.seedDefaultUsers();
    }

    @Test
    @DisplayName("LoginPage loads successfully with HTTP 200")
    void testLoginPageLoads() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome Back")));
    }

    @Test
    @DisplayName("RegisterPage loads successfully with HTTP 200")
    void testRegisterPageLoads() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create Customer Account")));
    }

    @Test
    @DisplayName("Unauthenticated user is redirected to /login when requesting protected /dashboard")
    void testUnauthenticatedRedirect() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Customer signup creates user in PostgreSQL with BCrypt password and CUSTOMER role")
    void testCustomerRegistrationSuccess() throws Exception {
        String testEmail = "newcustomer_" + System.currentTimeMillis() + "@example.com";

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Alex Mercer")
                        .param("email", testEmail)
                        .param("password", "SecurePass123")
                        .param("confirmPassword", "SecurePass123")
                        .param("companyName", "Mercer Cloud Solutions")
                        .param("termsAccepted", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        User savedUser = userRepository.findByEmailIgnoreCase(testEmail).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getFullName()).isEqualTo("Alex Mercer");
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(passwordEncoder.matches("SecurePass123", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Customer signup rejects duplicate email")
    void testDuplicateEmailRejection() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Duplicate Tester")
                        .param("email", "customer@billora.com") // Already seeded
                        .param("password", "SecurePass123")
                        .param("confirmPassword", "SecurePass123")
                        .param("termsAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registerDto", "email"));
    }

    @Test
    @DisplayName("Customer signup rejects weak password (less than 8 chars or no digits)")
    void testWeakPasswordRejection() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Weak Pass")
                        .param("email", "weakpass_" + System.currentTimeMillis() + "@example.com")
                        .param("password", "weak")
                        .param("confirmPassword", "weak")
                        .param("termsAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registerDto", "password"));
    }

    @Test
    @DisplayName("Customer signup rejects mismatched passwords")
    void testPasswordMismatchRejection() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Mismatch Pass")
                        .param("email", "mismatch_" + System.currentTimeMillis() + "@example.com")
                        .param("password", "SecurePass123")
                        .param("confirmPassword", "DifferentPass123")
                        .param("termsAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registerDto", "confirmPassword"));
    }

    @Test
    @DisplayName("Form login succeeds for seeded ADMIN user and redirects to /dashboard")
    void testAdminLoginSuccess() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "admin@billora.com").password("password", "Admin@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Form login succeeds for seeded CUSTOMER user and redirects to /dashboard")
    void testCustomerLoginSuccess() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "customer@billora.com").password("password", "Customer@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Form login fails for invalid password and redirects to /login?error=true")
    void testLoginFailure() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", "admin@billora.com").password("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @WithMockUser(username = "customer@billora.com", roles = {"CUSTOMER"})
    @DisplayName("Role-based: CUSTOMER accessing /dashboard/settings is forbidden with HTTP 403")
    void testCustomerForbiddenFromSettings() throws Exception {
        mockMvc.perform(get("/dashboard/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    @DisplayName("Role-based: ADMIN accessing /dashboard/settings loads HTTP 200")
    void testAdminAllowedInSettings() throws Exception {
        mockMvc.perform(get("/dashboard/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/settings"));
    }

    @Test
    @WithMockUser(username = "admin@billora.com", roles = {"ADMIN"})
    @DisplayName("Logout invalidates session and redirects to /login?logout=true")
    void testLogoutSuccess() throws Exception {
        mockMvc.perform(logout("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
