package com.piotrcapecki.bakelivery.maps.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.piotrcapecki.bakelivery.maps.dto.Coordinate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class OsrmClient {

    private final RestClient restClient;

    public OsrmClient(@Qualifier("osrmRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public OsrmTableResponse table(List<Coordinate> sources, List<Coordinate> destinations) {
        List<Coordinate> allCoords = new ArrayList<>(sources);
        allCoords.addAll(destinations);

        String coordStr = allCoords.stream()
            .map(c -> c.lon() + "," + c.lat())
            .collect(Collectors.joining(";"));

        String sourceIndices = IntStream.range(0, sources.size())
            .mapToObj(String::valueOf).collect(Collectors.joining(";"));
        String destIndices = IntStream.range(sources.size(), allCoords.size())
            .mapToObj(String::valueOf).collect(Collectors.joining(";"));

        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/table/v1/driving/" + coordStr)
                .queryParam("sources", sourceIndices)
                .queryParam("destinations", destIndices)
                .queryParam("annotations", "duration,distance")
                .build())
            .retrieve()
            .body(OsrmTableResponse.class);
    }

    public OsrmRouteResponse route(List<Coordinate> waypoints, boolean steps, boolean overview) {
        String coordStr = waypoints.stream()
            .map(c -> c.lon() + "," + c.lat())
            .collect(Collectors.joining(";"));

        return restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/route/v1/driving/" + coordStr)
                .queryParam("overview", overview ? "full" : "false")
                .queryParam("geometries", "geojson")
                .queryParam("steps", String.valueOf(steps))
                .build())
            .retrieve()
            .body(OsrmRouteResponse.class);
    }

    public record OsrmTableResponse(
        String code,
        @JsonProperty("durations") double[][] durations,
        @JsonProperty("distances") double[][] distances
    ) {}

    public record OsrmRouteResponse(
        String code,
        @JsonProperty("routes") List<OsrmRoute> routes
    ) {
        public record OsrmRoute(
            double distance,
            double duration,
            @JsonProperty("geometry") Object geometry,
            @JsonProperty("legs") List<OsrmLeg> legs
        ) {}

        public record OsrmLeg(
            double distance,
            double duration,
            @JsonProperty("steps") List<OsrmStep> steps
        ) {}

        public record OsrmStep(
            double distance,
            double duration,
            @JsonProperty("maneuver") OsrmManeuver maneuver,
            String name
        ) {}

        public record OsrmManeuver(
            String type,
            String modifier
        ) {}
    }
}
