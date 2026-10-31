package com.billora.modules.subscription.service;

import com.billora.common.exception.BusinessException;
import com.billora.common.exception.ResourceNotFoundException;
import com.billora.modules.product.entity.Product;
import com.billora.modules.product.service.ProductService;
import com.billora.modules.subscription.dto.PlanRequestDto;
import com.billora.modules.subscription.entity.Plan;
import com.billora.modules.subscription.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<Plan> getAllPlans() {
        return planRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Plan> getAllActivePlans() {
        return planRepository.findByActiveTrueOrderByPriceAsc();
    }

    @Transactional(readOnly = true)
    public Plan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing plan not found with id: " + id));
    }

    @Transactional
    public Plan savePlan(PlanRequestDto dto) {
        String cleanCode = generateOrCleanCode(dto.getCode(), dto.getName());

        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("Plan End Date cannot be before Start Date.");
        }

        Product product = null;
        if (dto.getProductId() != null) {
            product = productService.getProductById(dto.getProductId());
        }

        if (dto.getId() == null) {
            // New Plan
            if (planRepository.existsByCodeIgnoreCase(cleanCode)) {
                cleanCode = cleanCode + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }
            Plan newPlan = Plan.builder()
                    .name(dto.getName().trim())
                    .code(cleanCode)
                    .product(product)
                    .description(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription().trim() : null)
                    .price(dto.getPrice())
                    .billingCycle(dto.getBillingCycle())
                    .minQuantity(dto.getMinQuantity() > 0 ? dto.getMinQuantity() : 1)
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .trialDays(dto.getTrialDays())
                    .highlight(dto.getHighlight() != null && !dto.getHighlight().isBlank() ? dto.getHighlight().trim() : null)
                    .active(dto.isActive())
                    .build();
            return planRepository.save(newPlan);
        } else {
            // Update Plan
            Plan existing = getPlanById(dto.getId());
            if (!existing.getCode().equalsIgnoreCase(cleanCode) && planRepository.existsByCodeIgnoreCase(cleanCode)) {
                throw new BusinessException("Another plan with code '" + cleanCode + "' already exists.");
            }
            existing.setName(dto.getName().trim());
            existing.setCode(cleanCode);
            existing.setProduct(product);
            existing.setDescription(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription().trim() : null);
            existing.setPrice(dto.getPrice());
            existing.setBillingCycle(dto.getBillingCycle());
            existing.setMinQuantity(dto.getMinQuantity() > 0 ? dto.getMinQuantity() : 1);
            existing.setStartDate(dto.getStartDate());
            existing.setEndDate(dto.getEndDate());
            existing.setTrialDays(dto.getTrialDays());
            existing.setHighlight(dto.getHighlight() != null && !dto.getHighlight().isBlank() ? dto.getHighlight().trim() : null);
            existing.setActive(dto.isActive());
            return planRepository.save(existing);
        }
    }

    @Transactional
    public Plan togglePlanStatus(Long id) {
        Plan plan = getPlanById(id);
        plan.setActive(!plan.isActive());
        return planRepository.save(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        Plan plan = getPlanById(id);
        planRepository.delete(plan);
    }

    private String generateOrCleanCode(String code, String name) {
        if (code != null && !code.trim().isBlank()) {
            return code.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "");
        }
        if (name != null && !name.trim().isBlank()) {
            String slug = name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
            if (slug.length() > 20) {
                slug = slug.substring(0, 20);
            }
            if (slug.endsWith("-")) {
                slug = slug.substring(0, slug.length() - 1);
            }
            return slug;
        }
        return "PLAN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
