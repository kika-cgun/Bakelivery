package com.piotrcapecki.bakelivery.routing.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "route_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoutePlan {

    @Id @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoutePlanStatus status = RoutePlanStatus.PENDING;

    @Column(name = "total_distance_meters")
    private Double totalDistanceMeters;

    @Column(name = "total_duration_seconds")
    private Double totalDurationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
