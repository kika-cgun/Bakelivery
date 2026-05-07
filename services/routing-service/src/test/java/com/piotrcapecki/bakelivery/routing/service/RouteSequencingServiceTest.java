package com.piotrcapecki.bakelivery.routing.service;

import com.piotrcapecki.bakelivery.routing.client.MapsClient;
import com.piotrcapecki.bakelivery.routing.client.dto.*;
import com.piotrcapecki.bakelivery.routing.model.RouteStop;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteSequencingServiceTest {

    @Mock MapsClient mapsClient;
    @InjectMocks RouteSequencingService service;

    private DispatchStopDto stop(double lat, double lon) {
        return new DispatchStopDto(UUID.randomUUID(), null, null, null,
                "Name", "Address", lat, lon, UUID.randomUUID(), "Driver", "ASSIGNED", null);
    }

    @Test
    void sequencesUsingOsrmOrder() {
        var stops = List.of(stop(52.0, 21.0), stop(52.1, 21.1), stop(52.2, 21.2));
        var legs = List.of(new LegDto(1000, 60), new LegDto(1000, 60));
        when(mapsClient.trip(any(), anyString()))
                .thenReturn(new TripResponse("geojson", 2000, 120, legs, List.of(0, 2, 1)));

        List<RouteStop> result = service.sequenceStops(UUID.randomUUID(), UUID.randomUUID(),
                stops, UUID.randomUUID(), "Bearer token");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getSequenceNumber()).isEqualTo(1);
        // waypointOrder [0,2,1] means stop[0] first, then stop[2], then stop[1]
        assertThat(result.get(0).getLat()).isEqualTo(52.0);
        assertThat(result.get(1).getLat()).isEqualTo(52.2);
    }

    @Test
    void fallsBackToNearestNeighborOnFeignException() {
        var stops = List.of(stop(52.0, 21.0), stop(52.1, 21.1));
        when(mapsClient.trip(any(), anyString())).thenThrow(mock(FeignException.class));

        List<RouteStop> result = service.sequenceStops(UUID.randomUUID(), UUID.randomUUID(),
                stops, UUID.randomUUID(), "Bearer token");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSequenceNumber()).isEqualTo(1);
    }
}
