package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.dto.*;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal CatalogPrincipal user,
                                   @RequestBody @Valid CreateCategoryRequest req) {
        return service.create(user.bakeryId(), req);
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal CatalogPrincipal user) {
        return service.list(user.bakeryId());
    }

    @PatchMapping("/{id}")
    public CategoryResponse update(@AuthenticationPrincipal CatalogPrincipal user,
                                   @PathVariable UUID id,
                                   @RequestBody @Valid UpdateCategoryRequest req) {
        return service.update(user.bakeryId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CatalogPrincipal user,
                                       @PathVariable UUID id) {
        service.delete(user.bakeryId(), id);
        return ResponseEntity.noContent().build();
    }
}
