package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Operating expense model for rent, electricity, salaries, transport, etc.
 * Critical Accounting Rule: Operating expenses are strictly separated from inventory stock purchases.
 */
public class Expense {

    private String expenseId;
    private String category; // RENT, UTILITIES, SALARIES, TRANSPORT, PACKAGING, MAINTENANCE, MARKETING, OTHER
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private String paymentMethod; // CASH, BANK, EASYPAISA, JAZZCASH
    private String paymentAccount; // MAIN_DRAWER, BANK, etc.
    private String paidTo;
    private String referenceNumber;
    private String createdBy;
    private String notes;

    public Expense() {
        this.category = "OTHER";
        this.amount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.expenseDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.paymentMethod = "CASH";
        this.paymentAccount = "MAIN_DRAWER";
        this.createdBy = "admin";
    }

    public Expense(String expenseId, String category, String description, BigDecimal amount,
                   LocalDate expenseDate, LocalDateTime createdAt, String paymentMethod,
                   String paymentAccount, String paidTo, String referenceNumber,
                   String createdBy, String notes) {
        this.expenseId = expenseId;
        this.category = category != null && !category.trim().isEmpty() ? category.trim().toUpperCase() : "OTHER";
        this.description = description != null ? description.trim() : "";
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.expenseDate = expenseDate != null ? expenseDate : LocalDate.now();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.paymentAccount = paymentAccount != null ? paymentAccount : "MAIN_DRAWER";
        this.paidTo = paidTo != null ? paidTo.trim() : "";
        this.referenceNumber = referenceNumber != null ? referenceNumber.trim() : "";
        this.createdBy = createdBy != null ? createdBy.trim() : "admin";
        this.notes = notes != null ? notes.trim() : "";
    }

    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentAccount() {
        return paymentAccount;
    }

    public void setPaymentAccount(String paymentAccount) {
        this.paymentAccount = paymentAccount;
    }

    public String getPaidTo() {
        return paidTo;
    }

    public void setPaidTo(String paidTo) {
        this.paidTo = paidTo;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
