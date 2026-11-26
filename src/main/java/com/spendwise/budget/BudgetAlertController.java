package com.spendwise.budget;

import com.spendwise.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/budgets/alerts")
public class BudgetAlertController {
    private final BudgetAlertRepository alerts;

    public BudgetAlertController(BudgetAlertRepository alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    List<BudgetAlertView> list(@AuthenticationPrincipal UserPrincipal principal) {
        return alerts.findTop50ByUserIdOrderByCreatedAtDesc(principal.id())
                .stream()
                .map(BudgetAlertView::from)
                .toList();
    }

    @PutMapping("/{id}/read")
    BudgetAlertView markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        BudgetAlert alert = alerts.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found"));
        alert.markRead(Instant.now());
        return BudgetAlertView.from(alerts.save(alert));
    }

    public record BudgetAlertView(UUID id, UUID budgetId, String budgetName, int thresholdPercentage,
                                  BigDecimal spentAmount, Instant createdAt, Instant readAt) {
        static BudgetAlertView from(BudgetAlert alert) {
            return new BudgetAlertView(alert.getId(), alert.getBudget().getId(),
                    alert.getBudget().getName(), alert.getThresholdPercentage(),
                    alert.getSpentAmount(), alert.getCreatedAt(), alert.getReadAt());
        }
    }
}
