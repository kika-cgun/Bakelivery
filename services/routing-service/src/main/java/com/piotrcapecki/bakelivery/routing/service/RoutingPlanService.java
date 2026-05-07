package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.client.DispatchClient;
import com.piotrcapecki.bakelivery.routing.client.dto.DispatchStopDto;
import com.piotrcapecki.bakelivery.routing.client.dto.DriverTerritoryDto;
import com.piotrcapecki.bakelivery.routing.event.RouteUpdatedEvent;
import com.piotrcapecki.bakelivery.routing.model.RoutePlan;
import com.piotrcapecki.bakelivery.routing.model.RoutePlanStatus;
import com.piotrcapecki.bakelivery.routing.model.RouteStop;
import com.piotrcapecki.bakelivery.routing.repository.RoutePlanRepository;
import com.piotrcapecki.bakelivery.routing.repository.RouteStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingPlanService {

    private final DispatchClient dispatchClient;
    private final StopClusteringService clustering;
    private final RouteSequencingService sequencing;
    private final RoutePlanRepository planRepo;
    private final RouteStopRepository stopRepo;
    private final RouteEventPublisher eventPublisher;

    @Transactional
    public void optimize(UUID bakeryId, LocalDate date, String authHeader) {
        log.info("Optimizing routes for bakery {} on {}", bakeryId, date);

        List<DispatchStopDto> stops = dispatchClient.getStops(bakeryId, date, authHeader);
        if (stops.isEmpty()) {
            log.info("No stops for bakery {} on {}, skipping", bakeryId, date);
            return;
        }

        List<UUID> driverIds = stops.stream()
                .map(DispatchStopDto::assignedDriverId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (driverIds.isEmpty()) {
            log.info("No assigned drivers yet for bakery {} on {}", bakeryId, date);
            return;
        }

        Map<UUID, List<DriverTerritoryDto>> territories = new HashMap<>();
        for (UUID driverId : driverIds) {
            territories.put(driverId, dispatchClient.getTerritories(driverId, bakeryId, authHeader));
        }

        Map<UUID, List<DispatchStopDto>> clustered = clustering.clusterStops(stops, driverIds, territories);

        for (Map.Entry<UUID, List<DispatchStopDto>> entry : clustered.entrySet()) {
            UUID driverId = entry.getKey();
            List<DispatchStopDto> driverStops = entry.getValue();
            if (driverStops.isEmpty()) continue;

            RoutePlan plan = planRepo.findByBakeryIdAndDriverIdAndDate(bakeryId, driverId, date)
                    .orElse(RoutePlan.builder().bakeryId(bakeryId).driverId(driverId).date(date).build());
            plan.setStatus(RoutePlanStatus.OPTIMIZING);
            plan = planRepo.save(plan);

            stopRepo.deleteByRoutePlanId(plan.getId());

            List<RouteStop> orderedStops = sequencing.sequenceStops(
                    bakeryId, driverId, driverStops, plan.getId(), authHeader);
            stopRepo.saveAll(orderedStops);

            plan.setStatus(RoutePlanStatus.READY);
            if (!orderedStops.isEmpty()) {
                plan.setTotalDurationSeconds((double) orderedStops.get(orderedStops.size() - 1).getEtaSeconds());
            }
            planRepo.save(plan);

            eventPublisher.publishRouteUpdated(new RouteUpdatedEvent(
                    plan.getId(), driverId, bakeryId, date, orderedStops.size()));
        }
    }
}
