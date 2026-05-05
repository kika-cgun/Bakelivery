package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.model.Product;
import com.piotrcapecki.bakelivery.catalog.repository.CategoryRepository;
import com.piotrcapecki.bakelivery.catalog.repository.ProductRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;
    private final CategoryRepository categoryRepo;

    @Transactional
    public ProductResponse create(UUID bakeryId, CreateProductRequest req) {
        if (repo.existsBySlugAndBakeryId(req.slug(), bakeryId)) {
            throw new ConflictException("Product slug '" + req.slug() + "' already exists");
        }
        if (req.categoryId() != null
                && categoryRepo.findByIdAndBakeryId(req.categoryId(), bakeryId).isEmpty()) {
            throw new IllegalArgumentException("Category " + req.categoryId() + " not found in this bakery");
        }
        Product p = Product.builder()
                .bakeryId(bakeryId)
                .categoryId(req.categoryId())
                .sku(req.sku())
                .slug(req.slug())
                .name(req.name())
                .description(req.description())
                .basePrice(req.basePrice())
                .availableDays(req.availableDays() == null ? (short) 127 : req.availableDays())
                .active(true)
                .build();
        return ProductResponse.of(repo.save(p));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll(UUID bakeryId) {
        return repo.findAllByBakeryIdOrderByNameAsc(bakeryId)
                .stream().map(ProductResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActive(UUID bakeryId) {
        return repo.findAllByBakeryIdAndActiveTrueOrderByNameAsc(bakeryId)
                .stream().map(ProductResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID bakeryId, UUID id) {
        return ProductResponse.of(repo.findByIdAndBakeryId(id, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found")));
    }

    @Transactional
    public ProductResponse update(UUID bakeryId, UUID id, UpdateProductRequest req) {
        Product p = repo.findByIdAndBakeryId(id, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (req.slug() != null && !req.slug().equals(p.getSlug())) {
            if (repo.existsBySlugAndBakeryId(req.slug(), bakeryId)) {
                throw new ConflictException("Product slug '" + req.slug() + "' already exists");
            }
            p.setSlug(req.slug());
        }
        if (req.categoryId() != null) {
            if (categoryRepo.findByIdAndBakeryId(req.categoryId(), bakeryId).isEmpty()) {
                throw new IllegalArgumentException("Category " + req.categoryId() + " not found in this bakery");
            }
            p.setCategoryId(req.categoryId());
        }
        if (req.sku() != null) p.setSku(req.sku());
        if (req.name() != null) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.basePrice() != null) p.setBasePrice(req.basePrice());
        if (req.availableDays() != null) p.setAvailableDays(req.availableDays());
        if (req.active() != null) p.setActive(req.active());
        return ProductResponse.of(repo.save(p));
    }

    @Transactional
    public void softDelete(UUID bakeryId, UUID id) {
        Product p = repo.findByIdAndBakeryId(id, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        p.setActive(false);
        repo.save(p);
    }
}
