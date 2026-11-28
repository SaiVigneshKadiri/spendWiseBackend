package com.spendwise.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spendwise.budget.BudgetService;
import com.spendwise.expense.ExpenseController.ExpenseRequest;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
    @Mock
    private ExpenseRepository expenses;
    @Mock
    private UserRepository users;
    @Mock
    private BudgetService budgets;

    @Test
    void refusesToUpdateAnExpenseNotOwnedByTheUser() {
        UUID ownerId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        when(expenses.findByIdAndUserId(expenseId, ownerId)).thenReturn(Optional.empty());

        ExpenseService service = new ExpenseService(expenses, users, budgets);
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(ownerId, expenseId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void requiresAnAppWhenPaymentMethodIsUpi() {
        UUID userId = UUID.randomUUID();
        ExpenseService service = new ExpenseService(expenses, users, budgets);
        ExpenseRequest request = new ExpenseRequest(
                "Groceries", new BigDecimal("100.00"), "Food", "UPI",
                null, null, Instant.parse("2026-11-24T05:00:00Z"), null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.create(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(expenses, never()).save(any());
    }

    @Test
    void rejectsPaymentMethodsOutsideTheAllowedSet() {
        UUID userId = UUID.randomUUID();
        ExpenseService service = new ExpenseService(expenses, users, budgets);
        ExpenseRequest request = new ExpenseRequest(
                "Groceries", new BigDecimal("100.00"), "Food", "bitcoin",
                null, null, Instant.parse("2026-11-24T05:00:00Z"), null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> service.create(userId, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(expenses, never()).save(any());
    }

    @Test
    void discardsPaymentPlatformForNonUpiMethods() {
        UUID userId = UUID.randomUUID();
        when(users.getReferenceById(userId)).thenReturn(
                new User(userId, "A User", "user@example.test", "hash", "USER", "ACTIVE"));
        when(expenses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ExpenseService service = new ExpenseService(expenses, users, budgets);
        ExpenseRequest request = new ExpenseRequest(
                "Groceries", new BigDecimal("100.00"), "Food", "CARD",
                "GOOGLE_PAY", null, Instant.parse("2026-11-24T05:00:00Z"), null);

        service.create(userId, request);

        org.mockito.ArgumentCaptor<Expense> captor = org.mockito.ArgumentCaptor.forClass(Expense.class);
        verify(expenses).save(captor.capture());
        assertNull(captor.getValue().getPaymentPlatform());
    }
}
