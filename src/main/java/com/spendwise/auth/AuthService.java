package com.spendwise.auth;

import com.spendwise.auth.AuthController.AuthResponse;
import com.spendwise.auth.AuthController.LoginRequest;
import com.spendwise.auth.AuthController.ProfileUpdateRequest;
import com.spendwise.auth.AuthController.UserView;
import com.spendwise.security.JwtService;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       @Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public UserView createUser(String fullName, String rawEmail, String rawPassword) {
        requireValidPassword(rawPassword);
        String email = rawEmail.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = users.save(new User(UUID.randomUUID(), fullName.trim(), email,
                passwordEncoder.encode(rawPassword), "USER", "ACTIVE"));
        return new UserView(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Email or password is incorrect"));
        return issue(user);
    }

    @Transactional
    public UserView updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        String newPassword = request.newPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            requireValidPassword(newPassword);
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        } else if (request.currentPassword() != null && !request.currentPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a new password to change your password");
        }

        user.updateProfile(request.fullName().trim(), email);
        User saved = users.save(user);
        return new UserView(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserView updateManagedUser(UUID userId, String fullName, String rawEmail,
                                      String newPassword) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String email = rawEmail.trim().toLowerCase();
        if (users.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        if (newPassword != null && !newPassword.isBlank()) {
            requireValidPassword(newPassword);
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        user.updateProfile(fullName.trim(), email);
        User saved = users.save(user);
        return new UserView(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserView updateManagedUserStatus(UUID userId, String status) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!"USER".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrators can only change regular user account status");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or INACTIVE");
        }
        if ("INACTIVE".equals(status) && !"INACTIVE".equals(user.getStatus())) {
            refreshTokens.revokeAllByUserId(userId, Instant.now());
        }
        user.updateStatus(status);
        User saved = users.save(user);
        return new UserView(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        RefreshToken oldToken = refreshTokens.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null && token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"));
        oldToken.revoke(Instant.now());
        if (!"ACTIVE".equals(oldToken.getUser().getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive");
        }
        return issue(oldToken.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthResponse issue(User user) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        refreshTokens.save(new RefreshToken(UUID.randomUUID(), user, hash(rawRefreshToken),
                now.plusSeconds(refreshTokenDays * 86_400)));
        return new AuthResponse(jwtService.createAccessToken(user), rawRefreshToken,
                jwtService.accessTokenExpiresAt(),
                new UserView(user.getId(), user.getFullName(), user.getEmail(), user.getRole()));
    }

    private void requireValidPassword(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
