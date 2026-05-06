package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/admin")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService service;

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse create(@AuthenticationPrincipal CatalogPrincipal user,
                                  @PathVariable UUID productId,
                                  @RequestBody @Valid CreateVariantRequest req) {
        return service.create(user.bakeryId(), productId, req);
    }

    @GetMapping("/products/{productId}/variants")
    public Page<VariantResponse> list(@AuthenticationPrincipal CatalogPrincipal user,
                                      @PathVariable UUID productId,
                                      @PageableDefault(size = 50) Pageable pageable) {
        return service.listForProduct(user.bakeryId(), productId, pageable);
    }

    @PatchMapping("/variants/{variantId}")
    public VariantResponse update(@AuthenticationPrincipal CatalogPrincipal user,
                                  @PathVariable UUID variantId,
                                  @RequestBody @Valid UpdateVariantRequest req) {
        return service.update(user.bakeryId(), variantId, req);
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CatalogPrincipal user,
                                       @PathVariable UUID variantId) {
        service.delete(user.bakeryId(), variantId);
        return ResponseEntity.noContent().build();
    }
}
