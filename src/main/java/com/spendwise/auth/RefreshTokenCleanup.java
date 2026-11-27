package com.spendwise.auth;

import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens are rotated on every refresh, so without this job the table grows without bound.
 * Runs nightly; the one-day grace period keeps recent rows around for debugging.
 */
@Component
public class RefreshTokenCleanup {
    private static final Duration GRACE = Duration.ofDays(1);

    private final RefreshTokenRepository refreshTokens;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public int purgeStaleTokens() {
        return refreshTokens.deleteStale(Instant.now().minus(GRACE));
    }
}
