package com.spendwise.config;

import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import com.spendwise.auth.PasswordPolicy;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${APP_ADMIN_EMAIL:}") String email,
                          @Value("${APP_ADMIN_PASSWORD:}") String password,
                          @Value("${APP_ADMIN_NAME:SpendWise Admin}") String fullName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Set both APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD");
        }
        if (users.existsByEmailIgnoreCase(email)) return;
        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalStateException(PasswordPolicy.MESSAGE);
        }
        users.save(new User(UUID.randomUUID(), fullName, email, passwordEncoder.encode(password),
                "ADMIN", "ACTIVE"));
    }
}
