package com.piotrcapecki.bakelivery.maps.controller;

import com.piotrcapecki.bakelivery.maps.dto.GeocodeRequest;
import com.piotrcapecki.bakelivery.maps.dto.GeocodeResponse;
import com.piotrcapecki.bakelivery.maps.service.GeocodingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/maps")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @PostMapping("/geocode")
    public ResponseEntity<GeocodeResponse> geocode(@Valid @RequestBody GeocodeRequest request) {
        return ResponseEntity.ok(geocodingService.geocode(request));
    }
}
