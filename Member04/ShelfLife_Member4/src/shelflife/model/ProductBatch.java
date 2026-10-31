package shelflife.model;

import java.time.LocalDate;

public class ProductBatch {
    private String batchId;
    private String productId;
    private int quantity;
    private LocalDate expiryDate;

    public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate) {
        this.batchId = batchId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
