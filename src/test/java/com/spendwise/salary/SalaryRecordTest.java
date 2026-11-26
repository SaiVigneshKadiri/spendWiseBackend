package com.spendwise.salary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.spendwise.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalaryRecordTest {
    @Test
    void updatesSalaryAmountsAndEffectiveDate() {
        User user = new User(UUID.randomUUID(), "Test User", "test@example.com",
                "hash", "USER", "ACTIVE");
        SalaryRecord record = new SalaryRecord(UUID.randomUUID(), user,
                new BigDecimal("50000.00"), new BigDecimal("42000.00"),
                LocalDate.of(2026, 1, 1));

        record.update(new BigDecimal("60000.00"), new BigDecimal("51000.00"),
                LocalDate.of(2026, 2, 1));

        assertEquals(new BigDecimal("60000.00"), record.getGrossSalary());
        assertEquals(new BigDecimal("51000.00"), record.getTakeHomeSalary());
        assertEquals(LocalDate.of(2026, 2, 1), record.getEffectiveFrom());
    }
}
