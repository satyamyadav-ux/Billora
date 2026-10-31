package com.billora.modules.product.repository;

import com.billora.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findAllByOrderByCreatedAtDesc();

    Optional<Product> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
