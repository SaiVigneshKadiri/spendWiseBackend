package com.spendwise.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test
    void requiresSixCharactersAndEachCharacterClass() {
        assertTrue(PasswordPolicy.isValid("Ab1!cd"));
        assertFalse(PasswordPolicy.isValid("Ab1!c"));
        assertFalse(PasswordPolicy.isValid("ab1!cd"));
        assertFalse(PasswordPolicy.isValid("AB1!CD"));
        assertFalse(PasswordPolicy.isValid("Abc!de"));
        assertFalse(PasswordPolicy.isValid("Ab12cd"));
        assertFalse(PasswordPolicy.isValid("Ab1 cd"));
    }

    @Test
    void rejectsPasswordsLongerThanBcryptLimit() {
        assertFalse(PasswordPolicy.isValid("A1!" + "b".repeat(70)));
    }
}
