package shelflife.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Record representing inventory removed as unsaleable due to expiration, damage, or contamination.
 */
public class WastageRecord {
    private String wastageId;
    private String batchId;
    private int quantity;
    private LocalDateTime wastageDateTime;
    private String reason;

    public WastageRecord() {
        this.wastageDateTime = LocalDateTime.now();
    }

    public WastageRecord(String wastageId, String batchId, int quantity, LocalDateTime wastageDateTime, String reason) {
        setWastageId(wastageId);
        setBatchId(batchId);
        setQuantity(quantity);
        this.wastageDateTime = wastageDateTime != null ? wastageDateTime : LocalDateTime.now();
        setReason(reason);
    }

    public String getWastageId() {
        return wastageId;
    }

    public void setWastageId(String wastageId) {
        if (wastageId == null || wastageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Wastage ID cannot be null or empty.");
        }
        this.wastageId = wastageId.trim().toUpperCase();
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null or empty.");
        }
        this.batchId = batchId.trim().toUpperCase();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Wastage quantity must be greater than zero.");
        }
        this.quantity = quantity;
    }

    public LocalDateTime getWastageDateTime() {
        return wastageDateTime;
    }

    public void setWastageDateTime(LocalDateTime wastageDateTime) {
        this.wastageDateTime = wastageDateTime != null ? wastageDateTime : LocalDateTime.now();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Wastage reason cannot be null or empty.");
        }
        this.reason = reason.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WastageRecord that = (WastageRecord) o;
        return Objects.equals(wastageId, that.wastageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wastageId);
    }

    @Override
    public String toString() {
        return "WastageRecord[" + wastageId + " | Batch: " + batchId + " | Qty: " + quantity + 
                " | Date: " + wastageDateTime + " | Reason: " + reason + "]";
    }
}
