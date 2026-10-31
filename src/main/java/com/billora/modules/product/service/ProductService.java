package com.billora.modules.product.service;

import com.billora.common.exception.BusinessException;
import com.billora.common.exception.ResourceNotFoundException;
import com.billora.modules.product.dto.ProductRequestDto;
import com.billora.modules.product.dto.ProductVariantDto;
import com.billora.modules.product.entity.Product;
import com.billora.modules.product.entity.ProductVariant;
import com.billora.modules.product.repository.ProductRepository;
import com.billora.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product saveProduct(ProductRequestDto dto) {
        String cleanCode = generateOrCleanCode(dto.getCode(), dto.getName());

        if (dto.getId() == null) {
            // New Product: check uniqueness of code
            if (productRepository.existsByCodeIgnoreCase(cleanCode)) {
                cleanCode = cleanCode + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }

            Product product = Product.builder()
                    .name(dto.getName().trim())
                    .code(cleanCode)
                    .productType(dto.getProductType() != null && !dto.getProductType().isBlank() ? dto.getProductType().trim() : "SaaS")
                    .salesPrice(dto.getSalesPrice() != null ? dto.getSalesPrice() : BigDecimal.ZERO)
                    .costPrice(dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO)
                    .category(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory().trim() : "General")
                    .description(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription().trim() : null)
                    .active(dto.isActive())
                    .build();

            // Check if initial variant is requested
            if (dto.getInitialVariantName() != null && !dto.getInitialVariantName().trim().isBlank()) {
                BigDecimal addPrice = dto.getInitialVariantAdditionalPrice() != null ? dto.getInitialVariantAdditionalPrice() : BigDecimal.ZERO;
                ProductVariant variant = ProductVariant.builder()
                        .variantName(dto.getInitialVariantName().trim())
                        .sku(cleanCode + "-VAR1")
                        .additionalPrice(addPrice)
                        .active(true)
                        .build();
                product.addVariant(variant);
            }

            return productRepository.save(product);
        } else {
            // Edit existing product
            Product existing = getProductById(dto.getId());
            if (!existing.getCode().equalsIgnoreCase(cleanCode) && productRepository.existsByCodeIgnoreCase(cleanCode)) {
                throw new BusinessException("Another product with code '" + cleanCode + "' already exists.");
            }
            existing.setName(dto.getName().trim());
            existing.setCode(cleanCode);
            existing.setProductType(dto.getProductType() != null && !dto.getProductType().isBlank() ? dto.getProductType().trim() : "SaaS");
            existing.setSalesPrice(dto.getSalesPrice() != null ? dto.getSalesPrice() : BigDecimal.ZERO);
            existing.setCostPrice(dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO);
            existing.setCategory(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory().trim() : "General");
            existing.setDescription(dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription().trim() : null);
            existing.setActive(dto.isActive());
            return productRepository.save(existing);
        }
    }

    @Transactional
    public ProductVariant addProductVariant(Long productId, ProductVariantDto dto) {
        Product product = getProductById(productId);
        String sku = dto.getSku();
        if (sku == null || sku.trim().isBlank()) {
            sku = product.getCode() + "-V" + (product.getVariants().size() + 1);
        } else {
            sku = sku.trim().toUpperCase();
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantName(dto.getVariantName().trim())
                .sku(sku)
                .additionalPrice(dto.getAdditionalPrice() != null ? dto.getAdditionalPrice() : BigDecimal.ZERO)
                .active(dto.isActive())
                .build();

        return productVariantRepository.save(variant);
    }

    @Transactional
    public void deleteProductVariant(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + variantId));
        productVariantRepository.delete(variant);
    }

    @Transactional
    public Product toggleProductStatus(Long id) {
        Product product = getProductById(id);
        product.setActive(!product.isActive());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
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
        return "PRD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
