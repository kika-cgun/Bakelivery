package com.piotrcapecki.bakelivery.customer.controller;

import com.piotrcapecki.bakelivery.common.exception.ForbiddenException;
import com.piotrcapecki.bakelivery.customer.dto.AddressResponse;
import com.piotrcapecki.bakelivery.customer.dto.CreateAddressRequest;
import com.piotrcapecki.bakelivery.customer.dto.UpdateAddressRequest;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;
import com.piotrcapecki.bakelivery.customer.service.DeliveryAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
public class DeliveryAddressController {

    private final DeliveryAddressService service;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(@AuthenticationPrincipal CustomerPrincipal actor) {
        requireBakery(actor);
        List<AddressResponse> result = service.list(actor.userId(), actor.bakeryId()).stream()
                .map(AddressResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal CustomerPrincipal actor,
                                                   @Valid @RequestBody CreateAddressRequest req) {
        requireBakery(actor);
        var a = service.create(actor.userId(), actor.bakeryId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressResponse.from(a));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@AuthenticationPrincipal CustomerPrincipal actor,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateAddressRequest req) {
        requireBakery(actor);
        var a = service.update(actor.userId(), actor.bakeryId(), id, req);
        return ResponseEntity.ok(AddressResponse.from(a));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomerPrincipal actor,
                                        @PathVariable UUID id) {
        requireBakery(actor);
        service.delete(actor.userId(), actor.bakeryId(), id);
        return ResponseEntity.noContent().build();
    }

    private void requireBakery(CustomerPrincipal actor) {
        if (actor == null || actor.bakeryId() == null) {
            throw new ForbiddenException("This endpoint requires a bakery-scoped account");
        }
    }
}
