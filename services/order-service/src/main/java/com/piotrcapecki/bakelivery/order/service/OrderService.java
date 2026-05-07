package com.piotrcapecki.bakelivery.order.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.order.client.CatalogClient;
import com.piotrcapecki.bakelivery.order.config.RabbitConfig;
import com.piotrcapecki.bakelivery.order.dto.*;
import com.piotrcapecki.bakelivery.order.dto.catalog.ProductDetailResponse;
import com.piotrcapecki.bakelivery.order.dto.catalog.VariantResponse;
import com.piotrcapecki.bakelivery.order.model.Order;
import com.piotrcapecki.bakelivery.order.model.OrderItem;
import com.piotrcapecki.bakelivery.order.model.OrderStatus;
import com.piotrcapecki.bakelivery.order.repository.OrderRepository;
import com.piotrcapecki.bakelivery.order.security.OrderPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CatalogClient catalogClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponse placeOrder(OrderPrincipal principal, CreateOrderRequest req, String idempotencyKey, String bearerToken) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.items()) {
            ProductDetailResponse detail;
            try {
                detail = catalogClient.getProduct(
                        itemReq.productId(),
                        principal.bakeryId().toString(),
                        bearerToken);
            } catch (HttpClientErrorException.NotFound e) {
                throw new NotFoundException("Product not found: " + itemReq.productId());
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new IllegalArgumentException("Catalog service error: " + e.getStatusCode());
            }

            if (!detail.product().active()) {
                throw new IllegalArgumentException("Product is not active: " + itemReq.productId());
            }

            BigDecimal unitPrice = detail.product().basePrice();
            String variantName = null;

            if (itemReq.variantId() != null) {
                VariantResponse variant = detail.variants().stream()
                        .filter(v -> v.id().equals(itemReq.variantId()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Variant not found: " + itemReq.variantId()));
                unitPrice = unitPrice.add(variant.priceDelta());
                variantName = variant.name();
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(lineTotal);

            items.add(OrderItem.builder()
                    .bakeryId(principal.bakeryId())
                    .productId(itemReq.productId())
                    .productName(detail.product().name())
                    .variantId(itemReq.variantId())
                    .variantName(variantName)
                    .unitPrice(unitPrice)
                    .quantity(itemReq.quantity())
                    .lineTotal(lineTotal)
                    .build());
        }

        Order order = Order.builder()
                .bakeryId(principal.bakeryId())
                .customerId(principal.userId())
                .totalAmount(total)
                .deliveryAddressId(req.deliveryAddressId())
                .deliveryAddress(req.deliveryAddress())
                .notes(req.notes())
                .idempotencyKey(idempotencyKey)
                .build();

        order.getItems().addAll(items);
        items.forEach(i -> i.setOrder(order));

        Order saved;
        try {
            saved = orderRepo.saveAndFlush(order);
        } catch (DataIntegrityViolationException e) {
            return orderRepo.findByIdempotencyKey(idempotencyKey)
                    .map(OrderResponse::of)
                    .orElseThrow(() -> new IllegalArgumentException("Order conflict"));
        }

        final OrderPlacedEvent event = new OrderPlacedEvent(
                saved.getId(),
                saved.getBakeryId(),
                saved.getCustomerId(),
                principal.email(),
                saved.getDeliveryAddress(),
                saved.getTotalAmount(),
                saved.getItems().stream().map(OrderItemResponse::of).toList(),
                OffsetDateTime.now());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_ORDER_PLACED, event);
            }
        });

        return OrderResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listForCustomer(UUID customerId, UUID bakeryId, Pageable pageable) {
        return orderRepo.findAllByCustomerIdAndBakeryIdOrderByCreatedAtDesc(customerId, bakeryId, pageable)
                .map(OrderResponse::of);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForCustomer(UUID id, UUID customerId, UUID bakeryId) {
        return OrderResponse.of(
                orderRepo.findByIdAndCustomerIdAndBakeryId(id, customerId, bakeryId)
                        .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listForAdmin(UUID bakeryId, Pageable pageable) {
        return orderRepo.findAllByBakeryIdOrderByCreatedAtDesc(bakeryId, pageable)
                .map(OrderResponse::of);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForAdmin(UUID id, UUID bakeryId) {
        return OrderResponse.of(
                orderRepo.findByIdAndBakeryId(id, bakeryId)
                        .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, UUID bakeryId, UpdateOrderStatusRequest req) {
        Order order = orderRepo.findByIdAndBakeryId(id, bakeryId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStatus(OrderStatus.valueOf(req.status()));
        return OrderResponse.of(orderRepo.save(order));
    }
}
