package com.piotrcapecki.bakelivery.customer.repository;

import com.piotrcapecki.bakelivery.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUserIdAndBakeryId(UUID userId, UUID bakeryId);
}
