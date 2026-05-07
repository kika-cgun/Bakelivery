package com.piotrcapecki.bakelivery.dispatching.controller;

import com.piotrcapecki.bakelivery.dispatching.dto.CreateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.FixedPointResponse;
import com.piotrcapecki.bakelivery.dispatching.dto.UpdateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.security.DispatchPrincipal;
import com.piotrcapecki.bakelivery.dispatching.service.FixedDeliveryPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/admin/fixed-points")
@RequiredArgsConstructor
public class FixedDeliveryPointController {

    private final FixedDeliveryPointService service;

    @PostMapping
    public ResponseEntity<FixedPointResponse> create(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @Valid @RequestBody CreateFixedPointRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FixedPointResponse.from(service.create(actor.bakeryId(), req)));
    }

    @GetMapping
    public ResponseEntity<List<FixedPointResponse>> list(@AuthenticationPrincipal DispatchPrincipal actor) {
        return ResponseEntity.ok(service.list(actor.bakeryId()).stream()
                .map(FixedPointResponse::from).toList());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FixedPointResponse> update(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @PathVariable UUID id,
            @RequestBody UpdateFixedPointRequest req) {
        return ResponseEntity.ok(FixedPointResponse.from(service.update(actor.bakeryId(), id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @PathVariable UUID id) {
        service.deactivate(actor.bakeryId(), id);
        return ResponseEntity.noContent().build();
    }
}
