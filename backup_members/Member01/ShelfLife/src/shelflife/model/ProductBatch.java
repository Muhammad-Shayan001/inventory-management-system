package shelflife.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a physical batch of a product with its own expiration date and stock quantity.
 * Owned and maintained by Member 1.
 */
public class ProductBatch {
    private String batchId;
    private String productId;
    private int quantity;
    private LocalDate expiryDate;

    public ProductBatch() {
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate) {
        setBatchId(batchId);
        setProductId(productId);
        setQuantity(quantity);
        setExpiryDate(expiryDate);
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

    /**
     * Checks if this batch is expired relative to today.
     * Per ShelfLife business rules: a batch expiring today is NOT considered expired.
     */
    public boolean isExpired() {
        return isExpired(LocalDate.now());
    }

    /**
     * Checks if this batch is expired relative to a specific reference date.
     */
    public boolean isExpired(LocalDate referenceDate) {
        if (expiryDate == null || referenceDate == null) {
            return false;
        }
        return expiryDate.isBefore(referenceDate);
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
        return "Batch[" + batchId + " for " + productId + ", Qty=" + quantity + ", Expires=" + expiryDate + "]";
    }
}
