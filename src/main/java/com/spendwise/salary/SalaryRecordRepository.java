package com.spendwise.salary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, UUID> {
    List<SalaryRecord> findAllByUserIdOrderByEffectiveFromDesc(UUID userId);
    Optional<SalaryRecord> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserId(UUID userId);
    Optional<SalaryRecord> findFirstByUserIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID userId, LocalDate date);
}
