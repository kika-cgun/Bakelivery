package com.piotrcapecki.bakelivery.driverops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "driver_shifts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverShift {

    @Id
    private UUID id;

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "route_plan_id", nullable = false)
    private UUID routePlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShiftStatus status;

    @Column(name = "current_stop_index", nullable = false)
    private int currentStopIndex;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
