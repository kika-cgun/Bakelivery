package com.piotrcapecki.bakelivery.driverops.dto;

import java.util.List;

public record ShiftWithStopsResponse(
        ShiftResponse shift,
        StopProgressResponse currentStop,
        List<StopProgressResponse> allStops
) {}
