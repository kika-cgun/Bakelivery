package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.client.dto.DispatchStopDto;
import com.piotrcapecki.bakelivery.routing.client.dto.DriverTerritoryDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StopClusteringService {

    public Map<UUID, List<DispatchStopDto>> clusterStops(
            List<DispatchStopDto> stops,
            List<UUID> driverIds,
            Map<UUID, List<DriverTerritoryDto>> territoriesByDriver) {

        Map<UUID, List<DispatchStopDto>> result = new LinkedHashMap<>();
        driverIds.forEach(id -> result.put(id, new ArrayList<>()));

        List<DispatchStopDto> orphans = new ArrayList<>();

        for (DispatchStopDto stop : stops) {
            if (stop.fixedPointId() == null) {
                orphans.add(stop);
                continue;
            }
            // Find driver with highest affinity for this fixed point
            UUID bestDriver = null;
            int bestAffinity = -1;
            for (UUID driverId : driverIds) {
                List<DriverTerritoryDto> territories = territoriesByDriver.getOrDefault(driverId, List.of());
                for (DriverTerritoryDto t : territories) {
                    if (t.fixedPointId().equals(stop.fixedPointId()) && t.affinityScore() > bestAffinity) {
                        bestAffinity = t.affinityScore();
                        bestDriver = driverId;
                    }
                }
            }
            if (bestDriver != null) {
                result.get(bestDriver).add(stop);
            } else {
                orphans.add(stop);
            }
        }

        // Round-robin orphan stops to drivers by smallest pool
        for (DispatchStopDto orphan : orphans) {
            UUID smallestDriver = result.entrySet().stream()
                    .min(Comparator.comparingInt(e -> e.getValue().size()))
                    .map(Map.Entry::getKey)
                    .orElse(driverIds.get(0));
            result.get(smallestDriver).add(orphan);
        }

        return result;
    }
}
