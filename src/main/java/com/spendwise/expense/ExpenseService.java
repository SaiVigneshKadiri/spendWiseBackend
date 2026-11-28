package com.spendwise.expense;

import com.spendwise.budget.BudgetService;
import com.spendwise.common.FlowLog;
import com.spendwise.expense.ExpenseController.ExpenseRequest;
import com.spendwise.expense.ExpenseController.ExpenseView;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseService {
    private static final Logger LOG = LoggerFactory.getLogger(ExpenseService.class);
    private static final Set<String> UPI_APPS = Set.of("GOOGLE_PAY", "PHONEPE", "PAYTM", "BHIM", "AMAZON_PAY", "OTHER");

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
            results = FlowLog.step("expense.list", "query", () -> expenses.findFiltered(userId, from, to, normalize(category)));
        } else if (from == null && to == null && category == null) {
            results = FlowLog.step("expense.list", "query", () -> expenses.findAllByUserIdOrderByExpenseDateTimeDesc(userId));
        } else {
            Instant start = from == null ? Instant.EPOCH : from;
            Instant end = to == null ? Instant.parse("9999-12-31T23:59:59Z") : to;
            results = FlowLog.step("expense.list", "query", () -> expenses.findFiltered(userId, start, end, normalize(category)));
        }
        LOG.info("flow=expense.list rows={}", results.size());
        return results.stream().map(ExpenseView::from).toList();
    }

    @Transactional
    public ExpenseView create(UUID userId, ExpenseRequest request) {
        String method = paymentMethod(request.paymentMethod());
        String platform = paymentPlatform(method, request.paymentPlatform());
        User user = users.getReferenceById(userId);
        Expense expense = new Expense(UUID.randomUUID(), user, request.item().trim(), request.amount(),
                request.category().trim(), method, platform,
                blankToNull(request.transactionId()), request.expenseDateTime(), blankToNull(request.notes()));
        Expense saved = FlowLog.step("expense.create", "save", () -> expenses.save(expense));
        FlowLog.run("expense.create", "evaluate-budgets",
                () -> budgets.evaluateAlerts(userId, budgetMonth(saved.getExpenseDateTime())));
        LOG.info("flow=expense.create result=ok expense={}", saved.getId());
        return ExpenseView.from(saved);
    }

    @Transactional
    public ExpenseView update(UUID userId, UUID id, ExpenseRequest request) {
        Expense expense = ownedExpense(userId, id);
        LocalDate oldMonth = budgetMonth(expense.getExpenseDateTime());
        String method = paymentMethod(request.paymentMethod());
        String platform = paymentPlatform(method, request.paymentPlatform());
        expense.update(request.item().trim(), request.amount(), request.category().trim(),
                method, platform, blankToNull(request.transactionId()),
                request.expenseDateTime(), blankToNull(request.notes()));
        FlowLog.run("expense.update", "evaluate-budgets", () -> {
            budgets.evaluateAlerts(userId, oldMonth);
            LocalDate newMonth = budgetMonth(expense.getExpenseDateTime());
            if (!newMonth.equals(oldMonth)) budgets.evaluateAlerts(userId, newMonth);
        });
        LOG.info("flow=expense.update result=ok expense={}", id);
        return ExpenseView.from(expense);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Expense expense = ownedExpense(userId, id);
        LocalDate month = budgetMonth(expense.getExpenseDateTime());
        FlowLog.run("expense.delete", "delete", () -> expenses.delete(expense));
        FlowLog.run("expense.delete", "evaluate-budgets", () -> budgets.evaluateAlerts(userId, month));
        LOG.info("flow=expense.delete result=ok expense={}", id);
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

    /** Only the methods in PaymentFilter are stored, so analytics can always see every expense. */
    private String paymentMethod(String raw) {
        String method = raw.trim().toUpperCase(Locale.ROOT);
        if (!PaymentFilter.METHODS.contains(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method");
        }
        return method;
    }

    private String paymentPlatform(String method, String platform) {
        if (!"UPI".equals(method)) return null;
        String normalized = blankToNull(platform);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose the UPI app used for this payment");
        }
        if (!UPI_APPS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported UPI app");
        }
        return normalized;
    }

    private LocalDate budgetMonth(Instant dateTime) {
        return dateTime.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
    }
}
