package com.piotrcapecki.bakelivery.customer.repository;

import com.piotrcapecki.bakelivery.customer.model.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, UUID> {
    List<DeliveryAddress> findAllByCustomerId(UUID customerId);
    Optional<DeliveryAddress> findByIdAndCustomerId(UUID id, UUID customerId);
}
