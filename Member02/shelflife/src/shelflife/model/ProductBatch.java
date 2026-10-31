package shelflife.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an inventory batch of a specific product with an expiration date.
 * Crucial for FEFO (First-Expired, First-Out) stock allocation.
 */
public class ProductBatch {
    private String batchId;
    private String productId;
    private int quantity;
    private LocalDate expiryDate;
    private LocalDate receivedDate;

    public ProductBatch() {
    }

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate, LocalDate receivedDate) {
        this.batchId = batchId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.receivedDate = receivedDate;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    /**
     * Checks if this batch is expired relative to a reference date.
     * Expired batches are strictly excluded from ordinary sales.
     */
    public boolean isExpired(LocalDate referenceDate) {
        if (expiryDate == null || referenceDate == null) {
            return false;
        }
        return expiryDate.isBefore(referenceDate);
    }

    /**
     * Deducts a quantity from this batch.
     */
    public void deductQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot deduct negative amount: " + amount);
        }
        if (amount > this.quantity) {
            throw new IllegalArgumentException("Cannot deduct " + amount + " from batch " + batchId + " with remaining quantity " + this.quantity);
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
        return "ProductBatch{" +
                "batchId='" + batchId + '\'' +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", expiryDate=" + expiryDate +
                ", receivedDate=" + receivedDate +
                '}';
    }
}
