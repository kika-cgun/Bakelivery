package com.piotrcapecki.bakelivery.routing.client;

import com.piotrcapecki.bakelivery.routing.client.dto.TripRequest;
import com.piotrcapecki.bakelivery.routing.client.dto.TripResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "maps-client", url = "${maps.service.url:http://localhost:8089}")
public interface MapsClient {

    @PostMapping("/api/maps/trip")
    TripResponse trip(@RequestBody TripRequest request, @RequestHeader("Authorization") String auth);
}
