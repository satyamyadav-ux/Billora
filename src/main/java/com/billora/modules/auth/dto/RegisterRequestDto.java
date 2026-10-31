package com.billora.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 180, message = "Email cannot exceed 180 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters long and contain at least one letter and one number"
    )
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @Size(max = 120, message = "Company name cannot exceed 120 characters")
    private String companyName;

    @AssertTrue(message = "You must agree to the Terms of Service and Privacy Policy")
    private boolean termsAccepted;
}
