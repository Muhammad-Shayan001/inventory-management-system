package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an owner capital contribution or withdrawal.
 * Distinguishes equity financing from sales revenue and operating expenses.
 *
 * Types:
 * - CONTRIBUTION: Owner introduces capital (Cash / Bank)
 * - WITHDRAWAL: Owner draws funds for personal use
 */
public class CapitalTransaction {

    public enum Type {
        CONTRIBUTION("Capital Contribution"),
        WITHDRAWAL("Owner Withdrawal");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String transactionId;
    private Type type;
    private BigDecimal amount;
    private LocalDateTime dateTime;
    private String investorName;
    private String paymentMethod; // CASH, BANK, EASYPAISA, JAZZCASH
    private String destinationAccount; // MAIN_DRAWER, BANK, etc.
    private String description;
    private String createdBy;

    public CapitalTransaction() {
        this.type = Type.CONTRIBUTION;
        this.amount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.dateTime = LocalDateTime.now();
        this.investorName = "Owner";
        this.paymentMethod = "CASH";
        this.destinationAccount = "MAIN_DRAWER";
        this.description = "";
        this.createdBy = "admin";
    }

    public CapitalTransaction(String transactionId, Type type, BigDecimal amount,
                              LocalDateTime dateTime, String investorName,
                              String paymentMethod, String destinationAccount,
                              String description, String createdBy) {
        this.transactionId = transactionId;
        this.type = type != null ? type : Type.CONTRIBUTION;
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.dateTime = dateTime != null ? dateTime : LocalDateTime.now();
        this.investorName = investorName != null && !investorName.trim().isEmpty() ? investorName.trim() : "Owner";
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.destinationAccount = destinationAccount != null ? destinationAccount : "MAIN_DRAWER";
        this.description = description != null ? description : "";
        this.createdBy = createdBy != null ? createdBy : "admin";
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type != null ? type : Type.CONTRIBUTION;
    }

    public BigDecimal getAmount() {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getInvestorName() {
        return investorName;
    }

    public void setInvestorName(String investorName) {
        this.investorName = investorName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
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
