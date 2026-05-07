package com.piotrcapecki.bakelivery.routing.repository;

import com.piotrcapecki.bakelivery.routing.model.RoutePlan;
import com.piotrcapecki.bakelivery.routing.model.RoutePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, UUID> {
    Optional<RoutePlan> findByBakeryIdAndDriverIdAndDate(UUID bakeryId, UUID driverId, LocalDate date);
    List<RoutePlan> findByBakeryIdAndDate(UUID bakeryId, LocalDate date);
    List<RoutePlan> findByDate(LocalDate date);
    boolean existsByBakeryIdAndDateAndStatusIn(UUID bakeryId, LocalDate date, List<RoutePlanStatus> statuses);
}
