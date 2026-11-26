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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget_alerts")
public class BudgetAlert {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "threshold_percentage", nullable = false)
    private int thresholdPercentage;

    @Column(name = "spent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal spentAmount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected BudgetAlert() {
    }

    public BudgetAlert(UUID id, User user, Budget budget, int thresholdPercentage, BigDecimal spentAmount) {
        this.id = id;
        this.user = user;
        this.budget = budget;
        this.thresholdPercentage = thresholdPercentage;
        this.spentAmount = spentAmount;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Budget getBudget() { return budget; }
    public int getThresholdPercentage() { return thresholdPercentage; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void markRead(Instant time) {
        if (readAt == null) readAt = time;
    }
}
