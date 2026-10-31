package shelflife.model;

import java.time.LocalDateTime;

public class WastageRecord {
    private String wastageId;
    private String batchId;
    private int quantity;
    private LocalDateTime wastageDateTime;
    private String reason;

    public WastageRecord(String wastageId, String batchId, int quantity, LocalDateTime wastageDateTime, String reason) {
        this.wastageId = wastageId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.wastageDateTime = wastageDateTime;
        this.reason = reason;
    }

    public String getWastageId() { return wastageId; }
    public void setWastageId(String wastageId) { this.wastageId = wastageId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDateTime getWastageDateTime() { return wastageDateTime; }
    public void setWastageDateTime(LocalDateTime wastageDateTime) { this.wastageDateTime = wastageDateTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
