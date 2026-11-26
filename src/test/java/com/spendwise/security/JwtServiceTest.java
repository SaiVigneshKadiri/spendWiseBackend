package com.spendwise.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.spendwise.user.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String TEST_ONLY_KEY =
            "dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWZvci1wcm9kdWN0aW9uLTEyMzQ1Njc4OTA=";

    @Test
    void issuesShortLivedAccessTokenForTheUser() {
        JwtService service = new JwtService(TEST_ONLY_KEY, 15);
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "A User", "user@example.test", "hash", "USER", "ACTIVE");

        String token = service.createAccessToken(user);

        assertEquals(userId, service.userId(token));
        assertTrue(service.accessTokenExpiresAt().isAfter(java.time.Instant.now()));
    }
}
