package com.piotrcapecki.bakelivery.driverops.service;

import com.piotrcapecki.bakelivery.driverops.client.RouteStopDto;
import com.piotrcapecki.bakelivery.driverops.client.RoutingClient;
import com.piotrcapecki.bakelivery.driverops.domain.DriverShift;
import com.piotrcapecki.bakelivery.driverops.domain.ShiftStatus;
import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.domain.StopStatus;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftResponse;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftWithStopsResponse;
import com.piotrcapecki.bakelivery.driverops.dto.StartShiftRequest;
import com.piotrcapecki.bakelivery.driverops.dto.StopProgressResponse;
import com.piotrcapecki.bakelivery.driverops.repository.DriverShiftRepository;
import com.piotrcapecki.bakelivery.driverops.repository.StopProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final DriverShiftRepository shiftRepository;
    private final StopProgressRepository stopProgressRepository;
    private final RoutingClient routingClient;

    @Transactional
    public ShiftResponse startShift(UUID driverId, UUID bakeryId, StartShiftRequest request, String authHeader) {
        LocalDate today = LocalDate.now();

        shiftRepository.findByDriverIdAndDate(driverId, today).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Driver already has a shift for today");
        });

        List<RouteStopDto> routeStops = routingClient.getPlanStops(
                request.routePlanId(),
                bakeryId.toString(),
                authHeader);

        if (routeStops == null || routeStops.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Route plan has no stops");
        }

        UUID shiftId = UUID.randomUUID();
        DriverShift shift = DriverShift.builder()
                .id(shiftId)
                .bakeryId(bakeryId)
                .driverId(driverId)
                .date(today)
                .routePlanId(request.routePlanId())
                .status(ShiftStatus.ACTIVE)
                .currentStopIndex(0)
                .startedAt(OffsetDateTime.now())
                .build();
        shiftRepository.save(shift);

        List<StopProgress> stops = routeStops.stream()
                .map(rs -> StopProgress.builder()
                        .id(UUID.randomUUID())
                        .shiftId(shiftId)
                        .bakeryId(bakeryId)
                        .dispatchStopId(rs.dispatchStopId())
                        .routeStopId(rs.id())
                        .sequenceNumber(rs.sequenceNumber())
                        .customerName(rs.customerName())
                        .deliveryAddress(rs.deliveryAddress())
                        .lat(rs.lat())
                        .lon(rs.lon())
                        .status(StopStatus.PENDING)
                        .etaSeconds(rs.etaSeconds())
                        .build())
                .toList();
        stopProgressRepository.saveAll(stops);

        return toShiftResponse(shift);
    }

    @Transactional(readOnly = true)
    public ShiftWithStopsResponse getCurrentShift(UUID driverId) {
        DriverShift shift = shiftRepository
                .findByDriverIdAndDateAndStatus(driverId, LocalDate.now(), ShiftStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active shift found for today"));

        List<StopProgress> stops = stopProgressRepository.findByShiftIdOrderBySequenceNumberAsc(shift.getId());

        StopProgressResponse currentStop = null;
        if (shift.getCurrentStopIndex() < stops.size()) {
            currentStop = stops.stream()
                    .filter(s -> s.getStatus() == StopStatus.PENDING)
                    .findFirst()
                    .map(this::toStopResponse)
                    .orElse(null);
        }

        List<StopProgressResponse> allStops = stops.stream()
                .map(this::toStopResponse)
                .toList();

        return new ShiftWithStopsResponse(toShiftResponse(shift), currentStop, allStops);
    }

    @Transactional
    public ShiftResponse completeShift(UUID driverId) {
        DriverShift shift = shiftRepository
                .findByDriverIdAndDateAndStatus(driverId, LocalDate.now(), ShiftStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active shift found for today"));

        shift.setStatus(ShiftStatus.COMPLETED);
        shift.setCompletedAt(OffsetDateTime.now());
        shiftRepository.save(shift);

        return toShiftResponse(shift);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getShiftsForBakery(UUID bakeryId, LocalDate date) {
        return shiftRepository.findByBakeryIdAndDate(bakeryId, date)
                .stream()
                .map(this::toShiftResponse)
                .toList();
    }

    ShiftResponse toShiftResponse(DriverShift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getDriverId(),
                shift.getDate(),
                shift.getRoutePlanId(),
                shift.getStatus().name(),
                shift.getCurrentStopIndex(),
                shift.getStartedAt() != null ? shift.getStartedAt().toLocalDateTime() : null,
                shift.getCompletedAt() != null ? shift.getCompletedAt().toLocalDateTime() : null
        );
    }

    StopProgressResponse toStopResponse(StopProgress stop) {
        return new StopProgressResponse(
                stop.getId(),
                stop.getSequenceNumber(),
                stop.getDispatchStopId(),
                stop.getCustomerName(),
                stop.getDeliveryAddress(),
                stop.getLat(),
                stop.getLon(),
                stop.getStatus().name(),
                null,
                stop.getEtaSeconds(),
                stop.getCompletedAt() != null ? stop.getCompletedAt().toLocalDateTime() : null,
                stop.getSkippedReason()
        );
    }
}
