package com.billora.modules.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDto {

    private Long id;

    @NotBlank(message = "Customer name is required.")
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters.")
    private String name;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Please provide a valid email address.")
    @Size(max = 180, message = "Email cannot exceed 180 characters.")
    private String email;

    @Size(max = 150, message = "Company name cannot exceed 150 characters.")
    private String company;

    @Size(max = 40, message = "Phone number cannot exceed 40 characters.")
    private String phone;

    @Size(max = 255, message = "Address cannot exceed 255 characters.")
    private String address;

    @Builder.Default
    private boolean active = true;
}
