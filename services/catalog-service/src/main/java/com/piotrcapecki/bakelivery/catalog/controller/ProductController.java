package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@AuthenticationPrincipal CatalogPrincipal user,
                                  @RequestBody @Valid CreateProductRequest req) {
        return service.create(user.bakeryId(), req);
    }

    @GetMapping
    public List<ProductResponse> listAll(@AuthenticationPrincipal CatalogPrincipal user) {
        return service.listAll(user.bakeryId());
    }

    @GetMapping("/{id}")
    public ProductResponse get(@AuthenticationPrincipal CatalogPrincipal user,
                               @PathVariable UUID id) {
        return service.get(user.bakeryId(), id);
    }

    @PatchMapping("/{id}")
    public ProductResponse update(@AuthenticationPrincipal CatalogPrincipal user,
                                  @PathVariable UUID id,
                                  @RequestBody @Valid UpdateProductRequest req) {
        return service.update(user.bakeryId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@AuthenticationPrincipal CatalogPrincipal user,
                                           @PathVariable UUID id) {
        service.softDelete(user.bakeryId(), id);
        return ResponseEntity.noContent().build();
    }
}
