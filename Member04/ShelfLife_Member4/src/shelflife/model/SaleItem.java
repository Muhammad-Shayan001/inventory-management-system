package shelflife.model;

import java.math.BigDecimal;

public class SaleItem {
    private String saleId;
    private String productId;
    private String batchId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public SaleItem(String saleId, String productId, String batchId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.saleId = saleId;
        this.productId = productId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
