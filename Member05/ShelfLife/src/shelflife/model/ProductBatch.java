package shelflife.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a product batch/lot with expiry date.
 * Each batch references an existing product via productId.
 * Quantity never becomes negative. Uses FEFO (First-Expired, First-Out) ordering.
 * A batch expiring today is NOT considered expired per project rule.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class ProductBatch {
    private String batchId;
    private String productId;
    private int quantity;
    private LocalDate expiryDate;

    public ProductBatch() {
        this.quantity = 0;
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate) {
        setBatchId(batchId);
        setProductId(productId);
        setQuantity(quantity);
        this.expiryDate = expiryDate;
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
        this.expiryDate = expiryDate;
    }

    /**
     * Returns true if the batch has expired.
     * A batch expiring today is NOT considered expired per project rule.
     */
    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Deducts the specified quantity from this batch.
     * Throws exception if insufficient quantity.
     */
    public void deductQuantity(int amount) throws Exception {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduct amount cannot be negative.");
        }
        if (amount > quantity) {
            throw new Exception("Insufficient batch quantity: requested " + amount + ", available " + quantity);
        }
        this.quantity -= amount;
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
        return "Batch[" + batchId + "] Prod:" + productId + " Qty:" + quantity + " Exp:" + expiryDate;
    }
}