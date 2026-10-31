package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Line item in a sale record.
 * A SaleItem must identify the batch from which the units were allocated.
 * When one product is allocated across several batches, save separate SaleItem
 * records for those batch allocations.
 * Store the actual unit price charged so historical receipts remain correct
 * after the current product price changes.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class SaleItem {
    private String saleItemId;
    private String saleId;
    private String productId;
    private String batchId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public SaleItem() {
        this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public SaleItem(String saleItemId, String saleId, String productId, String batchId,
                    int quantity, BigDecimal unitPrice) {
        this.saleItemId = saleItemId;
        this.saleId = saleId;
        this.productId = productId;
        this.batchId = batchId;
        this.quantity = quantity;
        setUnitPrice(unitPrice);
        calculateLineTotal();
    }

    public String getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(String saleItemId) {
        if (saleItemId == null || saleItemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sale item ID cannot be null or empty.");
        }
        this.saleItemId = saleItemId.trim().toUpperCase();
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        if (saleId == null || saleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sale ID cannot be null or empty.");
        }
        this.saleId = saleId.trim().toUpperCase();
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
        this.quantity = quantity;
        calculateLineTotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        }
        calculateLineTotal();
    }

    public BigDecimal getLineTotal() {
        if (lineTotal == null) {
            calculateLineTotal();
        }
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal != null ? lineTotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates line total as unitPrice * quantity using exact BigDecimal arithmetic.
     */
    private void calculateLineTotal() {
        if (this.unitPrice != null && this.quantity > 0) {
            this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleItem saleItem = (SaleItem) o;
        return Objects.equals(saleItemId, saleItem.saleItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleItemId);
    }

    @Override
    public String toString() {
        return "SaleItem{" +
                "saleItemId='" + saleItemId + '\'' +
                ", batchId='" + batchId + '\'' +
                ", productId='" + productId + '\'' +
                ", qty=" + quantity +
                ", unitPrice=" + unitPrice +
                ", total=" + lineTotal +
                '}';
    }
}