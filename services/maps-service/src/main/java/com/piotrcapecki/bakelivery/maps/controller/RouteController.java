package com.piotrcapecki.bakelivery.maps.controller;

import com.piotrcapecki.bakelivery.maps.dto.RouteRequest;
import com.piotrcapecki.bakelivery.maps.dto.RouteResponse;
import com.piotrcapecki.bakelivery.maps.service.OsrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/maps")
@RequiredArgsConstructor
public class RouteController {

    private final OsrmService osrmService;

    @PostMapping("/route")
    public ResponseEntity<RouteResponse> route(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(osrmService.route(request));
    }
}
