package com.spendwise.admin;

import com.spendwise.expense.ExpenseController.ExpenseView;
import com.spendwise.expense.ExpenseRepository;
import com.spendwise.expense.ExpenseService;
import com.spendwise.expense.MonthlyTrend;
import com.spendwise.expense.PaymentFilter;
import com.spendwise.auth.AuthService;
import com.spendwise.security.UserPrincipal;
import com.spendwise.user.User;
import com.spendwise.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final com.spendwise.budget.BudgetRepository budgets;
    private final ExpenseService expenseService;
    private final AuthService authService;

    public AdminController(UserRepository users, ExpenseRepository expenses,
                           com.spendwise.budget.BudgetRepository budgets,
                           ExpenseService expenseService, AuthService authService) {
        this.users = users;
        this.expenses = expenses;
        this.budgets = budgets;
        this.expenseService = expenseService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    AdminDashboard dashboard() {
        Instant now = Instant.now();
        LocalDate currentMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant monthStart = currentMonth.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nextMonthStart = currentMonth.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<YearMonth> months = MonthlyTrend.lastSixMonths(currentMonth);
        Map<String, BigDecimal> totals = MonthlyTrend.totalsByMonth(expenses.sumMonthlyAll(
                MonthlyTrend.startUtc(months.getFirst()), MonthlyTrend.startUtc(months.getLast().plusMonths(1))));
        List<MonthlyTotal> spendingTrend = months.stream()
                .map(month -> new MonthlyTotal(month.toString(), totals.getOrDefault(month.toString(), BigDecimal.ZERO)))
                .toList();

        List<com.spendwise.budget.BudgetRepository.BudgetSpending> currentBudgets =
                budgets.findMonthlySpending(currentMonth, monthStart, nextMonthStart);
        long budgetsAtRisk = 0;
        long budgetsOverLimit = 0;
        BigDecimal totalBudgetUtilization = BigDecimal.ZERO;
        for (com.spendwise.budget.BudgetRepository.BudgetSpending budget : currentBudgets) {
            BigDecimal percentage = budget.getSpentAmount().multiply(BigDecimal.valueOf(100))
                    .divide(budget.getBudgetAmount(), 2, RoundingMode.HALF_UP);
            totalBudgetUtilization = totalBudgetUtilization.add(percentage);
            if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) budgetsAtRisk++;
            if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) budgetsOverLimit++;
        }
        BigDecimal averageBudgetUtilization = currentBudgets.isEmpty()
                ? BigDecimal.ZERO
                : totalBudgetUtilization.divide(BigDecimal.valueOf(currentBudgets.size()), 2, RoundingMode.HALF_UP);

        List<TopSpenderView> topSpenders = expenses.findTopSpenders(
                        monthStart, nextMonthStart, PageRequest.of(0, 5)).stream()
                .map(spender -> new TopSpenderView(spender.getUserId(), spender.getFullName(),
                        spender.getEmail(), spender.getAmount()))
                .toList();
        List<AdminExpenseView> recentActivity = expenses.findTop10ByOrderByExpenseDateTimeDesc().stream()
                .map(AdminExpenseView::from)
                .toList();

        return new AdminDashboard(
                expenses.countUsersWithExpensesSince(now.minusSeconds(30L * 24 * 60 * 60)),
                users.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(monthStart, nextMonthStart),
                currentBudgets.size(), budgetsAtRisk, budgetsOverLimit, averageBudgetUtilization,
                spendingTrend,
                expenses.sumAllByCategoryAndDateRange(monthStart, nextMonthStart).stream()
                        .map(total -> new CategoryTotal(total.getCategory(), total.getAmount())).toList(),
                topSpenders, recentActivity);
    }

    @GetMapping("/users")
    List<UserView> listUsers() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(UserView::from).toList();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    com.spendwise.auth.AuthController.UserView createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUser(request.fullName(), request.email(), request.password());
    }

    @PutMapping("/users/{userId}")
    com.spendwise.auth.AuthController.UserView updateUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = requireManagedUser(principal, userId);
        if (!"USER".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrators can only edit regular user accounts");
        }
        return authService.updateManagedUser(
                userId, request.fullName(), request.email(), request.newPassword());
    }

    @PutMapping("/users/{userId}/status")
    com.spendwise.auth.AuthController.UserView updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        User user = requireManagedUser(principal, userId);
        if (!"USER".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrators can only change regular user account status");
        }
        return authService.updateManagedUserStatus(userId, request.status());
    }

    @GetMapping("/users/{userId}/expenses")
    @Transactional(readOnly = true)
    List<AdminExpenseView> recentExpenses(@AuthenticationPrincipal UserPrincipal principal,
                                          @PathVariable UUID userId) {
        requireManagedUser(principal, userId);
        return expenses.findTop500ByUserIdOrderByExpenseDateTimeDesc(userId).stream()
                .map(AdminExpenseView::from).toList();
    }

    @PostMapping("/users/{userId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    AdminExpenseView createExpense(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID userId, @Valid @RequestBody
                                   com.spendwise.expense.ExpenseController.ExpenseRequest request) {
        User owner = requireManagedUser(principal, userId);
        ExpenseView expense = expenseService.create(userId, request);
        return AdminExpenseView.from(expense, owner);
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{userId}/expenses/{expenseId}")
    AdminExpenseView updateExpense(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable UUID userId, @PathVariable UUID expenseId,
                                   @Valid @RequestBody
                                   com.spendwise.expense.ExpenseController.ExpenseRequest request) {
        User owner = requireManagedUser(principal, userId);
        ExpenseView expense = expenseService.update(userId, expenseId, request);
        return AdminExpenseView.from(expense, owner);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/{userId}/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteExpense(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable UUID userId, @PathVariable UUID expenseId) {
        requireManagedUser(principal, userId);
        expenseService.delete(userId, expenseId);
    }

    @GetMapping("/users/{userId}/analytics")
    AnalyticsSummary analytics(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable UUID userId,
                               @RequestParam(required = false) String paymentMethod,
                               @RequestParam(required = false) String paymentPlatform) {
        requireManagedUser(principal, userId);
        PaymentFilter filter = PaymentFilter.from(paymentMethod, paymentPlatform);
        LocalDate current = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        List<YearMonth> months = MonthlyTrend.lastSixMonths(current);
        Map<String, BigDecimal> totals = MonthlyTrend.totalsByMonth(expenses.sumMonthlyByUserFiltered(
                userId, MonthlyTrend.startUtc(months.getFirst()),
                MonthlyTrend.startUtc(months.getLast().plusMonths(1)),
                filter.paymentMethod(), filter.paymentPlatform()));
        List<MonthlyTotal> monthly = months.stream()
                .map(month -> new MonthlyTotal(month.toString(), totals.getOrDefault(month.toString(), BigDecimal.ZERO)))
                .toList();
        Instant start = current.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = current.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<CategoryTotal> categories = expenses.sumByUserAndCategoryAndDateRangeFiltered(
                userId, start, end, filter.paymentMethod(), filter.paymentPlatform()).stream()
                .map(total -> new CategoryTotal(total.getCategory(), total.getAmount())).toList();
        return new AnalyticsSummary(monthly, categories);
    }

    private User requireManagedUser(UserPrincipal principal, UUID userId) {
        if (principal.id().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrators can only manage other accounts");
        }
        return users.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public record CreateUserRequest(@NotBlank @Size(max = 100) String fullName,
                                    @NotBlank @Email @Size(max = 255) String email,
                                    @NotBlank @Size(max = 72) String password) {}
    public record UpdateUserRequest(@NotBlank @Size(max = 100) String fullName,
                                    @NotBlank @Email @Size(max = 255) String email,
                                    @Size(max = 72) String newPassword) {}
    public record UpdateUserStatusRequest(
            @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status) {}
    public record AdminDashboard(long activeAccountsLast30Days, long newAccountsThisMonth,
                                 long monthlyBudgetsTracked, long budgetsAtRisk, long budgetsOverLimit,
                                 BigDecimal averageBudgetUtilization, List<MonthlyTotal> spendingTrend,
                                 List<CategoryTotal> categoryBreakdown, List<TopSpenderView> topSpenders,
                                 List<AdminExpenseView> recentActivity) {}
    public record MonthlyTotal(String month, BigDecimal amount) {}
    public record CategoryTotal(String category, BigDecimal amount) {}
    public record TopSpenderView(UUID userId, String fullName, String email, BigDecimal amount) {}
    public record AnalyticsSummary(List<MonthlyTotal> monthlyTrend, List<CategoryTotal> categoryBreakdown) {}
    public record UserView(UUID id, String fullName, String email, String role, String status, Instant createdAt) {
        public static UserView from(User user) {
            return new UserView(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                    user.getStatus(), user.getCreatedAt());
        }
    }
    public record AdminExpenseView(UUID id, String item, BigDecimal amount, String category,
                                   String paymentMethod, String paymentPlatform, String transactionId, Instant expenseDateTime,
                                   String notes, UUID userId, String userFullName, String userEmail) {
        static AdminExpenseView from(com.spendwise.expense.Expense expense) {
            return new AdminExpenseView(expense.getId(), expense.getItem(), expense.getAmount(),
                    expense.getCategory(), expense.getPaymentMethod(), expense.getPaymentPlatform(),
                    expense.getTransactionId(),
                    expense.getExpenseDateTime(), expense.getNotes(), expense.getUser().getId(),
                    expense.getUser().getFullName(), expense.getUser().getEmail());
        }

        static AdminExpenseView from(ExpenseView expense, User owner) {
            return new AdminExpenseView(expense.id(), expense.item(), expense.amount(), expense.category(),
                    expense.paymentMethod(), expense.paymentPlatform(), expense.transactionId(),
                    expense.expenseDateTime(),
                    expense.notes(), owner.getId(), owner.getFullName(), owner.getEmail());
        }
    }
}
