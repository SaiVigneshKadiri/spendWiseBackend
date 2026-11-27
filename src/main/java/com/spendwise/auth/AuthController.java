package com.spendwise.auth;

import com.spendwise.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request.fullName(), request.email(), request.password());
    }

    @PostMapping("/refresh")
    AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    CurrentUser currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return new CurrentUser(principal.id(), principal.fullName(), principal.username(), principal.role());
    }

    @PutMapping("/me")
    UserView updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                           @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(principal.id(), request);
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record SignUpRequest(@NotBlank @Size(max = 100) String fullName,
                                @NotBlank @Email @Size(max = 255) String email,
                                @NotBlank @Size(max = 72) String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record ProfileUpdateRequest(@NotBlank @Size(max = 100) String fullName,
                                       @NotBlank @Email @Size(max = 255) String email,
                                       String currentPassword,
                                       @Size(max = 72) String newPassword) {}
    public record UserView(UUID id, String fullName, String email, String role) {}
    public record CurrentUser(UUID id, String fullName, String email, String role) {}
    public record AuthResponse(String accessToken, String refreshToken, Instant accessTokenExpiresAt, UserView user) {}
}
