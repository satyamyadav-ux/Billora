package com.billora.modules.auth.service;

import com.billora.common.exception.BusinessException;
import com.billora.modules.auth.dto.RegisterRequestDto;
import com.billora.modules.auth.entity.User;
import com.billora.modules.auth.enums.Role;
import com.billora.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Check whether an email is already registered (case-insensitive).
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmailIgnoreCase(email.trim());
    }

    /**
     * Register a new Customer account with BCrypt password hashing.
     */
    @Transactional
    public User registerCustomer(RegisterRequestDto dto) {
        String cleanEmail = dto.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessException("An account with this email address already exists.");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }

        User newUser = User.builder()
                .fullName(dto.getFullName().trim())
                .email(cleanEmail)
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.CUSTOMER)
                .companyName(dto.getCompanyName() != null && !dto.getCompanyName().isBlank() ? dto.getCompanyName().trim() : null)
                .enabled(true)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New customer registered successfully: {} with role {}", savedUser.getEmail(), savedUser.getRole());
        return savedUser;
    }

    /**
     * Helper to seed demo users on startup if they don't already exist.
     */
    @Transactional
    public void seedDefaultUsers() {
        seedUserIfAbsent("admin@billora.com", "Billora Admin", "Admin@123", Role.ADMIN, "Billora HQ");
        seedUserIfAbsent("staff@billora.com", "Operations Staff", "Staff@123", Role.STAFF, "Billora Ops");
        seedUserIfAbsent("customer@billora.com", "Acme Corporation", "Customer@123", Role.CUSTOMER, "Acme Corp");
    }

    private void seedUserIfAbsent(String email, String fullName, String rawPassword, Role role, String company) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            User user = User.builder()
                    .email(email.toLowerCase())
                    .fullName(fullName)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .companyName(company)
                    .enabled(true)
                    .emailVerified(true)
                    .build();
            userRepository.save(user);
            log.info("Seeded initial user: {} [{}]", email, role);
        }
    }
}
