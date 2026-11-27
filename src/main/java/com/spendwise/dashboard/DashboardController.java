package com.spendwise.dashboard;

import com.spendwise.budget.BudgetRepository;
import com.spendwise.budget.Budget;
import com.spendwise.expense.Expense;
import com.spendwise.expense.ExpenseController.ExpenseView;
import com.spendwise.expense.ExpenseRepository;
import com.spendwise.expense.MonthlyTrend;
import com.spendwise.expense.PaymentFilter;
import com.spendwise.salary.SalaryRecord;
import com.spendwise.salary.SalaryRecordRepository;
import com.spendwise.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class DashboardController {
    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final SalaryRecordRepository salaries;

    public DashboardController(ExpenseRepository expenses, BudgetRepository budgets, SalaryRecordRepository salaries) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.salaries = salaries;
    }

    @GetMapping("/dashboard")
    DashboardSummary summary(@AuthenticationPrincipal UserPrincipal principal) {
        LocalDate month = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant from = month.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal spent = expenses.sumByUserAndDateRange(principal.id(), from, to);
        BigDecimal salary = salaries.findFirstByUserIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        principal.id(), LocalDate.now(ZoneOffset.UTC))
                .map(SalaryRecord::getTakeHomeSalary).orElse(BigDecimal.ZERO);
        List<Budget> monthlyBudgets = budgets.findAllByUserIdAndBudgetMonthOrderByNameAsc(principal.id(), month);
        BigDecimal budget = monthlyBudgets.stream()
                .filter(item -> item.getCategory() == null)
                .map(Budget::getAmount)
                .findFirst()
                .orElseGet(() -> monthlyBudgets.stream().map(Budget::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        List<ExpenseView> recent = expenses.findTop10ByUserIdOrderByExpenseDateTimeDesc(principal.id())
                .stream().map(ExpenseView::from).toList();
        return new DashboardSummary(month, salary, spent, salary.subtract(spent), budget, recent);
    }

    @GetMapping("/analytics")
    AnalyticsSummary analytics(@AuthenticationPrincipal UserPrincipal principal,
                               @RequestParam(required = false) String paymentMethod,
                               @RequestParam(required = false) String paymentPlatform) {
        PaymentFilter filter = PaymentFilter.from(paymentMethod, paymentPlatform);
        LocalDate current = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        List<YearMonth> months = MonthlyTrend.lastSixMonths(current);
        Map<String, BigDecimal> totals = MonthlyTrend.totalsByMonth(expenses.sumMonthlyByUserFiltered(
                principal.id(), MonthlyTrend.startUtc(months.getFirst()),
                MonthlyTrend.startUtc(months.getLast().plusMonths(1)),
                filter.paymentMethod(), filter.paymentPlatform()));
        List<MonthlyTotal> monthly = months.stream()
                .map(month -> new MonthlyTotal(month.toString(), totals.getOrDefault(month.toString(), BigDecimal.ZERO)))
                .toList();
        Instant start = current.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = current.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<CategoryTotal> categories = expenses
                .sumByUserAndCategoryAndDateRangeFiltered(
                        principal.id(), start, end, filter.paymentMethod(), filter.paymentPlatform()).stream()
                .map(total -> new CategoryTotal(total.getCategory(), total.getAmount())).toList();
        return new AnalyticsSummary(monthly, categories);
    }

    public record DashboardSummary(LocalDate month, BigDecimal monthlyIncome, BigDecimal monthSpend,
                                   BigDecimal netBalance, BigDecimal monthlyBudget, List<ExpenseView> recentExpenses) {}
    public record MonthlyTotal(String month, BigDecimal amount) {}
    public record CategoryTotal(String category, BigDecimal amount) {}
    public record AnalyticsSummary(List<MonthlyTotal> monthlyTrend, List<CategoryTotal> categoryBreakdown) {}
}
