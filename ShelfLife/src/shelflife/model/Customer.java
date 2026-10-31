package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer profile for credit account (Khata / Udhaar) management.
 */
public class Customer {
    private String customerId;
    private String fullName;
    private String phoneNumber;
    private String address;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private boolean active;
    private LocalDate createdAt;

    public Customer() {
        this.creditLimit = BigDecimal.valueOf(5000).setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currentBalance = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.active = true;
        this.createdAt = LocalDate.now();
    }

    public Customer(String customerId, String fullName, String phoneNumber, String address,
                    BigDecimal creditLimit, BigDecimal currentBalance, boolean active, LocalDate createdAt) {
        setCustomerId(customerId);
        setFullName(fullName);
        this.phoneNumber = phoneNumber != null ? phoneNumber.trim() : "";
        this.address = address != null ? address.trim() : "";
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : LocalDate.now();
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty.");
        }
        this.customerId = customerId.trim().toUpperCase();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber != null ? phoneNumber.trim() : "";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address != null ? address.trim() : "";
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance != null ? currentBalance.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getAvailableCredit() {
        BigDecimal avail = creditLimit.subtract(currentBalance);
        return avail.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : avail;
    }

    @Override
    public String toString() {
        return fullName + " (" + customerId + " - Bal: Rs. " + currentBalance + ")";
    }
}
