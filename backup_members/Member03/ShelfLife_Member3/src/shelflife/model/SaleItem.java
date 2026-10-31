package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a single line item within a sale, allocated to a specific inventory batch.
 * Per ShelfLife business rules:
 * - A SaleItem must identify the batch from which the units were allocated.
 * - When one product is allocated across several batches, separate SaleItem records
 *   are created for each batch allocation.
 * - The actual unit price charged is recorded here so historical receipts remain correct
 *   even if product prices change in the future.
 *
 * Primary Model Ownership: Member 3.
 */
public class SaleItem {
    private String saleItemId;
    private String saleId;
    private String productId;
    private String productName;
    private String batchId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public SaleItem() {
        this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Standard constructor matching the team contract specification.
     */
    public SaleItem(String saleId, String productId, String batchId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.saleId = saleId;
        this.productId = productId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineTotal = lineTotal != null ? lineTotal.setScale(2, RoundingMode.HALF_UP) : this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Rich constructor with product name for clear receipt presentation.
     */
    public SaleItem(String saleId, String productId, String productName, String batchId, int quantity, BigDecimal unitPrice) {
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public String getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(String saleItemId) {
        this.saleItemId = saleItemId;
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

    public String getProductName() {
        return productName != null ? productName : productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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
        recalculateLineTotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        recalculateLineTotal();
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal != null ? lineTotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void recalculateLineTotal() {
        if (unitPrice != null && quantity >= 0) {
            this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        }
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

    @Override
    public String toString() {
        return "SaleItem{" +
                "saleId='" + saleId + '\'' +
                ", productId='" + productId + '\'' +
                ", batchId='" + batchId + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", lineTotal=" + lineTotal +
                '}';
    }
}
