package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.model.Category;
import com.piotrcapecki.bakelivery.catalog.repository.CategoryRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repo;

    @Transactional
    public CategoryResponse create(UUID bakeryId, CreateCategoryRequest req) {
        if (repo.existsBySlugAndBakeryId(req.slug(), bakeryId)) {
            throw new ConflictException("Category with slug '" + req.slug() + "' already exists");
        }
        Category c = Category.builder()
                .bakeryId(bakeryId)
                .name(req.name())
                .slug(req.slug())
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .build();
        return CategoryResponse.of(repo.save(c));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID bakeryId) {
        return repo.findAllByBakeryIdOrderBySortOrderAscNameAsc(bakeryId)
                .stream().map(CategoryResponse::of).toList();
    }

    @Transactional
    public CategoryResponse update(UUID bakeryId, UUID id, UpdateCategoryRequest req) {
        Category c = repo.findByIdAndBakeryId(id, bakeryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (req.slug() != null && !req.slug().equals(c.getSlug())) {
            if (repo.existsBySlugAndBakeryId(req.slug(), bakeryId)) {
                throw new ConflictException("Category with slug '" + req.slug() + "' already exists");
            }
            c.setSlug(req.slug());
        }
        if (req.name() != null) c.setName(req.name());
        if (req.sortOrder() != null) c.setSortOrder(req.sortOrder());
        return CategoryResponse.of(repo.save(c));
    }

    @Transactional
    public void delete(UUID bakeryId, UUID id) {
        Category c = repo.findByIdAndBakeryId(id, bakeryId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        repo.delete(c);
    }
}
