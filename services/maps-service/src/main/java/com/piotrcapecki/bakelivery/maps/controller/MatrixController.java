package com.piotrcapecki.bakelivery.maps.controller;

import com.piotrcapecki.bakelivery.maps.dto.MatrixRequest;
import com.piotrcapecki.bakelivery.maps.dto.MatrixResponse;
import com.piotrcapecki.bakelivery.maps.service.OsrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/maps")
@RequiredArgsConstructor
public class MatrixController {

    private final OsrmService osrmService;

    @PostMapping("/matrix")
    public ResponseEntity<MatrixResponse> matrix(@Valid @RequestBody MatrixRequest request) {
        return ResponseEntity.ok(osrmService.matrix(request));
    }
}
