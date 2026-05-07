package com.piotrcapecki.bakelivery.dispatching.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.dto.AssignDriverRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.UpdateStatusRequest;
import com.piotrcapecki.bakelivery.dispatching.messaging.DispatchEventPublisher;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStop;
import com.piotrcapecki.bakelivery.dispatching.model.DispatchStopStatus;
import com.piotrcapecki.bakelivery.dispatching.repository.DispatchStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatchStopService {

    private final DispatchStopRepository repository;
    private final DispatchEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<DispatchStop> listByDate(UUID bakeryId, LocalDate date) {
        return repository.findByBakeryIdAndDate(bakeryId, date);
    }

    @Transactional(readOnly = true)
    public List<DispatchStop> listByDateAndDriver(UUID bakeryId, LocalDate date, UUID driverId) {
        return repository.findByBakeryIdAndDateAndAssignedDriverId(bakeryId, date, driverId);
    }

    @Transactional
    public DispatchStop assignDriver(UUID bakeryId, UUID stopId, AssignDriverRequest req) {
        DispatchStop stop = repository.findByIdAndBakeryId(stopId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Dispatch stop not found: " + stopId));
        stop.setAssignedDriverId(req.driverId());
        stop.setAssignedDriverName(req.driverName());
        stop.setStatus(DispatchStopStatus.ASSIGNED);
        DispatchStop saved = repository.save(stop);
        eventPublisher.publishAssigned(saved);
        return saved;
    }

    @Transactional
    public DispatchStop updateStatus(UUID bakeryId, UUID stopId, UpdateStatusRequest req) {
        DispatchStop stop = repository.findByIdAndBakeryId(stopId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Dispatch stop not found: " + stopId));
        stop.setStatus(DispatchStopStatus.valueOf(req.status()));
        return repository.save(stop);
    }
}
