package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.client.MapsClient;
import com.piotrcapecki.bakelivery.routing.client.dto.CoordinateDto;
import com.piotrcapecki.bakelivery.routing.client.dto.DispatchStopDto;
import com.piotrcapecki.bakelivery.routing.client.dto.TripRequest;
import com.piotrcapecki.bakelivery.routing.client.dto.TripResponse;
import com.piotrcapecki.bakelivery.routing.model.RouteStop;
import com.piotrcapecki.bakelivery.routing.model.RouteStopStatus;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteSequencingService {

    private final MapsClient mapsClient;

    public List<RouteStop> sequenceStops(UUID bakeryId, UUID driverId,
                                          List<DispatchStopDto> stops, UUID routePlanId, String authHeader) {
        if (stops.isEmpty()) return List.of();

        List<Integer> order;
        List<Double> legDurations = new ArrayList<>();

        try {
            List<CoordinateDto> coords = stops.stream()
                    .map(s -> new CoordinateDto(s.lat(), s.lon(), s.customerName()))
                    .toList();
            TripResponse resp = mapsClient.trip(new TripRequest(coords), authHeader);
            order = resp.waypointOrder();
            if (resp.legs() != null) {
                resp.legs().forEach(leg -> legDurations.add(leg.durationSeconds()));
            }
        } catch (FeignException e) {
            log.warn("OSRM unavailable, falling back to nearest-neighbor: {}", e.getMessage());
            order = nearestNeighbor(stops);
        }

        List<RouteStop> result = new ArrayList<>();
        double eta = 0;
        for (int seq = 0; seq < order.size(); seq++) {
            int idx = order.get(seq);
            DispatchStopDto s = stops.get(idx);
            if (seq > 0 && seq - 1 < legDurations.size()) {
                eta += legDurations.get(seq - 1);
            }
            result.add(RouteStop.builder()
                    .bakeryId(bakeryId)
                    .routePlanId(routePlanId)
                    .dispatchStopId(s.id())
                    .sequenceNumber(seq + 1)
                    .lat(s.lat() != null ? s.lat() : 0)
                    .lon(s.lon() != null ? s.lon() : 0)
                    .customerName(s.customerName())
                    .deliveryAddress(s.deliveryAddress())
                    .etaSeconds((int) eta)
                    .status(RouteStopStatus.PENDING)
                    .build());
        }
        return result;
    }

    List<Integer> nearestNeighbor(List<DispatchStopDto> stops) {
        if (stops.isEmpty()) return List.of();
        int n = stops.size();
        boolean[] visited = new boolean[n];
        List<Integer> order = new ArrayList<>();
        int current = 0;
        visited[0] = true;
        order.add(0);
        for (int i = 1; i < n; i++) {
            double minDist = Double.MAX_VALUE;
            int nearest = -1;
            for (int j = 0; j < n; j++) {
                if (!visited[j]) {
                    double dist = euclidean(stops.get(current), stops.get(j));
                    if (dist < minDist) { minDist = dist; nearest = j; }
                }
            }
            visited[nearest] = true;
            order.add(nearest);
            current = nearest;
        }
        return order;
    }

    private double euclidean(DispatchStopDto a, DispatchStopDto b) {
        double dlat = (a.lat() != null ? a.lat() : 0) - (b.lat() != null ? b.lat() : 0);
        double dlon = (a.lon() != null ? a.lon() : 0) - (b.lon() != null ? b.lon() : 0);
        return Math.sqrt(dlat * dlat + dlon * dlon);
    }
}
