package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.client.dto.DispatchStopDto;
import com.piotrcapecki.bakelivery.routing.client.dto.DriverTerritoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class StopClusteringServiceTest {

    StopClusteringService service;

    @BeforeEach
    void setup() { service = new StopClusteringService(); }

    private DispatchStopDto stop(UUID id, UUID fixedPointId, UUID assignedDriverId) {
        return new DispatchStopDto(id, null, null, fixedPointId,
                "Name", "Address", 52.0, 21.0, assignedDriverId, null, "ASSIGNED", null);
    }

    private DriverTerritoryDto territory(UUID driverId, UUID fixedPointId, int affinity) {
        return new DriverTerritoryDto(UUID.randomUUID(), driverId, "Driver",
                fixedPointId, "Point", affinity);
    }

    @Test
    void clustersStopToDriverByTerritory() {
        UUID driverA = UUID.randomUUID();
        UUID fixedPt = UUID.randomUUID();
        UUID stopId  = UUID.randomUUID();

        DispatchStopDto s = stop(stopId, fixedPt, driverA);
        Map<UUID, List<DriverTerritoryDto>> territories = Map.of(
                driverA, List.of(territory(driverA, fixedPt, 5)));

        Map<UUID, List<DispatchStopDto>> result = service.clusterStops(
                List.of(s), List.of(driverA), territories);

        assertThat(result.get(driverA)).hasSize(1);
        assertThat(result.get(driverA).get(0).id()).isEqualTo(stopId);
    }

    @Test
    void roundRobinsOrphanStops() {
        UUID driverA = UUID.randomUUID();
        UUID driverB = UUID.randomUUID();
        List<UUID> drivers = List.of(driverA, driverB);

        List<DispatchStopDto> stops = List.of(
                stop(UUID.randomUUID(), null, driverA),
                stop(UUID.randomUUID(), null, driverB),
                stop(UUID.randomUUID(), null, driverA));

        Map<UUID, List<DispatchStopDto>> result = service.clusterStops(
                stops, drivers, Map.of(driverA, List.of(), driverB, List.of()));

        int total = result.values().stream().mapToInt(List::size).sum();
        assertThat(total).isEqualTo(3);
    }
}
