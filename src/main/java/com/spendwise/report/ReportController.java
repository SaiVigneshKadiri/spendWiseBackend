package com.spendwise.report;

import com.spendwise.expense.Expense;
import com.spendwise.expense.ExpenseRepository;
import com.spendwise.security.UserPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/reports")
public class ReportController {
    private final ExpenseRepository expenses;

    public ReportController(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    @GetMapping(value = "/expenses.csv", produces = "text/csv")
    ResponseEntity<byte[]> exportExpenses(@AuthenticationPrincipal UserPrincipal principal) {
        List<Expense> rows = expenses.findAllByUserIdOrderByExpenseDateTimeDesc(principal.id());
        StringBuilder csv = new StringBuilder("date,item,category,paymentMethod,transactionId,amount,notes\r\n");
        rows.forEach(row -> csv.append(csv(row.getExpenseDateTime().toString())).append(',')
                .append(csv(row.getItem())).append(',').append(csv(row.getCategory())).append(',')
                .append(csv(row.getPaymentMethod())).append(',').append(csv(row.getTransactionId())).append(',')
                .append(row.getAmount().toPlainString()).append(',').append(csv(row.getNotes())).append("\r\n"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"spendwise-expenses.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csv(String value) {
        if (value == null) return "";
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
