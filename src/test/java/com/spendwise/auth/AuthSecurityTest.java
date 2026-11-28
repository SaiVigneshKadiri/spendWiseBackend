package com.spendwise.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spendwise.auth.AuthController.LoginRequest;
import com.spendwise.security.JwtService;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthSecurityTest {
    @Mock
    private UserRepository users;
    @Mock
    private RefreshTokenRepository refreshTokens;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @Test
    void presentingARevokedRefreshTokenEndsEverySessionOfThatUser() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "User", "user@example.com", "hash", "USER", "ACTIVE");
        RefreshToken rotated = new RefreshToken(UUID.randomUUID(), user, "hash-1", Instant.now().plusSeconds(3600));
        rotated.revoke(Instant.now());
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(rotated));

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.refresh("copied-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(refreshTokens).revokeAllByUserId(eq(userId), any(Instant.class));
    }

    @Test
    void unknownEmailStillRunsAPasswordCheckSoTimingDoesNotRevealAccounts() {
        when(users.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("nobody@example.com", "Some-pass1!")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(passwordEncoder).matches(eq("Some-pass1!"), any());
    }
}
