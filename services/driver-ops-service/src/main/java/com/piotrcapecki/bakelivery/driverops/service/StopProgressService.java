package com.piotrcapecki.bakelivery.driverops.service;

import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import com.piotrcapecki.bakelivery.driverops.repository.DriverShiftRepository;
import com.piotrcapecki.bakelivery.driverops.repository.StopProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StopProgressService {

    private final DriverShiftRepository shiftRepository;
    private final StopProgressRepository stopProgressRepository;

    @Transactional
    public StopProgress completeStop(UUID driverId, UUID stopId) {
        DriverShift shift = getActiveShiftOrThrow(driverId);

        StopProgress stop = stopProgressRepository.findByIdAndShiftId(stopId, shift.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Stop not found in current shift"));

        if (stop.getStatus() != StopStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stop is not in PENDING status");
        }

        stop.setStatus(StopStatus.COMPLETED);
        stop.setCompletedAt(OffsetDateTime.now());
        stopProgressRepository.save(stop);

        advanceStopIndex(shift);

        return stop;
    }

    @Transactional
    public StopProgress skipStop(UUID driverId, UUID stopId, String reason) {
        DriverShift shift = getActiveShiftOrThrow(driverId);

        StopProgress stop = stopProgressRepository.findByIdAndShiftId(stopId, shift.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Stop not found in current shift"));

        if (stop.getStatus() != StopStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stop is not in PENDING status");
        }

        List<StopProgress> pendingStops = stopProgressRepository
                .findByShiftIdAndStatusOrderBySequenceNumberAsc(shift.getId(), StopStatus.PENDING);

        int maxSeq = pendingStops.stream()
                .mapToInt(StopProgress::getSequenceNumber)
                .max()
                .orElse(stop.getSequenceNumber());

        stop.setSequenceNumber(maxSeq + 1);
        stop.setSkippedReason(reason);
        stop.setStatus(StopStatus.SKIPPED);
        stopProgressRepository.save(stop);

        advanceStopIndex(shift);

        return stop;
    }

    @Transactional
    public List<StopProgress> reorderStops(UUID driverId, List<UUID> orderedIds) {
        DriverShift shift = getActiveShiftOrThrow(driverId);

        List<StopProgress> pendingStops = stopProgressRepository
                .findByShiftIdAndStatusOrderBySequenceNumberAsc(shift.getId(), StopStatus.PENDING);

        Set<UUID> pendingIds = pendingStops.stream()
                .map(StopProgress::getId)
                .collect(Collectors.toSet());

        if (!pendingIds.containsAll(orderedIds) || orderedIds.size() != pendingIds.size()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "orderedIds must contain exactly all PENDING stop IDs for this shift");
        }

        int baseSeq = pendingStops.stream()
                .mapToInt(StopProgress::getSequenceNumber)
                .min()
                .orElse(1);

        Map<UUID, StopProgress> stopById = pendingStops.stream()
                .collect(Collectors.toMap(StopProgress::getId, s -> s));

        for (int i = 0; i < orderedIds.size(); i++) {
            StopProgress stop = stopById.get(orderedIds.get(i));
            stop.setSequenceNumber(baseSeq + i);
        }

        stopProgressRepository.saveAll(pendingStops);
        return pendingStops.stream()
                .sorted(Comparator.comparingInt(StopProgress::getSequenceNumber))
                .toList();
    }

    private DriverShift getActiveShiftOrThrow(UUID driverId) {
        return shiftRepository.findByDriverIdAndDateAndStatus(driverId, LocalDate.now(), ShiftStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active shift found for today"));
    }

    private void advanceStopIndex(DriverShift shift) {
        shift.setCurrentStopIndex(shift.getCurrentStopIndex() + 1);
        shiftRepository.save(shift);
    }
}
