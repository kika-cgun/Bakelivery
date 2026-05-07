package com.piotrcapecki.bakelivery.dispatching.controller;

import com.piotrcapecki.bakelivery.dispatching.dto.AssignDriverRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.DispatchStopResponse;
import com.piotrcapecki.bakelivery.dispatching.dto.UpdateStatusRequest;
import com.piotrcapecki.bakelivery.dispatching.security.DispatchPrincipal;
import com.piotrcapecki.bakelivery.dispatching.service.DispatchStopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/admin/stops")
@RequiredArgsConstructor
public class DispatchStopController {

    private final DispatchStopService service;

    @GetMapping
    public ResponseEntity<List<DispatchStopResponse>> list(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.listByDate(actor.bakeryId(), date)
                .stream().map(DispatchStopResponse::from).toList());
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<DispatchStopResponse> assign(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @PathVariable UUID id,
            @Valid @RequestBody AssignDriverRequest req) {
        return ResponseEntity.ok(DispatchStopResponse.from(service.assignDriver(actor.bakeryId(), id, req)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DispatchStopResponse> status(
            @AuthenticationPrincipal DispatchPrincipal actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest req) {
        return ResponseEntity.ok(DispatchStopResponse.from(service.updateStatus(actor.bakeryId(), id, req)));
    }
}
