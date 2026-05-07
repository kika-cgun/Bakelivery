package com.piotrcapecki.bakelivery.dispatching.repository;

import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FixedDeliveryPointRepository extends JpaRepository<FixedDeliveryPoint, UUID> {

    List<FixedDeliveryPoint> findByBakeryIdAndActiveTrue(UUID bakeryId);

    List<FixedDeliveryPoint> findByActiveTrue();

    Optional<FixedDeliveryPoint> findByIdAndBakeryId(UUID id, UUID bakeryId);
}
