package com.piotrcapecki.bakelivery.catalog.controller;

import com.piotrcapecki.bakelivery.catalog.dto.MediaResponse;
import com.piotrcapecki.bakelivery.catalog.security.CatalogPrincipal;
import com.piotrcapecki.bakelivery.catalog.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/admin")
@RequiredArgsConstructor
public class ProductMediaController {

    private final ProductMediaService service;

    @PostMapping(path = "/products/{productId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@AuthenticationPrincipal CatalogPrincipal user,
                                @PathVariable UUID productId,
                                @RequestPart("file") MultipartFile file,
                                @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
                                @RequestParam(value = "primary", defaultValue = "false") boolean primary)
            throws IOException {
        return service.upload(user.bakeryId(), productId, file, sortOrder, primary);
    }

    @GetMapping("/products/{productId}/media")
    public List<MediaResponse> list(@AuthenticationPrincipal CatalogPrincipal user,
                                    @PathVariable UUID productId) {
        return service.list(user.bakeryId(), productId);
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CatalogPrincipal user,
                                       @PathVariable UUID mediaId) {
        service.delete(user.bakeryId(), mediaId);
        return ResponseEntity.noContent().build();
    }
}
