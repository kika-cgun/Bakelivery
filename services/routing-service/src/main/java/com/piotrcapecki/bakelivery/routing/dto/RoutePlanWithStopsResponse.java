package com.piotrcapecki.bakelivery.routing.dto;

import java.util.List;

public record RoutePlanWithStopsResponse(RoutePlanResponse plan, List<RouteStopResponse> stops) {}
