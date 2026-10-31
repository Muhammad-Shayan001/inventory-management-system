package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a sale refund and product return transaction.
 */
public class SaleReturn {
    private String returnId;
    private String originalSaleId;
    private LocalDateTime returnDate;
    private BigDecimal totalRefund;
    private String processedBy;
    private String reason;
    private final List<SaleReturnItem> items = new ArrayList<>();

    public SaleReturn() {
        this.returnDate = LocalDateTime.now();
        this.totalRefund = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public SaleReturn(String returnId, String originalSaleId, LocalDateTime returnDate, BigDecimal totalRefund, String processedBy, String reason) {
        setReturnId(returnId);
        setOriginalSaleId(originalSaleId);
        this.returnDate = returnDate != null ? returnDate : LocalDateTime.now();
        this.totalRefund = totalRefund != null ? totalRefund.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.processedBy = processedBy != null ? processedBy.trim() : "ADMIN";
        this.reason = reason != null ? reason.trim() : "Customer return";
    }

    public String getReturnId() {
        return returnId;
    }

    public void setReturnId(String returnId) {
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new IllegalArgumentException("Return ID cannot be null or empty.");
        }
        this.returnId = returnId.trim().toUpperCase();
    }

    public String getOriginalSaleId() {
        return originalSaleId;
    }

    public void setOriginalSaleId(String originalSaleId) {
        if (originalSaleId == null || originalSaleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Original Sale ID cannot be null or empty.");
        }
        this.originalSaleId = originalSaleId.trim().toUpperCase();
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getTotalRefund() {
        return totalRefund;
    }

    public void setTotalRefund(BigDecimal totalRefund) {
        this.totalRefund = totalRefund != null ? totalRefund.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<SaleReturnItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(SaleReturnItem item) {
        if (item != null) {
            this.items.add(item);
        }
    }

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        for (SaleReturnItem it : items) {
            if (it.getLineRefund() != null) {
                sum = sum.add(it.getLineRefund());
            }
        }
        this.totalRefund = sum.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleReturn that = (SaleReturn) o;
        return Objects.equals(returnId, that.returnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnId);
    }

    @Override
    public String toString() {
        return "Return[" + returnId + " | Sale: " + originalSaleId + " | Refund: Rs. " + totalRefund + "]";
    }
}
