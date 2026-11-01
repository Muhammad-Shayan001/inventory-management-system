package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Line item of a return transaction.
 */
public class SaleReturnItem {
    private String returnItemId;
    private String returnId;
    private String productId;
    private String productName;
    private String batchId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineRefund;
    private String disposition; // "RESTOCK" or "WASTAGE"

    public SaleReturnItem() {
        this.disposition = "RESTOCK";
    }

    public SaleReturnItem(String returnItemId, String returnId, String productId, String productName, String batchId, int quantity, BigDecimal unitPrice, BigDecimal lineRefund, String disposition) {
        this.returnItemId = returnItemId;
        this.returnId = returnId;
        this.productId = productId;
        this.productName = productName;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineRefund = lineRefund != null ? lineRefund.setScale(2, RoundingMode.HALF_UP) : 
                (unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        this.disposition = disposition != null ? disposition : "RESTOCK";
    }

    public String getReturnItemId() {
        return returnItemId;
    }

    public void setReturnItemId(String returnItemId) {
        this.returnItemId = returnItemId;
    }

    public String getReturnId() {
        return returnId;
    }

    public void setReturnId(String returnId) {
        this.returnId = returnId;
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
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getLineRefund() {
        return lineRefund;
    }

    public void setLineRefund(BigDecimal lineRefund) {
        this.lineRefund = lineRefund != null ? lineRefund.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition != null ? disposition : "RESTOCK";
    }

    public boolean isRestocked() {
        return "RESTOCK".equalsIgnoreCase(disposition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleReturnItem that = (SaleReturnItem) o;
        return Objects.equals(returnItemId, that.returnItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnItemId);
    }
}
