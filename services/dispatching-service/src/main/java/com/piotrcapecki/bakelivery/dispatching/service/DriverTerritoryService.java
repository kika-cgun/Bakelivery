package com.piotrcapecki.bakelivery.dispatching.service;

import com.piotrcapecki.bakelivery.common.exception.ConflictException;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.dto.CreateTerritoryRequest;
import com.piotrcapecki.bakelivery.dispatching.model.DriverTerritory;
import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import com.piotrcapecki.bakelivery.dispatching.repository.DriverTerritoryRepository;
import com.piotrcapecki.bakelivery.dispatching.repository.FixedDeliveryPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverTerritoryService {

    private final DriverTerritoryRepository territoryRepository;
    private final FixedDeliveryPointRepository pointRepository;

    @Transactional
    public DriverTerritory create(UUID bakeryId, CreateTerritoryRequest req) {
        FixedDeliveryPoint point = pointRepository.findByIdAndBakeryId(req.fixedPointId(), bakeryId)
                .orElseThrow(() -> new NotFoundException(
                        "Fixed delivery point not found or does not belong to bakery: " + req.fixedPointId()));

        if (territoryRepository.existsByBakeryIdAndDriverIdAndFixedPointId(
                bakeryId, req.driverId(), req.fixedPointId())) {
            throw new ConflictException("Territory assignment already exists for this driver and point");
        }

        DriverTerritory territory = DriverTerritory.builder()
                .bakeryId(bakeryId)
                .driverId(req.driverId())
                .driverName(req.driverName())
                .fixedPoint(point)
                .build();
        return territoryRepository.save(territory);
    }

    @Transactional(readOnly = true)
    public List<DriverTerritory> listByDriver(UUID bakeryId, UUID driverId) {
        return territoryRepository.findByBakeryIdAndDriverId(bakeryId, driverId);
    }

    @Transactional
    public void delete(UUID bakeryId, UUID territoryId) {
        DriverTerritory territory = territoryRepository.findByIdAndBakeryId(territoryId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Territory not found: " + territoryId));
        territoryRepository.delete(territory);
    }
}
