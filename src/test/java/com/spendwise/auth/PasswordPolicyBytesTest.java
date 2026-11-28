package com.spendwise.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PasswordPolicyBytesTest {
    @Test
    void rejectsPasswordsThatExceedBcryptsByteLimitEvenWhenTheyFitTheCharacterLimit() {
        // 44 characters but 84 UTF-8 bytes: BCrypt cannot hash it.
        String password = "Ab1!" + "é".repeat(40);

        assertFalse(PasswordPolicy.isValid(password));
    }
}
