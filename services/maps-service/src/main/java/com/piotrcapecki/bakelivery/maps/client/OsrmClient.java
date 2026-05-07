package com.piotrcapecki.bakelivery.maps.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.piotrcapecki.bakelivery.maps.dto.Coordinate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class OsrmClient {

    private final RestClient restClient;
    private final String baseUrl;

    public OsrmClient(@Qualifier("osrmRestClient") RestClient restClient,
                      @Value("${maps.osrm.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
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

        String url = baseUrl + "/table/v1/driving/" + coordStr
            + "?sources=" + sourceIndices
            + "&destinations=" + destIndices
            + "&annotations=duration,distance";

        return restClient.get()
            .uri(URI.create(url))
            .retrieve()
            .body(OsrmTableResponse.class);
    }

    public OsrmRouteResponse route(List<Coordinate> waypoints, boolean steps, boolean overview) {
        String coordStr = waypoints.stream()
            .map(c -> c.lon() + "," + c.lat())
            .collect(Collectors.joining(";"));

        String url = baseUrl + "/route/v1/driving/" + coordStr
            + "?overview=" + (overview ? "full" : "false")
            + "&geometries=geojson"
            + "&steps=" + steps;

        return restClient.get()
            .uri(URI.create(url))
            .retrieve()
            .body(OsrmRouteResponse.class);
    }

    public record OsrmTableResponse(
        String code,
        double[][] durations,
        double[][] distances
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
