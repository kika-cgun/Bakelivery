package com.piotrcapecki.bakelivery.routing.repository;

import com.piotrcapecki.bakelivery.routing.model.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {
    List<RouteStop> findByRoutePlanIdOrderBySequenceNumberAsc(UUID routePlanId);
    void deleteByRoutePlanId(UUID routePlanId);
}
