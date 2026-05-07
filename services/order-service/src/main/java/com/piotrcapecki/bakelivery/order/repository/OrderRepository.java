package com.piotrcapecki.bakelivery.order.repository;

import com.piotrcapecki.bakelivery.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findAllByCustomerIdAndBakeryIdOrderByCreatedAtDesc(UUID customerId, UUID bakeryId);
    Optional<Order> findByIdAndCustomerIdAndBakeryId(UUID id, UUID customerId, UUID bakeryId);
    List<Order> findAllByBakeryIdOrderByCreatedAtDesc(UUID bakeryId);
    Optional<Order> findByIdAndBakeryId(UUID id, UUID bakeryId);
}
