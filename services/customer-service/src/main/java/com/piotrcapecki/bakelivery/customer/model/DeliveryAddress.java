package com.piotrcapecki.bakelivery.customer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "bakery_id", nullable = false)
    private UUID bakeryId;

    private String label;

    @Column(nullable = false)
    private String street;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String city;

    private Double latitude;

    private Double longitude;

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
