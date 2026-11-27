package com.spendwise.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonthlyTrendTest {
    @Test
    void returnsSixMonthsEndingWithCurrentOldestFirst() {
        List<YearMonth> months = MonthlyTrend.lastSixMonths(LocalDate.of(2026, 3, 1));

        assertEquals(List.of("2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03"),
                months.stream().map(YearMonth::toString).toList());
    }

    @Test
    void sumsRowsIntoMonthBuckets() {
        Map<String, BigDecimal> totals = MonthlyTrend.totalsByMonth(List.of(
                row("2026-01", "10.00"), row("2026-01", "2.50"), row("2026-02", "7.00")));

        assertEquals(new BigDecimal("12.50"), totals.get("2026-01"));
        assertEquals(new BigDecimal("7.00"), totals.get("2026-02"));
    }

    private static ExpenseRepository.MonthTotal row(String month, String amount) {
        return new ExpenseRepository.MonthTotal() {
            @Override
            public String getMonth() {
                return month;
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(amount);
            }
        };
    }
}
