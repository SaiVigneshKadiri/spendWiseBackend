package com.spendwise.salary;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_records")
public class SalaryRecord {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;
    @Column(name = "take_home_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal takeHomeSalary;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected SalaryRecord() {
    }

    public SalaryRecord(UUID id, User user, BigDecimal grossSalary, BigDecimal takeHomeSalary, LocalDate effectiveFrom) {
        this.id = id;
        this.user = user;
        this.grossSalary = grossSalary;
        this.takeHomeSalary = takeHomeSalary;
        this.effectiveFrom = effectiveFrom;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public BigDecimal getTakeHomeSalary() { return takeHomeSalary; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(BigDecimal grossSalary, BigDecimal takeHomeSalary, LocalDate effectiveFrom) {
        this.grossSalary = grossSalary;
        this.takeHomeSalary = takeHomeSalary;
        this.effectiveFrom = effectiveFrom;
    }
}
