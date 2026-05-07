package com.piotrcapecki.bakelivery.dispatching.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "dispatch_stops",
    uniqueConstraints = @UniqueConstraint(name = "uq_stop_order", columnNames = {"date", "order_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchStop {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "fixed_point_id")
    private UUID fixedPointId;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    private Double lat;

    private Double lon;

    @Column(name = "assigned_driver_id")
    private UUID assignedDriverId;

    @Column(name = "assigned_driver_name", length = 200)
    private String assignedDriverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DispatchStopStatus status = DispatchStopStatus.PENDING;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
