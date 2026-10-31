package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Line item in a sale record.
 * BUSINESS RULES:
 * - A SaleItem must identify the batch from which the units were allocated.
 * - When one product is allocated across several batches, save separate SaleItem
 *   records for those batch allocations.
 * - Store the actual unit price charged so historical receipts remain correct
 *   after the current product price changes.
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

    public SaleItem(String saleItemId, String saleId, String productId, String productName,
                    String batchId, int quantity, BigDecimal unitPrice) {
        this.saleItemId = saleItemId;
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.batchId = batchId;
        this.quantity = quantity;
        setUnitPrice(unitPrice);
        calculateLineTotal();
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
        return productName;
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
                ", product='" + productName + '\'' +
                ", qty=" + quantity +
                ", unitPrice=" + unitPrice +
                ", total=" + lineTotal +
                '}';
    }
}
