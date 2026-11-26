package com.spendwise.budget;

import com.spendwise.budget.BudgetController.BudgetRequest;
import com.spendwise.budget.BudgetController.BudgetView;
import com.spendwise.expense.ExpenseRepository;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final BudgetAlertRepository alerts;
    private final ExpenseRepository expenses;
    private final UserRepository users;

    public BudgetService(BudgetRepository budgets, BudgetAlertRepository alerts,
                         ExpenseRepository expenses, UserRepository users) {
        this.budgets = budgets;
        this.alerts = alerts;
        this.expenses = expenses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<BudgetView> list(UUID userId, LocalDate month) {
        Instant from = month.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<UUID, BigDecimal> spending = budgets.findMonthlySpendingByUserId(userId, month, from, to).stream()
                .collect(Collectors.toMap(BudgetRepository.BudgetSpending::getBudgetId,
                        BudgetRepository.BudgetSpending::getSpentAmount));
        return budgets.findAllByUserIdAndBudgetMonthOrderByNameAsc(userId, month)
                .stream().map(budget -> view(budget, spending.getOrDefault(budget.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public BudgetView create(UUID userId, BudgetRequest request) {
        User user = users.getReferenceById(userId);
        Budget budget = new Budget(UUID.randomUUID(), user, request.name().trim(), clean(request.category()),
                request.budgetMonth().withDayOfMonth(1), request.amount(),
                request.alertPercentage() == null ? 80 : request.alertPercentage());
        Budget saved = budgets.save(budget);
        evaluateAlerts(userId, saved.getBudgetMonth());
        return view(saved);
    }

    @Transactional
    public BudgetView update(UUID userId, UUID id, BudgetRequest request) {
        Budget budget = ownedBudget(userId, id);
        budget.update(request.name().trim(), clean(request.category()),
                request.budgetMonth().withDayOfMonth(1), request.amount(),
                request.alertPercentage() == null ? budget.getAlertPercentage() : request.alertPercentage());
        evaluateAlerts(userId, budget.getBudgetMonth());
        return view(budget);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        budgets.delete(ownedBudget(userId, id));
    }

    @Transactional
    public void evaluateAlerts(UUID userId, LocalDate month) {
        LocalDate budgetMonth = month.withDayOfMonth(1);
        Instant from = budgetMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = budgetMonth.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<UUID, BigDecimal> spending = budgets.findMonthlySpendingByUserId(userId, budgetMonth, from, to)
                .stream().collect(Collectors.toMap(BudgetRepository.BudgetSpending::getBudgetId,
                        BudgetRepository.BudgetSpending::getSpentAmount));
        for (Budget budget : budgets.findAllByUserIdAndBudgetMonthOrderByNameAsc(userId, budgetMonth)) {
            BigDecimal spent = spending.getOrDefault(budget.getId(), BigDecimal.ZERO);
            int threshold = budget.getAlertPercentage();
            boolean crossed = spent.multiply(BigDecimal.valueOf(100))
                    .compareTo(budget.getAmount().multiply(BigDecimal.valueOf(threshold))) >= 0;
            if (crossed && !alerts.existsByBudgetIdAndThresholdPercentage(budget.getId(), threshold)) {
                    alerts.save(new BudgetAlert(UUID.randomUUID(), budget.getUser(), budget,
                            threshold, spent));
            }
        }
    }

    private Budget ownedBudget(UUID userId, UUID id) {
        return budgets.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private BudgetView view(Budget budget) {
        Instant from = budget.getBudgetMonth().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = budget.getBudgetMonth().plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return view(budget, spentFor(budget, from, to));
    }

    private BudgetView view(Budget budget, BigDecimal spent) {
        BigDecimal percentage = spent.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
        return new BudgetView(budget.getId(), budget.getName(), budget.getCategory(), budget.getBudgetMonth(),
                budget.getAmount(), spent, percentage, budget.getAlertPercentage());
    }

    private BigDecimal spentFor(Budget budget, Instant from, Instant to) {
        if (budget.getCategory() == null) {
            return expenses.sumByUserAndDateRange(budget.getUser().getId(), from, to);
        }
        return expenses.sumByUserAndCategoryAndDateRange(
                budget.getUser().getId(), budget.getCategory(), from, to);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
