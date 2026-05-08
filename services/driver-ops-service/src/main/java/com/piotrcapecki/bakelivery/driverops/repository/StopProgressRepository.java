package com.piotrcapecki.bakelivery.driverops.repository;

import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StopProgressRepository extends JpaRepository<StopProgress, UUID> {

    List<StopProgress> findByShiftIdOrderBySequenceNumberAsc(UUID shiftId);

    Optional<StopProgress> findByIdAndShiftId(UUID id, UUID shiftId);

    List<StopProgress> findByShiftIdAndStatusOrderBySequenceNumberAsc(UUID shiftId, StopStatus status);
}
