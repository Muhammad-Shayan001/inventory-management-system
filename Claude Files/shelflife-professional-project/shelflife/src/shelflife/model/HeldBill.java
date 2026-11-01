package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a suspended/held billing session in the POS.
 * Allows cashiers to park an active customer cart and resume it later.
 */
public class HeldBill {
    private String heldBillId;
    private LocalDateTime createdAt;
    private String cashierUsername;
    private String customerName;
    private String note;
    private int itemCount;
    private BigDecimal estimatedTotal;
    private List<HeldBillItem> items = new ArrayList<>();

    public HeldBill() {
        this.createdAt = LocalDateTime.now();
        this.estimatedTotal = BigDecimal.ZERO;
    }

    public HeldBill(String heldBillId, LocalDateTime createdAt, String cashierUsername,
                    String customerName, String note, int itemCount, BigDecimal estimatedTotal) {
        this.heldBillId = heldBillId;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.cashierUsername = cashierUsername;
        this.customerName = customerName != null ? customerName : "Walk-in Customer";
        this.note = note != null ? note : "";
        this.itemCount = itemCount;
        this.estimatedTotal = estimatedTotal != null ? estimatedTotal : BigDecimal.ZERO;
    }

    public String getHeldBillId() {
        return heldBillId;
    }

    public void setHeldBillId(String heldBillId) {
        this.heldBillId = heldBillId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public List<HeldBillItem> getItems() {
        return items;
    }

    public void setItems(List<HeldBillItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
