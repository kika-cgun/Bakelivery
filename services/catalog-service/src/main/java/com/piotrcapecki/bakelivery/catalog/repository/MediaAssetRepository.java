package com.piotrcapecki.bakelivery.catalog.repository;

import com.piotrcapecki.bakelivery.catalog.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findAllByProductIdOrderBySortOrderAscCreatedAtAsc(UUID productId);
    Optional<MediaAsset> findByIdAndBakeryId(UUID id, UUID bakeryId);

    @Modifying
    @Query("UPDATE MediaAsset m SET m.primary = false WHERE m.productId = :productId AND m.id <> :keepId")
    void unsetPrimaryExcept(@Param("productId") UUID productId, @Param("keepId") UUID keepId);
}
