package com.spendwise.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findAllByUserIdAndBudgetMonthOrderByNameAsc(UUID userId, LocalDate budgetMonth);
    List<Budget> findAllByBudgetMonthOrderByNameAsc(LocalDate budgetMonth);
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);
    long count();

    @Query("""
        select b.id as budgetId, b.amount as budgetAmount, coalesce(sum(e.amount), 0) as spentAmount
        from Budget b
        left join Expense e on e.user.id = b.user.id
          and e.expenseDateTime >= :from and e.expenseDateTime < :to
          and (b.category is null or e.category = b.category)
        where b.budgetMonth = :month
        group by b.id, b.amount
        order by b.id
        """)
    List<BudgetSpending> findMonthlySpending(@Param("month") LocalDate month,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to);

    @Query("""
        select b.id as budgetId, b.amount as budgetAmount, coalesce(sum(e.amount), 0) as spentAmount
        from Budget b
        left join Expense e on e.user.id = b.user.id
          and e.expenseDateTime >= :from and e.expenseDateTime < :to
          and (b.category is null or e.category = b.category)
        where b.user.id = :userId and b.budgetMonth = :month
        group by b.id, b.amount
        order by b.id
        """)
    List<BudgetSpending> findMonthlySpendingByUserId(@Param("userId") UUID userId,
                                                     @Param("month") LocalDate month,
                                                     @Param("from") Instant from,
                                                     @Param("to") Instant to);

    interface BudgetSpending {
        UUID getBudgetId();
        BigDecimal getBudgetAmount();
        BigDecimal getSpentAmount();
    }
}
