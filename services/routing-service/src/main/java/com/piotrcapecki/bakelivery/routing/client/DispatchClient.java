package com.piotrcapecki.bakelivery.routing.client;

import com.piotrcapecki.bakelivery.routing.client.dto.DispatchStopDto;
import com.piotrcapecki.bakelivery.routing.client.dto.DriverTerritoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "dispatch-client", url = "${dispatch.service.url:http://localhost:8085}")
public interface DispatchClient {

    @GetMapping("/api/dispatch/internal/stops")
    List<DispatchStopDto> getStops(@RequestParam UUID bakeryId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestHeader("Authorization") String auth);

    @GetMapping("/api/dispatch/internal/territories/{driverId}")
    List<DriverTerritoryDto> getTerritories(@PathVariable UUID driverId,
                                             @RequestParam UUID bakeryId,
                                             @RequestHeader("Authorization") String auth);
}
