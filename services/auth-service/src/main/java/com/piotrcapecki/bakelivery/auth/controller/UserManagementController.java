package com.piotrcapecki.bakelivery.auth.controller;

import com.piotrcapecki.bakelivery.auth.dto.CreateEmployeeRequest;
import com.piotrcapecki.bakelivery.auth.model.User;
import com.piotrcapecki.bakelivery.auth.service.UserManagementService;
import com.piotrcapecki.bakelivery.common.exception.ForbiddenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('BAKERY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UserSummary> createEmployee(@AuthenticationPrincipal User actor,
                                                       @Valid @RequestBody CreateEmployeeRequest req) {
        UUID bakeryId = actor.getBakery() != null ? actor.getBakery().getId() : null;
        if (bakeryId == null) {
            throw new ForbiddenException("Cross-bakery operations not allowed via this endpoint");
        }
        User created = service.createEmployee(bakeryId, req);
        return ResponseEntity.ok(new UserSummary(created.getId(), created.getEmail(), created.getRole().name()));
    }

    public record UserSummary(UUID id, String email, String role) {}
}
