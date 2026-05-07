package com.piotrcapecki.bakelivery.dispatching.controller;

import com.piotrcapecki.bakelivery.dispatching.dto.DispatchStopResponse;
import com.piotrcapecki.bakelivery.dispatching.dto.TerritoryResponse;
import com.piotrcapecki.bakelivery.dispatching.service.DispatchStopService;
import com.piotrcapecki.bakelivery.dispatching.service.DriverTerritoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/internal")
@RequiredArgsConstructor
public class InternalDispatchController {

    private final DispatchStopService stopService;
    private final DriverTerritoryService territoryService;

    @GetMapping("/stops")
    public ResponseEntity<List<DispatchStopResponse>> stops(
            @RequestParam UUID bakeryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(stopService.listByDate(bakeryId, date)
                .stream().map(DispatchStopResponse::from).toList());
    }

    @GetMapping("/territories/{driverId}")
    public ResponseEntity<List<TerritoryResponse>> territories(
            @PathVariable UUID driverId,
            @RequestParam UUID bakeryId) {
        return ResponseEntity.ok(territoryService.listByDriver(bakeryId, driverId)
                .stream().map(TerritoryResponse::from).toList());
    }
}
