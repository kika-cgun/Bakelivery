package com.piotrcapecki.bakelivery.dispatching.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fixed_delivery_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedDeliveryPoint {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    private Double lat;

    private Double lon;

    @Column(name = "delivery_days", nullable = false)
    @Builder.Default
    private short deliveryDays = 127;

    @Column(name = "default_notes", length = 500)
    private String defaultNotes;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
