package com.piotrcapecki.bakelivery.auth.repository;

import com.piotrcapecki.bakelivery.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t
            SET t.revoked = true
            WHERE t.tokenHash = :tokenHash
              AND t.revoked = false
              AND t.expiresAt > :now
            """)
    int revokeIfUsable(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.userId = :userId")
    void revokeAllForUser(@Param("userId") UUID userId);
}
