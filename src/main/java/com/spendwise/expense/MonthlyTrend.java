package com.spendwise.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Helpers for the six-month trend charts: month buckets are UTC "yyyy-MM" strings. */
public final class MonthlyTrend {
    private MonthlyTrend() {
    }

    /** The six calendar months ending with {@code currentMonth}, oldest first. */
    public static List<YearMonth> lastSixMonths(LocalDate currentMonth) {
        YearMonth end = YearMonth.from(currentMonth);
        return IntStream.rangeClosed(0, 5).mapToObj(index -> end.minusMonths(5L - index)).toList();
    }

    public static Instant startUtc(YearMonth month) {
        return month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /** Maps "yyyy-MM" buckets to totals. */
    public static Map<String, BigDecimal> totalsByMonth(List<ExpenseRepository.MonthTotal> rows) {
        return rows.stream().collect(Collectors.toMap(
                ExpenseRepository.MonthTotal::getMonth,
                ExpenseRepository.MonthTotal::getAmount,
                BigDecimal::add));
    }
}
