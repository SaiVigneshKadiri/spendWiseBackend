package com.spendwise.auth;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    public static final String MESSAGE =
            "Password must be at least 6 characters and include an uppercase letter, "
                    + "a lowercase letter, a number, and a special character";

    private static final int BCRYPT_MAX_BYTES = 72; // BCrypt silently truncates beyond this, and Spring rejects it
    private static final Pattern VALID_PASSWORD = Pattern.compile(
            "\\A(?=.{6,72}\\z)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).*\\z");

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        return password != null && VALID_PASSWORD.matcher(password).matches()
                && password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
