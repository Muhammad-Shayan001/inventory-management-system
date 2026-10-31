package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable ledger entry for cash and bank transactions.
 */
public class AccountTransaction {

    public enum Type {
        CAPITAL_IN("Capital Contribution"),
        OWNER_WITHDRAWAL("Owner Withdrawal"),
        SALES_COLLECTION("Sales Collection"),
        CUSTOMER_PAYMENT("Customer Credit Collection"),
        SUPPLIER_PAYMENT("Supplier Purchase Payment"),
        OPERATING_EXPENSE("Operating Expense"),
        REFUND_PAID("Customer Return Refund"),
        TRANSFER_IN("Account Transfer In"),
        TRANSFER_OUT("Account Transfer Out"),
        OPENING_BALANCE("Opening Balance Setup");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String transactionId;
    private String accountId;
    private LocalDateTime dateTime;
    private Type type;
    private BigDecimal debit;  // Money entering the account (+)
    private BigDecimal credit; // Money leaving the account (-)
    private BigDecimal runningBalance;
    private String referenceId;
    private String description;
    private String createdBy;

    public AccountTransaction() {
        this.dateTime = LocalDateTime.now();
        this.type = Type.SALES_COLLECTION;
        this.debit = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.credit = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.runningBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.createdBy = "admin";
    }

    public AccountTransaction(String transactionId, String accountId, LocalDateTime dateTime,
                              Type type, BigDecimal debit, BigDecimal credit,
                              BigDecimal runningBalance, String referenceId,
                              String description, String createdBy) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.dateTime = dateTime != null ? dateTime : LocalDateTime.now();
        this.type = type != null ? type : Type.SALES_COLLECTION;
        this.debit = debit != null ? debit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.credit = credit != null ? credit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.runningBalance = runningBalance != null ? runningBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.referenceId = referenceId != null ? referenceId : "";
        this.description = description != null ? description : "";
        this.createdBy = createdBy != null ? createdBy : "admin";
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
