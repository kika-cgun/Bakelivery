package com.piotrcapecki.bakelivery.driverops.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "routing-client", url = "${routing.service.url:http://localhost:8086}")
public interface RoutingClient {

    @GetMapping("/api/routing/internal/plans/{planId}/stops")
    List<RouteStopDto> getPlanStops(
            @PathVariable UUID planId,
            @RequestHeader("X-Bakery-Id") String bakeryId,
            @RequestHeader("Authorization") String auth);
}
