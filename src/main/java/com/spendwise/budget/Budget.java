package com.spendwise.budget;

import com.spendwise.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "alert_percentage", nullable = false)
    private int alertPercentage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private java.time.Instant createdAt;

    protected Budget() {
    }

    public Budget(UUID id, User user, String name, String category, LocalDate budgetMonth, BigDecimal amount) {
        this(id, user, name, category, budgetMonth, amount, 80);
    }

    public Budget(UUID id, User user, String name, String category, LocalDate budgetMonth,
                  BigDecimal amount, int alertPercentage) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.category = category;
        this.budgetMonth = budgetMonth;
        this.amount = amount;
        this.alertPercentage = alertPercentage;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public LocalDate getBudgetMonth() { return budgetMonth; }
    public BigDecimal getAmount() { return amount; }
    public int getAlertPercentage() { return alertPercentage; }
    public void update(String name, String category, LocalDate budgetMonth, BigDecimal amount,
                       int alertPercentage) {
        this.name = name;
        this.category = category;
        this.budgetMonth = budgetMonth;
        this.amount = amount;
        this.alertPercentage = alertPercentage;
    }
}
