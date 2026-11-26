package com.spendwise.expense;

import com.spendwise.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users/me/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    List<ExpenseView> list(@AuthenticationPrincipal UserPrincipal principal,
                           @RequestParam(required = false) Instant from,
                           @RequestParam(required = false) Instant to,
                           @RequestParam(required = false) String category) {
        requireRegularUser(principal);
        return expenseService.list(principal.id(), from, to, category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseView create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ExpenseRequest request) {
        requireRegularUser(principal);
        return expenseService.create(principal.id(), request);
    }

    @PutMapping("/{id}")
    ExpenseView update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                       @Valid @RequestBody ExpenseRequest request) {
        requireRegularUser(principal);
        return expenseService.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        requireRegularUser(principal);
        expenseService.delete(principal.id(), id);
    }

    private void requireRegularUser(UserPrincipal principal) {
        if ("ADMIN".equals(principal.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrators can only manage other accounts");
        }
    }

    public record ExpenseRequest(@NotBlank @Size(max = 200) String item,
                                 @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                 @NotBlank @Size(max = 50) String category,
                                 @NotBlank @Size(max = 30) String paymentMethod,
                                 @Size(max = 50) String paymentPlatform,
                                 @Size(max = 100) String transactionId,
                                 @NotNull Instant expenseDateTime,
                                 @Size(max = 5000) String notes) {}

    public record ExpenseView(UUID id, String item, BigDecimal amount, String category, String paymentMethod,
                              String paymentPlatform, String transactionId, Instant expenseDateTime, String notes) {
        public static ExpenseView from(Expense expense) {
            return new ExpenseView(expense.getId(), expense.getItem(), expense.getAmount(), expense.getCategory(),
                    expense.getPaymentMethod(), expense.getPaymentPlatform(), expense.getTransactionId(),
                    expense.getExpenseDateTime(),
                    expense.getNotes());
        }
    }
}
