package com.piotrcapecki.bakelivery.order.client;

import com.piotrcapecki.bakelivery.order.dto.catalog.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "catalog-service", url = "${catalog.service.url:http://localhost:8083}")
public interface CatalogClient {

    @GetMapping("/api/catalog/products/{productId}")
    ProductDetailResponse getProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-Bakery-Id") String bakeryId,
            @RequestHeader("Authorization") String auth);
}
