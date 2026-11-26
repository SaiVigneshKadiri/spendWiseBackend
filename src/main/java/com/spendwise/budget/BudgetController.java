package com.spendwise.budget;

import com.spendwise.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/api/v1/users/me/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    List<BudgetView> list(@AuthenticationPrincipal UserPrincipal principal,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        return budgetService.list(principal.id(), month == null ? LocalDate.now().withDayOfMonth(1) : month.withDayOfMonth(1));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BudgetView create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody BudgetRequest request) {
        return budgetService.create(principal.id(), request);
    }

    @PutMapping("/{id}")
    BudgetView update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                      @Valid @RequestBody BudgetRequest request) {
        return budgetService.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        budgetService.delete(principal.id(), id);
    }

    public record BudgetRequest(@NotBlank @Size(max = 100) String name, @Size(max = 50) String category,
                                @NotNull LocalDate budgetMonth,
                                @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                @Min(1) @Max(100) Integer alertPercentage) {}
    public record BudgetView(UUID id, String name, String category, LocalDate budgetMonth,
                             BigDecimal amount, BigDecimal spent, BigDecimal percentage,
                             int alertPercentage) {}
}
