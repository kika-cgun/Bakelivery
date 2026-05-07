package com.piotrcapecki.bakelivery.dispatching.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "driver_territories",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_territory",
        columnNames = {"bakery_id", "driver_id", "fixed_point_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverTerritory {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "driver_name", nullable = false, length = 200)
    private String driverName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_point_id", nullable = false)
    private FixedDeliveryPoint fixedPoint;

    @Column(name = "affinity_score", nullable = false)
    @Builder.Default
    private int affinityScore = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
