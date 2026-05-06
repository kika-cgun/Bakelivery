package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final ProductVariantService variantService;
    private final ProductMediaService mediaService;

    @GetMapping("/categories")
    public Page<CategoryResponse> categories(@AuthenticationPrincipal CatalogPrincipal user,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return categoryService.list(user.bakeryId(), pageable);
    }

    @GetMapping("/products")
    public Page<ProductResponse> activeProducts(@AuthenticationPrincipal CatalogPrincipal user,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return productService.listActive(user.bakeryId(), pageable);
    }

    @GetMapping("/products/{id}")
    public ProductDetailResponse productDetail(@AuthenticationPrincipal CatalogPrincipal user,
                                               @PathVariable UUID id) {
        ProductResponse p = productService.get(user.bakeryId(), id);
        List<VariantResponse> variants = variantService.listForProduct(user.bakeryId(), id);
        List<MediaResponse> media = mediaService.list(user.bakeryId(), id);
        return new ProductDetailResponse(p, variants, media);
    }
}
