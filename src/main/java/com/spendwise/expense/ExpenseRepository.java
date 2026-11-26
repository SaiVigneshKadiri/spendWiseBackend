package com.spendwise.expense;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findTop10ByUserIdOrderByExpenseDateTimeDesc(UUID userId);
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

    @Query("""
        select coalesce(sum(e.amount), 0) from Expense e
        where e.user.id = :userId and e.expenseDateTime >= :from and e.expenseDateTime < :to
          and (:paymentMethod is null or e.paymentMethod = :paymentMethod)
          and (:paymentPlatform is null or e.paymentPlatform = :paymentPlatform)
        """)
    BigDecimal sumByUserAndDateRangeFiltered(@Param("userId") UUID userId,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to,
                                             @Param("paymentMethod") String paymentMethod,
                                             @Param("paymentPlatform") String paymentPlatform);

    @Query("""
        select coalesce(sum(e.amount), 0) from Expense e
        where e.user.id = :userId and e.category = :category
          and e.expenseDateTime >= :from and e.expenseDateTime < :to
        """)
    BigDecimal sumByUserAndCategoryAndDateRange(@Param("userId") UUID userId,
                                                @Param("category") String category,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.expenseDateTime >= :from and e.expenseDateTime < :to")
    java.math.BigDecimal sumAllByDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(e) from Expense e where e.expenseDateTime >= :from and e.expenseDateTime < :to")
    long countByDateRange(@Param("from") Instant from, @Param("to") Instant to);

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

    List<Expense> findTop500ByOrderByExpenseDateTimeDesc();

    interface CategoryTotal {
        String getCategory();
        BigDecimal getAmount();
    }

    interface TopSpender {
        UUID getUserId();
        String getFullName();
        String getEmail();
        BigDecimal getAmount();
    }
}
