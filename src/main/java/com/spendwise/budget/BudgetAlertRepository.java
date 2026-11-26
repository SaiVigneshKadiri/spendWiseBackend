package com.spendwise.budget;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, UUID> {
    boolean existsByBudgetIdAndThresholdPercentage(UUID budgetId, int thresholdPercentage);
    @EntityGraph(attributePaths = "budget")
    List<BudgetAlert> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    @EntityGraph(attributePaths = "budget")
    Optional<BudgetAlert> findByIdAndUserId(UUID id, UUID userId);
}
