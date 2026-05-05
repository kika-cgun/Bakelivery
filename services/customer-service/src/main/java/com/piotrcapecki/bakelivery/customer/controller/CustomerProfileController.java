package com.piotrcapecki.bakelivery.customer.controller;

import com.piotrcapecki.bakelivery.customer.dto.ProfileResponse;
import com.piotrcapecki.bakelivery.customer.dto.UpsertProfileRequest;
import com.piotrcapecki.bakelivery.customer.model.Customer;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;
import com.piotrcapecki.bakelivery.customer.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController extends BaseCustomerController {

    private final CustomerProfileService service;

    @GetMapping
    public ResponseEntity<ProfileResponse> get(@AuthenticationPrincipal CustomerPrincipal actor) {
        requireBakery(actor);
        Customer c = service.getProfile(actor.userId(), actor.bakeryId());
        return ResponseEntity.ok(ProfileResponse.from(c));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> upsert(@AuthenticationPrincipal CustomerPrincipal actor,
                                                   @Valid @RequestBody UpsertProfileRequest req) {
        requireBakery(actor);
        Customer c = service.upsertProfile(actor.userId(), actor.bakeryId(), req);
        return ResponseEntity.ok(ProfileResponse.from(c));
    }
}
