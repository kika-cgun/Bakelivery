package com.piotrcapecki.bakelivery.catalog.repository;

import com.piotrcapecki.bakelivery.catalog.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByBakeryIdOrderBySortOrderAscNameAsc(UUID bakeryId);
    Page<Category> findAllByBakeryId(UUID bakeryId, Pageable pageable);
    Optional<Category> findByIdAndBakeryId(UUID id, UUID bakeryId);
    Optional<Category> findBySlugAndBakeryId(String slug, UUID bakeryId);
    boolean existsBySlugAndBakeryId(String slug, UUID bakeryId);
}
