package com.piotrcapecki.bakelivery.auth.controller;

import com.piotrcapecki.bakelivery.auth.dto.CreateBakeryRequest;
import com.piotrcapecki.bakelivery.auth.model.Bakery;
import com.piotrcapecki.bakelivery.auth.service.BakeryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/platform/bakeries")
@RequiredArgsConstructor
public class BakeryController {

    private final BakeryService bakeryService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BakerySummary> createBakery(@Valid @RequestBody CreateBakeryRequest req) {
        Bakery bakery = bakeryService.createBakeryWithFirstAdmin(req);
        return ResponseEntity.status(201).body(new BakerySummary(bakery.getId(), bakery.getName(), bakery.getSlug()));
    }

    public record BakerySummary(UUID id, String name, String slug) {}
}
