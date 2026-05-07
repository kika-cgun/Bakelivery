package com.piotrcapecki.bakelivery.order.client;

import com.piotrcapecki.bakelivery.order.dto.catalog.ProductDetailResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

@HttpExchange
public interface CatalogClient {

    @GetExchange("/api/catalog/products/{productId}")
    ProductDetailResponse getProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-Bakery-Id") String bakeryId,
            @RequestHeader("Authorization") String auth);
}
