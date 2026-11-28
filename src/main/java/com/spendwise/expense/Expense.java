package com.spendwise.expense;

import com.spendwise.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String item;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(name = "payment_platform", length = 50)
    private String paymentPlatform;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "expense_datetime", nullable = false)
    private Instant expenseDateTime;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected Expense() {
    }

    public Expense(UUID id, User user, String item, BigDecimal amount, String category,
                   String paymentMethod, String paymentPlatform, String transactionId,
                   Instant expenseDateTime, String notes) {
        this.id = id;
        this.user = user;
        this.item = item;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.paymentPlatform = paymentPlatform;
        this.transactionId = transactionId;
        this.expenseDateTime = expenseDateTime;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getItem() { return item; }
    public BigDecimal getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentPlatform() { return paymentPlatform; }
    public String getTransactionId() { return transactionId; }
    public Instant getExpenseDateTime() { return expenseDateTime; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String item, BigDecimal amount, String category, String paymentMethod,
                       String paymentPlatform, String transactionId, Instant expenseDateTime, String notes) {
        this.updatedAt = Instant.now();
        this.item = item;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.paymentPlatform = paymentPlatform;
        this.transactionId = transactionId;
        this.expenseDateTime = expenseDateTime;
        this.notes = notes;
    }
}
