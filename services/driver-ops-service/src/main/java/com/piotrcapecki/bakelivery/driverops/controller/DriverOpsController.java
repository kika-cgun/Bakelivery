package com.piotrcapecki.bakelivery.driverops.controller;

import com.piotrcapecki.bakelivery.driverops.domain.StopProgress;
import com.piotrcapecki.bakelivery.driverops.dto.DeliveryCompletedEvent;
import com.piotrcapecki.bakelivery.driverops.dto.DeliverySkippedEvent;
import com.piotrcapecki.bakelivery.driverops.dto.PositionRequest;
import com.piotrcapecki.bakelivery.driverops.dto.ReorderStopsRequest;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftResponse;
import com.piotrcapecki.bakelivery.driverops.dto.ShiftWithStopsResponse;
import com.piotrcapecki.bakelivery.driverops.dto.SkipStopRequest;
import com.piotrcapecki.bakelivery.driverops.dto.StartShiftRequest;
import com.piotrcapecki.bakelivery.driverops.dto.StopProgressResponse;
import com.piotrcapecki.bakelivery.driverops.messaging.DeliveryEventPublisher;
import com.piotrcapecki.bakelivery.driverops.security.DriverOpsPrincipal;
import com.piotrcapecki.bakelivery.driverops.service.DriverPositionService;
import com.piotrcapecki.bakelivery.driverops.service.ProofOfDeliveryService;
import com.piotrcapecki.bakelivery.driverops.service.ShiftService;
import com.piotrcapecki.bakelivery.driverops.service.StopProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/driver-ops")
@RequiredArgsConstructor
public class DriverOpsController {

    private final ShiftService shiftService;
    private final StopProgressService stopProgressService;
    private final ProofOfDeliveryService proofService;
    private final DriverPositionService positionService;
    private final DeliveryEventPublisher eventPublisher;

    @PostMapping("/shift/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse startShift(
            @AuthenticationPrincipal DriverOpsPrincipal principal,
            @Valid @RequestBody StartShiftRequest request,
            @RequestHeader("Authorization") String auth) {
        return shiftService.startShift(principal.userId(), principal.bakeryId(), request, auth);
    }

    @GetMapping("/shift/current")
    public ShiftWithStopsResponse getCurrentShift(
            @AuthenticationPrincipal DriverOpsPrincipal principal) {
        return shiftService.getCurrentShift(principal.userId());
    }

    @PostMapping("/shift/stops/{stopId}/complete")
    @ResponseStatus(HttpStatus.OK)
    public StopProgressResponse completeStop(
            @AuthenticationPrincipal DriverOpsPrincipal principal,
            @PathVariable UUID stopId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes) throws IOException {

        StopProgress stop = stopProgressService.completeStop(principal.userId(), stopId);

        String proofObjectKey = null;
        if (file != null && !file.isEmpty()) {
            proofObjectKey = proofService.upload(
                    principal.bakeryId(),
                    principal.userId(),
                    LocalDate.now(),
                    stop.getId(),
                    file);
            stop.setProofObjectKey(proofObjectKey);
        }

        String proofUrl = proofObjectKey != null ? proofService.presignedGetUrl(proofObjectKey) : null;

        eventPublisher.publishCompleted(new DeliveryCompletedEvent(
                stop.getDispatchStopId(),
                null,
                principal.bakeryId(),
                principal.userId(),
                stop.getShiftId(),
                proofObjectKey != null,
                LocalDateTime.now()));

        return new StopProgressResponse(
                stop.getId(),
                stop.getSequenceNumber(),
                stop.getDispatchStopId(),
                stop.getCustomerName(),
                stop.getDeliveryAddress(),
                stop.getLat(),
                stop.getLon(),
                stop.getStatus().name(),
                proofUrl,
                stop.getEtaSeconds(),
                stop.getCompletedAt() != null ? stop.getCompletedAt().toLocalDateTime() : null,
                stop.getSkippedReason()
        );
    }

    @PostMapping("/shift/stops/{stopId}/skip")
    public StopProgressResponse skipStop(
            @AuthenticationPrincipal DriverOpsPrincipal principal,
            @PathVariable UUID stopId,
            @Valid @RequestBody SkipStopRequest request) {

        StopProgress stop = stopProgressService.skipStop(principal.userId(), stopId, request.reason());

        eventPublisher.publishSkipped(new DeliverySkippedEvent(
                stop.getDispatchStopId(),
                null,
                principal.bakeryId(),
                principal.userId(),
                request.reason()));

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

    @PutMapping("/shift/stops/reorder")
    public List<StopProgressResponse> reorderStops(
            @AuthenticationPrincipal DriverOpsPrincipal principal,
            @Valid @RequestBody ReorderStopsRequest request) {

        return stopProgressService.reorderStops(principal.userId(), request.stopIds())
                .stream()
                .map(s -> new StopProgressResponse(
                        s.getId(),
                        s.getSequenceNumber(),
                        s.getDispatchStopId(),
                        s.getCustomerName(),
                        s.getDeliveryAddress(),
                        s.getLat(),
                        s.getLon(),
                        s.getStatus().name(),
                        null,
                        s.getEtaSeconds(),
                        null,
                        null))
                .toList();
    }

    @PostMapping("/shift/complete")
    public ShiftResponse completeShift(
            @AuthenticationPrincipal DriverOpsPrincipal principal) {
        return shiftService.completeShift(principal.userId());
    }

    @PostMapping("/position")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePosition(
            @AuthenticationPrincipal DriverOpsPrincipal principal,
            @Valid @RequestBody PositionRequest request) {

        try {
            ShiftWithStopsResponse current = shiftService.getCurrentShift(principal.userId());
            positionService.update(
                    principal.userId(),
                    principal.bakeryId(),
                    current.shift().id(),
                    request.lat(),
                    request.lon());
        } catch (Exception e) {
            positionService.update(
                    principal.userId(),
                    principal.bakeryId(),
                    UUID.fromString("00000000-0000-0000-0000-000000000000"),
                    request.lat(),
                    request.lon());
        }
    }

    @GetMapping("/admin/shifts")
    public List<ShiftResponse> getAdminShifts(
            @AuthenticationPrincipal DriverOpsPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = date != null ? date : LocalDate.now();
        return shiftService.getShiftsForBakery(principal.bakeryId(), queryDate);
    }
}
