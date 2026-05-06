package com.piotrcapecki.bakelivery.catalog.repository;

import com.piotrcapecki.bakelivery.catalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByBakeryIdOrderByNameAsc(UUID bakeryId);
    List<Product> findAllByBakeryIdAndActiveTrueOrderByNameAsc(UUID bakeryId);
    Page<Product> findAllByBakeryId(UUID bakeryId, Pageable pageable);
    Page<Product> findAllByBakeryIdAndActiveTrue(UUID bakeryId, Pageable pageable);
    Optional<Product> findByIdAndBakeryId(UUID id, UUID bakeryId);
    boolean existsBySlugAndBakeryId(String slug, UUID bakeryId);
}
