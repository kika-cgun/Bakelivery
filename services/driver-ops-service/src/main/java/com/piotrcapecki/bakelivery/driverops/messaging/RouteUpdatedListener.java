package com.piotrcapecki.bakelivery.driverops.messaging;

import com.piotrcapecki.bakelivery.driverops.client.RouteStopDto;
import com.piotrcapecki.bakelivery.driverops.client.RoutingClient;
import com.piotrcapecki.bakelivery.driverops.config.RabbitConfig;
import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import com.piotrcapecki.bakelivery.driverops.repository.DriverShiftRepository;
import com.piotrcapecki.bakelivery.driverops.repository.StopProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RouteUpdatedListener {

    private final DriverShiftRepository shiftRepository;
    private final StopProgressRepository stopProgressRepository;
    private final RoutingClient routingClient;

    @RabbitListener(queues = RabbitConfig.ROUTE_UPDATED_QUEUE)
    @Transactional
    public void onRouteUpdated(Map<String, String> event) {
        try {
            UUID driverId = UUID.fromString(event.get("driverId"));
            UUID bakeryId = UUID.fromString(event.get("bakeryId"));
            UUID planId = UUID.fromString(event.get("planId"));

            Optional<DriverShift> shiftOpt = shiftRepository
                    .findByDriverIdAndDateAndStatus(driverId, LocalDate.now(), ShiftStatus.ACTIVE);

            if (shiftOpt.isEmpty()) {
                log.debug("No active shift for driver {} on route.updated — ignoring", driverId);
                return;
            }

            DriverShift shift = shiftOpt.get();

            List<RouteStopDto> updatedStops = routingClient.getPlanStops(
                    planId,
                    bakeryId.toString(),
                    "internal");

            List<StopProgress> existingStops = stopProgressRepository
                    .findByShiftIdOrderBySequenceNumberAsc(shift.getId());

            Map<UUID, StopProgress> byRouteStopId = existingStops.stream()
                    .collect(Collectors.toMap(StopProgress::getRouteStopId, s -> s));

            for (RouteStopDto updatedStop : updatedStops) {
                StopProgress existing = byRouteStopId.get(updatedStop.id());
                if (existing != null && existing.getStatus() == StopStatus.PENDING) {
                    existing.setSequenceNumber(updatedStop.sequenceNumber());
                    existing.setEtaSeconds(updatedStop.etaSeconds());
                    stopProgressRepository.save(existing);
                }
            }

            log.info("Refreshed StopProgress for shift {} after route.updated (planId={})",
                    shift.getId(), planId);

        } catch (Exception e) {
            log.error("Failed to process route.updated event: {}", e.getMessage(), e);
            throw e;
        }
    }
}
