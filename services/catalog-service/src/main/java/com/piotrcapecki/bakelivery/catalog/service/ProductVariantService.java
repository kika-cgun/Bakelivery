package com.piotrcapecki.bakelivery.catalog.service;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.model.Product;
import com.piotrcapecki.bakelivery.catalog.model.ProductVariant;
import com.piotrcapecki.bakelivery.catalog.repository.ProductRepository;
import com.piotrcapecki.bakelivery.catalog.repository.ProductVariantRepository;
import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;

    @Transactional
    public VariantResponse create(UUID bakeryId, UUID productId, CreateVariantRequest req) {
        Product p = productRepo.findByIdAndBakeryId(productId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (variantRepo.existsByProductIdAndName(productId, req.name())) {
            throw new ConflictException("Variant '" + req.name() + "' already exists for this product");
        }
        ProductVariant v = ProductVariant.builder()
                .productId(p.getId())
                .bakeryId(p.getBakeryId())
                .name(req.name())
                .sku(req.sku())
                .priceDelta(req.priceDelta() == null ? BigDecimal.ZERO : req.priceDelta())
                .sortOrder(req.sortOrder() == null ? 0 : req.sortOrder())
                .build();
        return VariantResponse.of(variantRepo.save(v));
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> listForProduct(UUID bakeryId, UUID productId) {
        productRepo.findByIdAndBakeryId(productId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return variantRepo.findAllByProductIdOrderBySortOrderAscNameAsc(productId)
                .stream().map(VariantResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public Page<VariantResponse> listForProduct(UUID bakeryId, UUID productId, Pageable pageable) {
        productRepo.findByIdAndBakeryId(productId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        PageRequest sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("sortOrder").ascending().and(Sort.by("name").ascending()));
        return variantRepo.findAllByProductId(productId, sorted).map(VariantResponse::of);
    }

    @Transactional
    public VariantResponse update(UUID bakeryId, UUID variantId, UpdateVariantRequest req) {
        ProductVariant v = variantRepo.findByIdAndBakeryId(variantId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Variant not found"));
        if (req.name() != null && !req.name().equals(v.getName())) {
            if (variantRepo.existsByProductIdAndName(v.getProductId(), req.name())) {
                throw new ConflictException("Variant '" + req.name() + "' already exists for this product");
            }
            v.setName(req.name());
        }
        if (req.sku() != null) v.setSku(req.sku());
        if (req.priceDelta() != null) v.setPriceDelta(req.priceDelta());
        if (req.sortOrder() != null) v.setSortOrder(req.sortOrder());
        return VariantResponse.of(variantRepo.save(v));
    }

    @Transactional
    public void delete(UUID bakeryId, UUID variantId) {
        ProductVariant v = variantRepo.findByIdAndBakeryId(variantId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Variant not found"));
        variantRepo.delete(v);
    }
}
