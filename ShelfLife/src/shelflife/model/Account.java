package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bank or Cash account (Main Drawer, Reserve, Bank, Easypaisa, JazzCash).
 */
public class Account {

    public enum Type {
        CASH("Cash Drawer"),
        BANK("Bank Account"),
        MOBILE_WALLET("Mobile Wallet");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String accountId;
    private String name;
    private Type type;
    private BigDecimal openingBalance;
    private BigDecimal currentBalance;
    private boolean active;
    private LocalDateTime updatedAt;

    public Account() {
        this.type = Type.CASH;
        this.openingBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currentBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public Account(String accountId, String name, Type type, BigDecimal openingBalance,
                   BigDecimal currentBalance, boolean active) {
        this.accountId = accountId;
        this.name = name;
        this.type = type != null ? type : Type.CASH;
        this.openingBalance = openingBalance != null ? openingBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : this.openingBalance;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance != null ? openingBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return name + " (" + type.getDisplayName() + ") - Bal: Rs. " + currentBalance;
    }
}
