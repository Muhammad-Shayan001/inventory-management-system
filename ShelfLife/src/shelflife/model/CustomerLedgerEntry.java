package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only customer credit transaction record for Khata auditability.
 */
public class CustomerLedgerEntry {
    private String entryId;
    private String customerId;
    private LocalDateTime timestamp;
    private String transactionType; // SALE_ON_CREDIT, PAYMENT_RECEIVED, ADJUSTMENT
    private String referenceId;    // Sale ID or Payment Receipt ID
    private BigDecimal debit;      // Increases customer debt (e.g. sale)
    private BigDecimal credit;     // Decreases customer debt (e.g. payment)
    private BigDecimal runningBalance;
    private String notes;
    private String cashierUsername;

    public CustomerLedgerEntry() {
        this.timestamp = LocalDateTime.now();
        this.debit = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.credit = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.runningBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public CustomerLedgerEntry(String entryId, String customerId, LocalDateTime timestamp,
                               String transactionType, String referenceId, BigDecimal debit,
                               BigDecimal credit, BigDecimal runningBalance, String notes,
                               String cashierUsername) {
        this.entryId = entryId;
        this.customerId = customerId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.transactionType = transactionType;
        this.referenceId = referenceId != null ? referenceId : "";
        this.debit = debit != null ? debit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.credit = credit != null ? credit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.runningBalance = runningBalance != null ? runningBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.notes = notes != null ? notes : "";
        this.cashierUsername = cashierUsername != null ? cashierUsername : "system";
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit != null ? debit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit != null ? credit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance != null ? runningBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }
}
