package com.piotrcapecki.bakelivery.dispatching.controller;

import com.piotrcapecki.bakelivery.dispatching.dto.DispatchStopResponse;
import com.piotrcapecki.bakelivery.dispatching.security.DispatchPrincipal;
import com.piotrcapecki.bakelivery.dispatching.service.DispatchStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dispatch/driver")
@RequiredArgsConstructor
public class DriverDispatchController {

    private final DispatchStopService service;

    @GetMapping("/stops/today")
    public ResponseEntity<List<DispatchStopResponse>> today(@AuthenticationPrincipal DispatchPrincipal actor) {
        return ResponseEntity.ok(service.listByDateAndDriver(
                        actor.bakeryId(), LocalDate.now(), actor.userId())
                .stream().map(DispatchStopResponse::from).toList());
    }
}
