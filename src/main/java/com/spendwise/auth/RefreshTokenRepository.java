package com.spendwise.auth;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
            + "where token.user.id = :userId and token.revokedAt is null")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);

    /** Removes tokens that expired or were revoked before the cutoff; neither can be used again. */
    @Modifying
    @Query("delete from RefreshToken token where token.expiresAt < :cutoff or token.revokedAt < :cutoff")
    int deleteStale(@Param("cutoff") Instant cutoff);
}
