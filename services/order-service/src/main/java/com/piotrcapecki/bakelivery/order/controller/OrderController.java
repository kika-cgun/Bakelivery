package com.piotrcapecki.bakelivery.order.controller;

import com.piotrcapecki.bakelivery.order.dto.*;
import com.piotrcapecki.bakelivery.order.security.OrderPrincipal;
import com.piotrcapecki.bakelivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@AuthenticationPrincipal OrderPrincipal principal,
                                    @RequestBody @Valid CreateOrderRequest req,
                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @RequestHeader("Authorization") String authorization) {
        return service.placeOrder(principal, req, idempotencyKey, authorization);
    }

    @GetMapping("/api/orders")
    public Page<OrderResponse> listMyOrders(@AuthenticationPrincipal OrderPrincipal principal,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return service.listForCustomer(principal.userId(), principal.bakeryId(), pageable);
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse getMyOrder(@AuthenticationPrincipal OrderPrincipal principal,
                                    @PathVariable UUID id) {
        return service.getForCustomer(id, principal.userId(), principal.bakeryId());
    }

    @GetMapping("/api/orders/admin")
    public Page<OrderResponse> listAllOrders(@AuthenticationPrincipal OrderPrincipal principal,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return service.listForAdmin(principal.bakeryId(), pageable);
    }

    @GetMapping("/api/orders/admin/{id}")
    public OrderResponse getOrderForAdmin(@AuthenticationPrincipal OrderPrincipal principal,
                                          @PathVariable UUID id) {
        return service.getForAdmin(id, principal.bakeryId());
    }

    @PatchMapping("/api/orders/admin/{id}/status")
    public OrderResponse updateStatus(@AuthenticationPrincipal OrderPrincipal principal,
                                      @PathVariable UUID id,
                                      @RequestBody @Valid UpdateOrderStatusRequest req) {
        return service.updateStatus(id, principal.bakeryId(), req);
    }
}
