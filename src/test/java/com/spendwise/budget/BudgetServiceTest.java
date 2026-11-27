package com.spendwise.budget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spendwise.expense.ExpenseRepository;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    @Mock
    private BudgetRepository budgets;
    @Mock
    private BudgetAlertRepository alerts;
    @Mock
    private ExpenseRepository expenses;
    @Mock
    private UserRepository users;

    @Test
    void calculatesBudgetUsageWithBigDecimalPrecision() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "A User", "user@example.test", "hash", "USER", "ACTIVE");
        LocalDate month = LocalDate.of(2026, 11, 1);
        Budget budget = new Budget(UUID.randomUUID(), user, "Monthly", null, month, new BigDecimal("100.00"));
        when(budgets.findAllByUserIdAndBudgetMonthOrderByNameAsc(userId, month)).thenReturn(List.of(budget));
        BudgetRepository.BudgetSpending spending = org.mockito.Mockito.mock(BudgetRepository.BudgetSpending.class);
        when(spending.getBudgetId()).thenReturn(budget.getId());
        when(spending.getSpentAmount()).thenReturn(new BigDecimal("33.33"));
        when(budgets.findMonthlySpendingByUserId(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(month),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(spending));

        BudgetService service = new BudgetService(budgets, alerts, expenses, users);
        BudgetController.BudgetView result = service.list(userId, month).getFirst();

        assertEquals(new BigDecimal("33.33"), result.spent());
        assertEquals(new BigDecimal("33.33"), result.percentage());
    }

    @Test
    void createsTheConfiguredThresholdAlertWhenReached() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "A User", "user@example.test", "hash", "USER", "ACTIVE");
        LocalDate month = LocalDate.of(2026, 11, 1);
        Budget budget = new Budget(UUID.randomUUID(), user, "Monthly", null, month,
                new BigDecimal("100.00"), 75);
        when(budgets.findAllByUserIdAndBudgetMonthOrderByNameAsc(userId, month)).thenReturn(List.of(budget));
        BudgetRepository.BudgetSpending spending = org.mockito.Mockito.mock(BudgetRepository.BudgetSpending.class);
        when(spending.getBudgetId()).thenReturn(budget.getId());
        when(spending.getSpentAmount()).thenReturn(new BigDecimal("80.00"));
        when(budgets.findMonthlySpendingByUserId(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(month), any(), any()))
                .thenReturn(List.of(spending));
        when(alerts.findAlertKeysByUserIdAndMonth(userId, month)).thenReturn(List.of());
        BudgetService service = new BudgetService(budgets, alerts, expenses, users);
        service.evaluateAlerts(userId, month);

        ArgumentCaptor<BudgetAlert> captor = ArgumentCaptor.forClass(BudgetAlert.class);
        verify(alerts).save(captor.capture());
        assertEquals(List.of(75), captor.getAllValues().stream()
                .map(BudgetAlert::getThresholdPercentage).toList());
    }

    @Test
    void doesNotDuplicateAnAlertThatWasAlreadyRaised() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "A User", "user@example.test", "hash", "USER", "ACTIVE");
        LocalDate month = LocalDate.of(2026, 11, 1);
        Budget budget = new Budget(UUID.randomUUID(), user, "Monthly", null, month,
                new BigDecimal("100.00"), 75);
        when(budgets.findAllByUserIdAndBudgetMonthOrderByNameAsc(userId, month)).thenReturn(List.of(budget));
        BudgetRepository.BudgetSpending spending = org.mockito.Mockito.mock(BudgetRepository.BudgetSpending.class);
        when(spending.getBudgetId()).thenReturn(budget.getId());
        when(spending.getSpentAmount()).thenReturn(new BigDecimal("90.00"));
        when(budgets.findMonthlySpendingByUserId(eq(userId), eq(month), any(), any()))
                .thenReturn(List.of(spending));
        BudgetAlertRepository.AlertKey existing = org.mockito.Mockito.mock(BudgetAlertRepository.AlertKey.class);
        when(existing.getBudgetId()).thenReturn(budget.getId());
        when(existing.getThresholdPercentage()).thenReturn(75);
        when(alerts.findAlertKeysByUserIdAndMonth(userId, month)).thenReturn(List.of(existing));

        new BudgetService(budgets, alerts, expenses, users).evaluateAlerts(userId, month);

        verify(alerts, never()).save(any());
    }
}
