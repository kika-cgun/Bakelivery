package com.piotrcapecki.bakelivery.auth.repository;

import com.piotrcapecki.bakelivery.auth.model.Bakery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BakeryRepository extends JpaRepository<Bakery, UUID> {
    Optional<Bakery> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
