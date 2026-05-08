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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stop_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopProgress {

    @Id
    private UUID id;

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "dispatch_stop_id", nullable = false)
    private UUID dispatchStopId;

    @Column(name = "route_stop_id", nullable = false)
    private UUID routeStopId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StopStatus status;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "skipped_reason")
    private String skippedReason;

    @Column(name = "proof_object_key")
    private String proofObjectKey;

    @Column(name = "eta_seconds")
    private Integer etaSeconds;
}
