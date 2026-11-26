package com.spendwise.expense;

import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PaymentFilter(String paymentMethod, String paymentPlatform) {
    private static final Set<String> METHODS = Set.of("CASH", "CARD", "UPI", "BANK_TRANSFER");
    private static final Set<String> PLATFORMS =
            Set.of("GOOGLE_PAY", "PHONEPE", "PAYTM", "BHIM", "AMAZON_PAY", "OTHER");

    public static PaymentFilter from(String method, String platform) {
        String normalizedMethod = normalize(method, METHODS, "paymentMethod");
        String normalizedPlatform = normalize(platform, PLATFORMS, "paymentPlatform");
        if (normalizedPlatform != null && normalizedMethod != null
                && !"UPI".equals(normalizedMethod)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "paymentPlatform can only be used with UPI");
        }
        if (normalizedPlatform != null) normalizedMethod = "UPI";
        return new PaymentFilter(normalizedMethod, normalizedPlatform);
    }

    private static String normalize(String value, Set<String> allowed, String parameter) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + parameter);
        }
        return normalized;
    }
}
