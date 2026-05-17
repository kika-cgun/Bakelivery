package com.piotrcapecki.bakelivery.messaging.repository;

import com.piotrcapecki.bakelivery.messaging.model.Thread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadRepository extends JpaRepository<Thread, UUID> {

    Optional<Thread> findByBakeryIdAndOrderId(UUID bakeryId, UUID orderId);

    List<Thread> findByBakeryIdAndCustomerId(UUID bakeryId, UUID customerId);

    List<Thread> findByBakeryId(UUID bakeryId);

    List<Thread> findByBakeryIdAndDriverId(UUID bakeryId, UUID driverId);

    boolean existsByBakeryIdAndOrderId(UUID bakeryId, UUID orderId);
}
