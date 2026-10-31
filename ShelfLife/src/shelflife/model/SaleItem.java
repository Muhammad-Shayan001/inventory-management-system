package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Line item in a sale record identifying the allocated batch and price charged at sale time.
 * When one product is fulfilled from multiple batches, separate SaleItem records are created.
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
    private BigDecimal costPrice;

    public SaleItem() {
        this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.costPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public SaleItem(String saleId, String productId, String batchId, int quantity, BigDecimal unitPrice) {
        this(null, saleId, productId, productId, batchId, quantity, unitPrice);
    }

    public SaleItem(String saleId, String productId, String batchId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lineTotal = lineTotal != null ? lineTotal.setScale(2, RoundingMode.HALF_UP) : this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public SaleItem(String saleId, String productId, String productName, String batchId, int quantity, BigDecimal unitPrice) {
        this(null, saleId, productId, productName, batchId, quantity, unitPrice);
    }

    public SaleItem(String saleItemId, String saleId, String productId, String productName, String batchId, int quantity, BigDecimal unitPrice) {
        this.saleItemId = saleItemId;
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName != null ? productName : productId;
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
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity = quantity;
        calculateLineTotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null.");
        }
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        calculateLineTotal();
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        if (lineTotal != null) {
            this.lineTotal = lineTotal.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getCostPrice() {
        return costPrice != null ? costPrice : BigDecimal.ZERO;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice != null ? costPrice.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public void calculateLineTotal() {
        if (this.unitPrice != null) {
            this.lineTotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.lineTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public String toString() {
        return "SaleItem[" + (saleItemId != null ? saleItemId : "") + " | Prod: " + productId + " (" + getProductName() + 
                ") | Batch: " + batchId + " | Qty: " + quantity + " @ " + unitPrice + " = " + lineTotal + "]";
    }
}
