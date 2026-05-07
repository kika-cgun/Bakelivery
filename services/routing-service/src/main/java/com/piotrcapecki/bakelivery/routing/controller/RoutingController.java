package com.piotrcapecki.bakelivery.routing.controller;

import com.piotrcapecki.bakelivery.routing.config.RabbitConfig;
import com.piotrcapecki.bakelivery.routing.dto.RoutePlanResponse;
import com.piotrcapecki.bakelivery.routing.dto.RoutePlanWithStopsResponse;
import com.piotrcapecki.bakelivery.routing.dto.RouteStopResponse;
import com.piotrcapecki.bakelivery.routing.event.OptimizeRequest;
import com.piotrcapecki.bakelivery.routing.model.RoutePlan;
import com.piotrcapecki.bakelivery.routing.model.RouteStop;
import com.piotrcapecki.bakelivery.routing.repository.RoutePlanRepository;
import com.piotrcapecki.bakelivery.routing.repository.RouteStopRepository;
import com.piotrcapecki.bakelivery.routing.security.RoutingPrincipal;
import com.piotrcapecki.bakelivery.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final RabbitTemplate rabbitTemplate;
    private final RoutePlanRepository planRepo;
    private final RouteStopRepository stopRepo;

    @PostMapping("/admin/optimize")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> triggerOptimize(
            @AuthenticationPrincipal RoutingPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        rabbitTemplate.convertAndSend(RabbitConfig.ROUTING_EXCHANGE, RabbitConfig.QUEUE_OPTIMIZE,
                new OptimizeRequest(user.bakeryId(), date, user.email()));
        return Map.of("message", "Optimization queued", "date", date.toString());
    }

    @GetMapping("/admin/plans")
    public List<RoutePlanResponse> listPlans(
            @AuthenticationPrincipal RoutingPrincipal user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return planRepo.findByBakeryIdAndDate(user.bakeryId(), date)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/admin/plans/{planId}/stops")
    public List<RouteStopResponse> planStops(
            @AuthenticationPrincipal RoutingPrincipal user,
            @PathVariable UUID planId) {
        RoutePlan plan = planRepo.findById(planId)
                .filter(p -> p.getBakeryId().equals(user.bakeryId()))
                .orElseThrow(() -> new NotFoundException("Plan not found"));
        return stopRepo.findByRoutePlanIdOrderBySequenceNumberAsc(plan.getId())
                .stream().map(this::toStopResponse).toList();
    }

    @GetMapping("/driver/plan/today")
    public RoutePlanWithStopsResponse driverPlanToday(@AuthenticationPrincipal RoutingPrincipal user) {
        RoutePlan plan = planRepo.findByBakeryIdAndDriverIdAndDate(
                        user.bakeryId(), user.userId(), LocalDate.now())
                .orElseThrow(() -> new NotFoundException("No route plan for today"));
        List<RouteStopResponse> stops = stopRepo.findByRoutePlanIdOrderBySequenceNumberAsc(plan.getId())
                .stream().map(this::toStopResponse).toList();
        return new RoutePlanWithStopsResponse(toResponse(plan), stops);
    }

    @GetMapping("/internal/plans/{planId}/stops")
    public List<RouteStopResponse> internalPlanStops(@PathVariable UUID planId) {
        return stopRepo.findByRoutePlanIdOrderBySequenceNumberAsc(planId)
                .stream().map(this::toStopResponse).toList();
    }

    private RoutePlanResponse toResponse(RoutePlan p) {
        return new RoutePlanResponse(p.getId(), p.getDriverId(), p.getDate(),
                p.getStatus().name(), p.getTotalDistanceMeters(), p.getTotalDurationSeconds());
    }

    private RouteStopResponse toStopResponse(RouteStop s) {
        return new RouteStopResponse(s.getId(), s.getDispatchStopId(), s.getSequenceNumber(),
                s.getLat(), s.getLon(), s.getCustomerName(), s.getDeliveryAddress(),
                s.getAffinityScore(), s.getEtaSeconds(), s.getStatus().name());
    }
}
