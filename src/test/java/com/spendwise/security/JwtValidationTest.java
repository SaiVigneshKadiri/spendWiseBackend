package com.spendwise.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtValidationTest {
    private static final String KEY = "dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWZvci1wcm9kdWN0aW9uLTEyMzQ1Njc4OTA=";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(KEY));

    @Test
    void acceptsTokensIssuedByTheService() {
        UUID userId = UUID.randomUUID();
        JwtService service = new JwtService(KEY, 15);
        String token = service.createAccessToken(new com.spendwise.user.User(
                userId, "A", "a@example.test", "hash", "USER", "ACTIVE"));

        assertEquals(userId, service.userId(token));
    }

    @Test
    void rejectsTokensWithoutTheExpectedIssuer() {
        String token = Jwts.builder().subject(UUID.randomUUID().toString())
                .claim("typ", "access")
                .audience().add(JwtService.AUDIENCE).and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SIGNING_KEY).compact();

        assertThrows(JwtException.class, () -> new JwtService(KEY, 15).userId(token));
    }

    @Test
    void rejectsTokensOfAnotherType() {
        String token = Jwts.builder().subject(UUID.randomUUID().toString())
                .issuer(JwtService.ISSUER)
                .audience().add(JwtService.AUDIENCE).and()
                .claim("typ", "refresh")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SIGNING_KEY).compact();

        assertThrows(JwtException.class, () -> new JwtService(KEY, 15).userId(token));
    }
}
