package com.spendwise.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spendwise.security.JwtService;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository users;
    @Mock
    private RefreshTokenRepository refreshTokens;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @Test
    void adminCreatedAccountsAreActiveRegularUsersWithHashedPasswords() {
        when(users.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Long-enough1!")).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        AuthController.UserView result = service.createUser(
                " New User ", " NewUser@Example.com ", "Long-enough1!");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(users).save(savedUser.capture());
        assertEquals("newuser@example.com", result.email());
        assertEquals("USER", result.role());
        assertEquals("New User", savedUser.getValue().getFullName());
        assertEquals("ACTIVE", savedUser.getValue().getStatus());
        assertEquals("encoded-password", savedUser.getValue().getPasswordHash());
        assertEquals(result.id(), savedUser.getValue().getId());
    }

    @Test
    void updatesAccountIdentityWithoutChangingPassword() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Old Name", "old@example.com", "existing-hash",
                "USER", "ACTIVE");
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", userId))
                .thenReturn(false);
        when(users.save(user)).thenReturn(user);

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        AuthController.UserView result = service.updateProfile(
                userId,
                new AuthController.ProfileUpdateRequest(" New Name ", "New@Example.com",
                        null, null));

        assertEquals("New Name", result.fullName());
        assertEquals("new@example.com", result.email());
        assertEquals("existing-hash", user.getPasswordHash());
    }

    @Test
    void verifiesCurrentPasswordBeforeChangingIt() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Name", "user@example.com", "existing-hash",
                "USER", "ACTIVE");
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.existsByEmailIgnoreCaseAndIdNot(eq("user@example.com"), eq(userId)))
                .thenReturn(false);
        when(passwordEncoder.matches("Wrong-Password1!", "existing-hash")).thenReturn(false);

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        org.springframework.web.server.ResponseStatusException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.updateProfile(
                        userId,
                        new AuthController.ProfileUpdateRequest("Name", "user@example.com",
                                "Wrong-Password1!", "New-Password1!")));
        assertEquals("Current password is incorrect", exception.getReason());
    }

    @Test
    void adminCanResetManagedUserPasswordWithoutCurrentPassword() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Old Name", "old@example.com", "existing-hash",
                "USER", "ACTIVE");
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", userId)).thenReturn(false);
        when(passwordEncoder.encode("New-Password1!")).thenReturn("new-hash");
        when(users.save(user)).thenReturn(user);

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        AuthController.UserView result = service.updateManagedUser(
                userId, " New Name ", "New@Example.com", "New-Password1!");

        assertEquals("New Name", result.fullName());
        assertEquals("new@example.com", result.email());
        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void adminCanEditManagedUserWithoutChangingPassword() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Old Name", "old@example.com", "existing-hash",
                "USER", "ACTIVE");
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", userId)).thenReturn(false);
        when(users.save(user)).thenReturn(user);

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        service.updateManagedUser(userId, "New Name", "new@example.com", null);

        assertEquals("existing-hash", user.getPasswordHash());
    }

    @Test
    void deactivatingManagedUserPreservesAccountAndRevokesRefreshTokens() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "User", "user@example.com", "password-hash",
                "USER", "ACTIVE");
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.save(user)).thenReturn(user);

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        service.updateManagedUserStatus(userId, "INACTIVE");

        assertEquals("INACTIVE", user.getStatus());
        verify(users).save(user);
        verify(refreshTokens).revokeAllByUserId(eq(userId), any(java.time.Instant.class));
    }

    @Test
    void reactivatingManagedUserDoesNotRevokeRefreshTokensAgain() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "User", "user@example.com", "password-hash",
                "USER", "INACTIVE");
        when(users.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(users.save(user)).thenReturn(user);

        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);
        service.updateManagedUserStatus(userId, "ACTIVE");

        assertEquals("ACTIVE", user.getStatus());
        verify(refreshTokens, org.mockito.Mockito.never())
                .revokeAllByUserId(any(), any(java.time.Instant.class));
    }

    @Test
    void rejectsPasswordsThatDoNotMeetComplexityPolicy() {
        UUID userId = UUID.randomUUID();
        AuthService service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 30);

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.createUser("Name", "user@example.com", "weak1!"));
        verify(users, org.mockito.Mockito.never()).save(any(User.class));
    }
}
