package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Physical inventory batch of a product with its own expiration date and stock quantity.
 * Supports FEFO ordering (earliest expiry first, batch ID as deterministic tie-breaker).
 */
public class ProductBatch implements Comparable<ProductBatch> {
    private String batchId;
    private String productId;
    private int quantity;
    private LocalDate expiryDate;

    private BigDecimal costPrice;
    private String supplierId;
    private LocalDate receivedDate;
    private boolean recalled = false;

    public ProductBatch() {
        this.costPrice = java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        this.supplierId = "SUP-DEFAULT";
        this.receivedDate = LocalDate.now();
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate) {
        this(batchId, productId, quantity, expiryDate, java.math.BigDecimal.ZERO, "SUP-DEFAULT", LocalDate.now(), false);
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate, java.math.BigDecimal costPrice, String supplierId) {
        this(batchId, productId, quantity, expiryDate, costPrice, supplierId, LocalDate.now(), false);
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate, java.math.BigDecimal costPrice, String supplierId, LocalDate receivedDate) {
        this(batchId, productId, quantity, expiryDate, costPrice, supplierId, receivedDate, false);
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate, java.math.BigDecimal costPrice, String supplierId, LocalDate receivedDate, boolean recalled) {
        setBatchId(batchId);
        setProductId(productId);
        setQuantity(quantity);
        setExpiryDate(expiryDate);
        this.costPrice = costPrice != null ? costPrice.setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        this.supplierId = supplierId != null ? supplierId.trim().toUpperCase() : "SUP-DEFAULT";
        this.receivedDate = receivedDate != null ? receivedDate : LocalDate.now();
        this.recalled = recalled;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty.");
        }
        this.productId = productId.trim().toUpperCase();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Batch quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null.");
        }
        this.expiryDate = expiryDate;
    }

    public java.math.BigDecimal getCostPrice() {
        return costPrice != null ? costPrice : java.math.BigDecimal.ZERO;
    }

    public void setCostPrice(java.math.BigDecimal costPrice) {
        this.costPrice = costPrice != null ? costPrice.setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO;
    }

    public String getSupplierId() {
        return supplierId != null ? supplierId : "SUP-DEFAULT";
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId != null ? supplierId.trim().toUpperCase() : "SUP-DEFAULT";
    }

    public LocalDate getReceivedDate() {
        return receivedDate != null ? receivedDate : LocalDate.now();
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate != null ? receivedDate : LocalDate.now();
    }

    /**
     * Checks if this batch is expired relative to today.
     * Note: Per ShelfLife business rules, a batch expiring today is NOT expired.
     */
    public boolean isExpired() {
        return isExpired(LocalDate.now());
    }

    /**
     * Checks if this batch is expired relative to a reference date.
     */
    public boolean isExpired(LocalDate referenceDate) {
        if (expiryDate == null || referenceDate == null) {
            return false;
        }
        return expiryDate.isBefore(referenceDate);
    }

    /**
     * Checks if this batch is near its expiry date within the given number of days.
     */
    public boolean isNearExpiry(int daysThreshold) {
        if (isExpired() || quantity <= 0) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(daysThreshold);
        return !expiryDate.isBefore(today) && !expiryDate.isAfter(cutoff);
    }

    public boolean isRecalled() {
        return recalled;
    }

    public void setRecalled(boolean recalled) {
        this.recalled = recalled;
    }

    /**
     * Checks if this batch is eligible for sale under FEFO rules.
     * Batches that are expired, have zero quantity, or are recalled cannot be sold.
     */
    public boolean isSellable() {
        return quantity > 0 && !isExpired() && !recalled;
    }

    /**
     * Safely deducts a specified amount from this batch's stock quantity.
     */
    public void deductQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction quantity cannot be negative.");
        }
        if (amount > this.quantity) {
            throw new IllegalArgumentException("Insufficient batch quantity in " + batchId + 
                    ". Available: " + this.quantity + ", Requested: " + amount);
        }
        this.quantity -= amount;
    }

    /**
     * FEFO ordering: ascending by expiry date, batch ID as deterministic tie-breaker.
     */
    @Override
    public int compareTo(ProductBatch other) {
        if (other == null) return 1;
        int dateCmp = this.expiryDate.compareTo(other.expiryDate);
        if (dateCmp != 0) {
            return dateCmp;
        }
        return this.batchId.compareTo(other.batchId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductBatch that = (ProductBatch) o;
        return Objects.equals(batchId, that.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId);
    }

    @Override
    public String toString() {
        return "Batch[" + batchId + " | Product: " + productId + " | Qty: " + quantity + " | Exp: " + expiryDate + "]";
    }
}
