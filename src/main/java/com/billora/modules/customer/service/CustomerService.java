package com.billora.modules.customer.service;

import com.billora.common.exception.BusinessException;
import com.billora.common.exception.ResourceNotFoundException;
import com.billora.modules.customer.dto.CustomerRequestDto;
import com.billora.modules.customer.entity.Customer;
import com.billora.modules.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllActiveCustomers() {
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    @Transactional
    public Customer saveCustomer(CustomerRequestDto dto) {
        String cleanEmail = dto.getEmail().trim().toLowerCase();

        if (dto.getId() == null) {
            // New Customer
            if (customerRepository.existsByEmailIgnoreCase(cleanEmail)) {
                throw new BusinessException("A customer with email '" + cleanEmail + "' already exists.");
            }
            Customer customer = Customer.builder()
                    .name(dto.getName().trim())
                    .email(cleanEmail)
                    .company(dto.getCompany() != null && !dto.getCompany().isBlank() ? dto.getCompany().trim() : null)
                    .phone(dto.getPhone() != null && !dto.getPhone().isBlank() ? dto.getPhone().trim() : null)
                    .address(dto.getAddress() != null && !dto.getAddress().isBlank() ? dto.getAddress().trim() : null)
                    .active(dto.isActive())
                    .build();
            return customerRepository.save(customer);
        } else {
            // Update Customer
            Customer existing = getCustomerById(dto.getId());
            if (!existing.getEmail().equalsIgnoreCase(cleanEmail) && customerRepository.existsByEmailIgnoreCase(cleanEmail)) {
                throw new BusinessException("Another customer with email '" + cleanEmail + "' already exists.");
            }
            existing.setName(dto.getName().trim());
            existing.setEmail(cleanEmail);
            existing.setCompany(dto.getCompany() != null && !dto.getCompany().isBlank() ? dto.getCompany().trim() : null);
            existing.setPhone(dto.getPhone() != null && !dto.getPhone().isBlank() ? dto.getPhone().trim() : null);
            existing.setAddress(dto.getAddress() != null && !dto.getAddress().isBlank() ? dto.getAddress().trim() : null);
            existing.setActive(dto.isActive());
            return customerRepository.save(existing);
        }
    }

    @Transactional
    public Customer getOrCreateCustomer(String name, String email, String company) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be empty.");
        }
        String cleanEmail = email.trim().toLowerCase();
        return customerRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseGet(() -> customerRepository.save(
                        Customer.builder()
                                .name(name != null && !name.isBlank() ? name.trim() : cleanEmail)
                                .email(cleanEmail)
                                .company(company != null && !company.isBlank() ? company.trim() : null)
                                .active(true)
                                .build()
                ));
    }

    @Transactional
    public Customer toggleCustomerStatus(Long id) {
        Customer customer = getCustomerById(id);
        customer.setActive(!customer.isActive());
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public long countActiveCustomers() {
        return customerRepository.findByActiveTrueOrderByNameAsc().size();
    }
}
