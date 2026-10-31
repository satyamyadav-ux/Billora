package com.billora.modules.product.repository;

import com.billora.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    List<ProductVariant> findByProductIdAndActiveTrueOrderByIdAsc(Long productId);
}
