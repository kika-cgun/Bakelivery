package com.piotrcapecki.bakelivery.driverops.repository;

import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverShiftRepository extends JpaRepository<DriverShift, UUID> {

    Optional<DriverShift> findByDriverIdAndDate(UUID driverId, LocalDate date);

    List<DriverShift> findByBakeryIdAndDate(UUID bakeryId, LocalDate date);

    Optional<DriverShift> findByDriverIdAndDateAndStatus(UUID driverId, LocalDate date, ShiftStatus status);
}
