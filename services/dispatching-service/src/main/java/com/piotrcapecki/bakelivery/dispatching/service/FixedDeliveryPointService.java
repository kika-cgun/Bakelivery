package com.piotrcapecki.bakelivery.dispatching.service;

import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import com.piotrcapecki.bakelivery.dispatching.dto.CreateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.dto.UpdateFixedPointRequest;
import com.piotrcapecki.bakelivery.dispatching.model.FixedDeliveryPoint;
import com.piotrcapecki.bakelivery.dispatching.repository.FixedDeliveryPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FixedDeliveryPointService {

    private final FixedDeliveryPointRepository repository;

    @Transactional
    public FixedDeliveryPoint create(UUID bakeryId, CreateFixedPointRequest req) {
        FixedDeliveryPoint point = FixedDeliveryPoint.builder()
                .bakeryId(bakeryId)
                .name(req.name())
                .address(req.address())
                .lat(req.lat())
                .lon(req.lon())
                .deliveryDays(req.deliveryDays() != null ? req.deliveryDays() : 127)
                .defaultNotes(req.defaultNotes())
                .build();
        return repository.save(point);
    }

    @Transactional(readOnly = true)
    public List<FixedDeliveryPoint> list(UUID bakeryId) {
        return repository.findByBakeryIdAndActiveTrue(bakeryId);
    }

    @Transactional
    public FixedDeliveryPoint update(UUID bakeryId, UUID pointId, UpdateFixedPointRequest req) {
        FixedDeliveryPoint point = repository.findByIdAndBakeryId(pointId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Fixed delivery point not found: " + pointId));
        if (req.name() != null) point.setName(req.name());
        if (req.address() != null) point.setAddress(req.address());
        if (req.lat() != null) point.setLat(req.lat());
        if (req.lon() != null) point.setLon(req.lon());
        if (req.deliveryDays() != null) point.setDeliveryDays(req.deliveryDays());
        if (req.defaultNotes() != null) point.setDefaultNotes(req.defaultNotes());
        return repository.save(point);
    }

    @Transactional
    public void deactivate(UUID bakeryId, UUID pointId) {
        FixedDeliveryPoint point = repository.findByIdAndBakeryId(pointId, bakeryId)
                .orElseThrow(() -> new NotFoundException("Fixed delivery point not found: " + pointId));
        point.setActive(false);
        repository.save(point);
    }
}
