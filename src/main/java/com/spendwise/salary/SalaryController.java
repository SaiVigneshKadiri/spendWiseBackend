package com.spendwise.salary;

import com.spendwise.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users/me/salaries")
public class SalaryController {
    private final SalaryRecordRepository salaries;
    private final com.spendwise.user.UserRepository users;

    public SalaryController(SalaryRecordRepository salaries, com.spendwise.user.UserRepository users) {
        this.salaries = salaries;
        this.users = users;
    }

    @GetMapping
    List<SalaryView> list(@AuthenticationPrincipal UserPrincipal principal) {
        requireRegularUser(principal);
        return salaries.findAllByUserIdOrderByEffectiveFromDesc(principal.id()).stream()
                .map(SalaryView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    SalaryView create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody SalaryRequest request) {
        requireRegularUser(principal);
        if (salaries.existsByUserId(principal.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A salary record already exists; edit the existing record");
        }
        SalaryRecord record = salaries.save(new SalaryRecord(UUID.randomUUID(), users.getReferenceById(principal.id()),
                request.grossSalary(), request.takeHomeSalary(), request.effectiveFrom()));
        return SalaryView.from(record);
    }

    @PutMapping("/{id}")
    @Transactional
    SalaryView update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id,
                      @Valid @RequestBody SalaryRequest request) {
        requireRegularUser(principal);
        SalaryRecord record = salaries.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Salary record not found"));
        record.update(request.grossSalary(), request.takeHomeSalary(), request.effectiveFrom());
        return SalaryView.from(salaries.save(record));
    }

    private void requireRegularUser(UserPrincipal principal) {
        if ("ADMIN".equals(principal.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrators cannot manage personal salary records");
        }
    }

    public record SalaryRequest(@NotNull @DecimalMin("0.00") BigDecimal grossSalary,
                                @NotNull @DecimalMin("0.00") BigDecimal takeHomeSalary,
                                @NotNull LocalDate effectiveFrom) {}
    public record SalaryView(UUID id, BigDecimal grossSalary, BigDecimal takeHomeSalary, LocalDate effectiveFrom) {
        public static SalaryView from(SalaryRecord record) {
            return new SalaryView(record.getId(), record.getGrossSalary(), record.getTakeHomeSalary(),
                    record.getEffectiveFrom());
        }
    }
}
