package com.piotrcapecki.bakelivery.dispatching.repository;

import com.piotrcapecki.bakelivery.dispatching.model.DriverTerritory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverTerritoryRepository extends JpaRepository<DriverTerritory, UUID> {

    List<DriverTerritory> findByBakeryIdAndDriverId(UUID bakeryId, UUID driverId);

    List<DriverTerritory> findByBakeryId(UUID bakeryId);

    Optional<DriverTerritory> findByIdAndBakeryId(UUID id, UUID bakeryId);

    boolean existsByBakeryIdAndDriverIdAndFixedPointId(UUID bakeryId, UUID driverId, UUID fixedPointId);
}
