package shelflife.service;

import shelflife.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service contract for Operating Expense management.
 * Tracks shop overheads (rent, salaries, electricity, etc.) without treating inventory purchases as expenses.
 */
public interface ExpenseService {

    Expense recordExpense(String category, String description, BigDecimal amount,
                          LocalDate expenseDate, String paymentMethod, String paymentAccount,
                          String paidTo, String referenceNumber, String createdBy, String notes) throws Exception;

    List<Expense> getAllExpenses();

    List<Expense> getExpensesBetween(LocalDate start, LocalDate end);

    BigDecimal getTotalExpenses();

    BigDecimal getTotalExpensesBetween(LocalDate start, LocalDate end);

    Map<String, BigDecimal> getExpensesByCategory(LocalDate start, LocalDate end);

    void reload();
}
