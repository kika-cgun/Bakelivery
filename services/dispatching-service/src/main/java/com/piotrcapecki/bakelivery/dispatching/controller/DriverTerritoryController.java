package com.piotrcapecki.bakelivery.dispatching.controller;

import com.piotrcapecki.bakelivery.dispatching.dto.CreateTerritoryRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.TerritoryResponse;
import com.piotrcapecki.bakelivery.dispatching.security.DispatchPrincipal;
import com.piotrcapecki.bakelivery.dispatching.service.DriverTerritoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/admin/territories")
@RequiredArgsConstructor
public class DriverTerritoryController {

    private final DriverTerritoryService service;

    @PostMapping
    public ResponseEntity<TerritoryResponse> create(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @Valid @RequestBody CreateTerritoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TerritoryResponse.from(service.create(actor.bakeryId(), req)));
    }

    @GetMapping
    public ResponseEntity<List<TerritoryResponse>> list(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @RequestParam UUID driverId) {
        return ResponseEntity.ok(service.listByDriver(actor.bakeryId(), driverId)
                .stream().map(TerritoryResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @PathVariable UUID id) {
        service.delete(actor.bakeryId(), id);
        return ResponseEntity.noContent().build();
    }
}
