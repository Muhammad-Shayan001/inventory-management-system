package shelflife.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Shared SaleItem model matching Member 3/4 specification.
 * Identifies the batch from which items were allocated.
 * Primary ownership: Member 3.
 */
public class SaleItem {
    private String saleId;
    private String productId;
    private String batchId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public SaleItem() {
    }

    public SaleItem(String saleId, String productId, String batchId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.saleId = saleId;
        this.productId = productId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleItem saleItem = (SaleItem) o;
        return quantity == saleItem.quantity &&
                Objects.equals(saleId, saleItem.saleId) &&
                Objects.equals(productId, saleItem.productId) &&
                Objects.equals(batchId, saleItem.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId, productId, batchId, quantity);
    }
}
