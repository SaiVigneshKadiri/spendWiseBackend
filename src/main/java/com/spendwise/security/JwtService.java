package com.spendwise.security;

import com.spendwise.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Access tokens are HS256, short-lived, and must carry this issuer, audience and token type.
 * Refresh tokens are opaque and never accepted here.
 */
@Service
public class JwtService {
    static final String ISSUER = "spendwise-api";
    static final String AUDIENCE = "spendwise-app";
    private static final String TOKEN_TYPE = "access";

    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(user.getId().toString())
                .claim("typ", TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Instant accessTokenExpiresAt() {
        return Instant.now().plusSeconds(accessTokenMinutes * 60);
    }

    /** Verifies signature, expiry, issuer, audience and token type; throws JwtException otherwise. */
    public UUID userId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .require("typ", TOKEN_TYPE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }
}
