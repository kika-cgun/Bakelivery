package com.piotrcapecki.bakelivery.order.repository;

import com.piotrcapecki.bakelivery.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findAllByCustomerIdAndBakeryIdOrderByCreatedAtDesc(UUID customerId, UUID bakeryId, Pageable pageable);
    Optional<Order> findByIdAndCustomerIdAndBakeryId(UUID id, UUID customerId, UUID bakeryId);
    Page<Order> findAllByBakeryIdOrderByCreatedAtDesc(UUID bakeryId, Pageable pageable);
    Optional<Order> findByIdAndBakeryId(UUID id, UUID bakeryId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
