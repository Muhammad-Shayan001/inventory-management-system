package shelflife.model;

import java.math.BigDecimal;

/**
 * Line item within a held bill session.
 */
public class HeldBillItem {
    private String heldItemId;
    private String heldBillId;
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private String batchId;

    public HeldBillItem() {
        this.unitPrice = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
    }

    public HeldBillItem(String heldItemId, String heldBillId, String productId,
                        String productName, int quantity, BigDecimal unitPrice,
                        BigDecimal discountAmount, String batchId) {
        this.heldItemId = heldItemId;
        this.heldBillId = heldBillId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.batchId = batchId;
    }

    public String getHeldItemId() {
        return heldItemId;
    }

    public void setHeldItemId(String heldItemId) {
        this.heldItemId = heldItemId;
    }

    public String getHeldBillId() {
        return heldBillId;
    }

    public void setHeldBillId(String heldBillId) {
        this.heldBillId = heldBillId;
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

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public BigDecimal getLineTotal() {
        BigDecimal base = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal tot = base.subtract(disc);
        return tot.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : tot;
    }
}
