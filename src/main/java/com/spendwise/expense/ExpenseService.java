package com.spendwise.expense;

import com.spendwise.budget.BudgetService;
import com.spendwise.expense.ExpenseController.ExpenseRequest;
import com.spendwise.expense.ExpenseController.ExpenseView;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final UserRepository users;
    private final BudgetService budgets;

    public ExpenseService(ExpenseRepository expenses, UserRepository users, BudgetService budgets) {
        this.expenses = expenses;
        this.users = users;
        this.budgets = budgets;
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> list(UUID userId, Instant from, Instant to, String category) {
        List<Expense> results;
        if (from != null && to != null) {
            if (!from.isBefore(to)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before 'to'");
            }
            results = expenses.findFiltered(userId, from, to, normalize(category));
        } else if (from == null && to == null && category == null) {
            results = expenses.findAllByUserIdOrderByExpenseDateTimeDesc(userId);
        } else {
            Instant start = from == null ? Instant.EPOCH : from;
            Instant end = to == null ? Instant.parse("9999-12-31T23:59:59Z") : to;
            results = expenses.findFiltered(userId, start, end, normalize(category));
        }
        return results.stream().map(ExpenseView::from).toList();
    }

    @Transactional
    public ExpenseView create(UUID userId, ExpenseRequest request) {
        User user = users.getReferenceById(userId);
        String paymentPlatform = paymentPlatform(request.paymentMethod(), request.paymentPlatform());
        Expense expense = new Expense(UUID.randomUUID(), user, request.item().trim(), request.amount(),
                request.category().trim(), request.paymentMethod().trim().toUpperCase(), paymentPlatform,
                blankToNull(request.transactionId()), request.expenseDateTime(), blankToNull(request.notes()));
        Expense saved = expenses.save(expense);
        budgets.evaluateAlerts(userId, budgetMonth(saved.getExpenseDateTime()));
        return ExpenseView.from(saved);
    }

    @Transactional
    public ExpenseView update(UUID userId, UUID id, ExpenseRequest request) {
        Expense expense = ownedExpense(userId, id);
        LocalDate oldMonth = budgetMonth(expense.getExpenseDateTime());
        String paymentPlatform = paymentPlatform(request.paymentMethod(), request.paymentPlatform());
        expense.update(request.item().trim(), request.amount(), request.category().trim(),
                request.paymentMethod().trim().toUpperCase(), paymentPlatform,
                blankToNull(request.transactionId()),
                request.expenseDateTime(), blankToNull(request.notes()));
        budgets.evaluateAlerts(userId, oldMonth);
        LocalDate newMonth = budgetMonth(expense.getExpenseDateTime());
        if (!newMonth.equals(oldMonth)) budgets.evaluateAlerts(userId, newMonth);
        return ExpenseView.from(expense);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Expense expense = ownedExpense(userId, id);
        LocalDate month = budgetMonth(expense.getExpenseDateTime());
        expenses.delete(expense);
        budgets.evaluateAlerts(userId, month);
    }

    private Expense ownedExpense(UUID userId, UUID id) {
        return expenses.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private String normalize(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String paymentPlatform(String method, String platform) {
        if (!"UPI".equalsIgnoreCase(method.trim())) return null;
        String normalized = blankToNull(platform);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose the UPI app used for this payment");
        }
        if (!Set.of("GOOGLE_PAY", "PHONEPE", "PAYTM", "BHIM", "AMAZON_PAY", "OTHER")
                .contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported UPI app");
        }
        return normalized;
    }

    private LocalDate budgetMonth(Instant dateTime) {
        return dateTime.atZone(java.time.ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
    }
}
