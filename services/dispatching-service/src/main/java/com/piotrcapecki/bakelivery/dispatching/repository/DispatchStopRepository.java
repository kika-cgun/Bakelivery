package com.piotrcapecki.bakelivery.dispatching.repository;

import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispatchStopRepository extends JpaRepository<DispatchStop, UUID> {

    List<DispatchStop> findByBakeryIdAndDate(UUID bakeryId, LocalDate date);

    List<DispatchStop> findByBakeryIdAndDateAndAssignedDriverId(UUID bakeryId, LocalDate date, UUID driverId);

    boolean existsByDateAndOrderId(LocalDate date, UUID orderId);

    Optional<DispatchStop> findByIdAndBakeryId(UUID id, UUID bakeryId);
}
