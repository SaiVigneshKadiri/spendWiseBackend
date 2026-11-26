package com.spendwise.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PaymentFilterTest {
    @Test
    void normalizesMethodAndAppAndInfersUpi() {
        PaymentFilter filter = PaymentFilter.from(null, " google_pay ");

        assertEquals("UPI", filter.paymentMethod());
        assertEquals("GOOGLE_PAY", filter.paymentPlatform());
    }

    @Test
    void rejectsUnsupportedPaymentValues() {
        assertThrows(ResponseStatusException.class, () -> PaymentFilter.from("CHEQUE", null));
        assertThrows(ResponseStatusException.class, () -> PaymentFilter.from("CASH", "PHONEPE"));
    }
}
