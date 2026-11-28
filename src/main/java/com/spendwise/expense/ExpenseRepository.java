package com.spendwise.expense;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findTop10ByUserIdOrderByExpenseDateTimeDesc(UUID userId);
    @EntityGraph(attributePaths = "user")
    List<Expense> findTop10ByOrderByExpenseDateTimeDesc();
    List<Expense> findTop500ByUserIdOrderByExpenseDateTimeDesc(UUID userId);
    List<Expense> findAllByUserIdOrderByExpenseDateTimeDesc(UUID userId);
    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        select e from Expense e
        where e.user.id = :userId
          and e.expenseDateTime >= :from
          and e.expenseDateTime < :to
          and (:category is null or e.category = :category)
        order by e.expenseDateTime desc
        """)
    List<Expense> findFiltered(@Param("userId") UUID userId, @Param("from") Instant from,
                               @Param("to") Instant to, @Param("category") String category);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.user.id = :userId and e.expenseDateTime >= :from and e.expenseDateTime < :to")
    java.math.BigDecimal sumByUserAndDateRange(@Param("userId") UUID userId,
                                               @Param("from") Instant from, @Param("to") Instant to);

    /** One round trip for a user's monthly totals (replaces one query per month). Months are "yyyy-MM" in UTC. */
    @Query(value = """
        select to_char(e.expense_datetime at time zone 'UTC', 'YYYY-MM') as month,
               coalesce(sum(e.amount), 0) as amount
        from expenses e
        where e.user_id = :userId
          and e.expense_datetime >= :from and e.expense_datetime < :to
          and (cast(:paymentMethod as text) is null or e.payment_method = :paymentMethod)
          and (cast(:paymentPlatform as text) is null or e.payment_platform = :paymentPlatform)
        group by 1
        """, nativeQuery = true)
    List<MonthTotal> sumMonthlyByUserFiltered(@Param("userId") UUID userId,
                                              @Param("from") Instant from,
                                              @Param("to") Instant to,
                                              @Param("paymentMethod") String paymentMethod,
                                              @Param("paymentPlatform") String paymentPlatform);

    /** Platform-wide monthly totals in one round trip. Months are "yyyy-MM" in UTC. */
    @Query(value = """
        select to_char(e.expense_datetime at time zone 'UTC', 'YYYY-MM') as month,
               coalesce(sum(e.amount), 0) as amount
        from expenses e
        where e.expense_datetime >= :from and e.expense_datetime < :to
        group by 1
        """, nativeQuery = true)
    List<MonthTotal> sumMonthlyAll(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select coalesce(sum(e.amount), 0) from Expense e
        where e.user.id = :userId and e.category = :category
          and e.expenseDateTime >= :from and e.expenseDateTime < :to
        """)
    BigDecimal sumByUserAndCategoryAndDateRange(@Param("userId") UUID userId,
                                                @Param("category") String category,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query("select count(distinct e.user.id) from Expense e where e.expenseDateTime >= :from")
    long countUsersWithExpensesSince(@Param("from") Instant from);

    @Query("""
        select e.user.id as userId, e.user.fullName as fullName, e.user.email as email, sum(e.amount) as amount
        from Expense e
        where e.expenseDateTime >= :from and e.expenseDateTime < :to
        group by e.user.id, e.user.fullName, e.user.email
        order by sum(e.amount) desc
        """)
    List<TopSpender> findTopSpenders(@Param("from") Instant from, @Param("to") Instant to,
                                     Pageable pageable);

    @Query("""
        select e.category as category, sum(e.amount) as amount
        from Expense e
        where e.expenseDateTime >= :from and e.expenseDateTime < :to
        group by e.category
        order by e.category
        """)
    List<CategoryTotal> sumAllByCategoryAndDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select e.category as category, sum(e.amount) as amount
        from Expense e
        where e.user.id = :userId
          and e.expenseDateTime >= :from and e.expenseDateTime < :to
        group by e.category
        order by e.category
        """)
    List<CategoryTotal> sumByUserAndCategoryAndDateRange(@Param("userId") UUID userId,
                                                         @Param("from") Instant from,
                                                         @Param("to") Instant to);

    @Query("""
        select e.category as category, sum(e.amount) as amount
        from Expense e
        where e.user.id = :userId and e.expenseDateTime >= :from and e.expenseDateTime < :to
          and (:paymentMethod is null or e.paymentMethod = :paymentMethod)
          and (:paymentPlatform is null or e.paymentPlatform = :paymentPlatform)
        group by e.category
        order by e.category
        """)
    List<CategoryTotal> sumByUserAndCategoryAndDateRangeFiltered(
            @Param("userId") UUID userId, @Param("from") Instant from, @Param("to") Instant to,
            @Param("paymentMethod") String paymentMethod, @Param("paymentPlatform") String paymentPlatform);


    interface CategoryTotal {
        String getCategory();
        BigDecimal getAmount();
    }

    interface MonthTotal {
        String getMonth();
        BigDecimal getAmount();
    }

    interface TopSpender {
        UUID getUserId();
        String getFullName();
        String getEmail();
        BigDecimal getAmount();
    }
}
