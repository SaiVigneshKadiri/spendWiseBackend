package com.spendwise.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, UUID> {
    @EntityGraph(attributePaths = "budget")
    List<BudgetAlert> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    @EntityGraph(attributePaths = "budget")
    Optional<BudgetAlert> findByIdAndUserId(UUID id, UUID userId);

    /** Every alert already raised for a user's budgets in one month, used to avoid duplicates in one query. */
    @Query("""
        select a.budget.id as budgetId, a.thresholdPercentage as thresholdPercentage
        from BudgetAlert a
        where a.budget.user.id = :userId and a.budget.budgetMonth = :month
        """)
    List<AlertKey> findAlertKeysByUserIdAndMonth(@Param("userId") UUID userId,
                                                 @Param("month") LocalDate month);

    interface AlertKey {
        UUID getBudgetId();
        int getThresholdPercentage();
    }
}
