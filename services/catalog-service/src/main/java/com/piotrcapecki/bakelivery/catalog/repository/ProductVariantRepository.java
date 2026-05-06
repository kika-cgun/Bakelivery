package com.piotrcapecki.bakelivery.catalog.repository;

import com.piotrcapecki.bakelivery.catalog.model.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findAllByProductIdOrderBySortOrderAscNameAsc(UUID productId);
    Page<ProductVariant> findAllByProductId(UUID productId, Pageable pageable);
    Optional<ProductVariant> findByIdAndBakeryId(UUID id, UUID bakeryId);
    boolean existsByProductIdAndName(UUID productId, String name);
}
