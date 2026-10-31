package shelflife.service;

import shelflife.model.Expense;
import shelflife.storage.DataStore;
import shelflife.util.CurrencyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Standard implementation of ExpenseService.
 */
public class ExpenseServiceImpl implements ExpenseService {

    private final DataStore dataStore;
    private final List<Expense> expenses = new ArrayList<>();

    public ExpenseServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        expenses.clear();
        try {
            List<Expense> loaded = dataStore.loadExpenses();
            if (loaded != null) {
                expenses.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load expenses: " + e.getMessage());
        }
    }

    @Override
    public synchronized Expense recordExpense(String category, String description, BigDecimal amount,
                                              LocalDate expenseDate, String paymentMethod, String paymentAccount,
                                              String paidTo, String referenceNumber, String createdBy, String notes) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }
        if (category == null || category.trim().isEmpty()) {
            category = "OTHER";
        }

        String id = "EXP-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 900 + 100);
        Expense exp = new Expense(
                id,
                category,
                description != null ? description : "",
                amount,
                expenseDate != null ? expenseDate : LocalDate.now(),
                LocalDateTime.now(),
                paymentMethod != null ? paymentMethod : "CASH",
                paymentAccount != null ? paymentAccount : "MAIN_DRAWER",
                paidTo != null ? paidTo : "",
                referenceNumber != null ? referenceNumber : "",
                createdBy != null ? createdBy : "admin",
                notes != null ? notes : ""
        );

        dataStore.executeTransaction(() -> {
            List<Expense> list = dataStore.loadExpenses();
            list.add(exp);
            dataStore.saveExpenses(list);
        });

        reload();
        return exp;
    }

    @Override
    public synchronized List<Expense> getAllExpenses() {
        List<Expense> copy = new ArrayList<>(expenses);
        copy.sort((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()));
        return Collections.unmodifiableList(copy);
    }

    @Override
    public synchronized List<Expense> getExpensesBetween(LocalDate start, LocalDate end) {
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            LocalDate d = e.getExpenseDate();
            if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                filtered.add(e);
            }
        }
        filtered.sort((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()));
        return filtered;
    }

    @Override
    public synchronized BigDecimal getTotalExpenses() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Expense e : expenses) {
            sum = sum.add(e.getAmount());
        }
        return CurrencyUtil.roundMoney(sum);
    }

    @Override
    public synchronized BigDecimal getTotalExpensesBetween(LocalDate start, LocalDate end) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Expense e : getExpensesBetween(start, end)) {
            sum = sum.add(e.getAmount());
        }
        return CurrencyUtil.roundMoney(sum);
    }

    @Override
    public synchronized Map<String, BigDecimal> getExpensesByCategory(LocalDate start, LocalDate end) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Expense e : getExpensesBetween(start, end)) {
            String cat = e.getCategory();
            map.put(cat, map.getOrDefault(cat, BigDecimal.ZERO).add(e.getAmount()));
        }
        return map;
    }
}
