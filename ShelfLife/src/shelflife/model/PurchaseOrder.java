package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Purchase invoice/order record capturing inventory acquisition and supplier obligation.
 *
 * Statuses:
 * - ORDERED: PO sent to supplier (does NOT increase inventory)
 * - RECEIVED: Goods accepted and added to physical inventory batches
 * - CANCELLED: Order cancelled
 */
public class PurchaseOrder {

    public enum Status {
        ORDERED("Ordered"),
        RECEIVED("Received & Stocked"),
        CANCELLED("Cancelled");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String purchaseId;
    private String supplierId;
    private String supplierInvoiceNo;
    private LocalDate orderDate;
    private LocalDate receivedDate;
    private String productId;
    private String batchId;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal additionalCost; // Freight, transport allocated
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String paymentMethod;
    private String paymentAccount;
    private LocalDate expiryDate;
    private Status status;
    private String createdBy;
    private String notes;

    public PurchaseOrder() {
        this.orderDate = LocalDate.now();
        this.unitCost = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.additionalCost = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.totalAmount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.paidAmount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.unpaidAmount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.status = Status.ORDERED;
        this.paymentMethod = "CASH";
        this.paymentAccount = "MAIN_DRAWER";
        this.createdBy = "admin";
    }

    public PurchaseOrder(String purchaseId, String supplierId, String supplierInvoiceNo,
                         LocalDate orderDate, LocalDate receivedDate, String productId,
                         String batchId, int quantity, BigDecimal unitCost,
                         BigDecimal additionalCost, BigDecimal totalAmount,
                         BigDecimal paidAmount, String paymentMethod, String paymentAccount,
                         LocalDate expiryDate, Status status, String createdBy, String notes) {
        this.purchaseId = purchaseId;
        this.supplierId = supplierId;
        this.supplierInvoiceNo = supplierInvoiceNo != null ? supplierInvoiceNo.trim() : "";
        this.orderDate = orderDate != null ? orderDate : LocalDate.now();
        this.receivedDate = receivedDate;
        this.productId = productId;
        this.batchId = batchId != null ? batchId.trim().toUpperCase() : "";
        this.quantity = quantity;
        this.unitCost = unitCost != null ? unitCost.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.additionalCost = additionalCost != null ? additionalCost.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : this.unitCost.multiply(BigDecimal.valueOf(quantity)).add(this.additionalCost).setScale(2, BigDecimal.ROUND_HALF_UP);
        this.paidAmount = paidAmount != null ? paidAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.unpaidAmount = this.totalAmount.subtract(this.paidAmount).max(BigDecimal.ZERO);
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.paymentAccount = paymentAccount != null ? paymentAccount : "MAIN_DRAWER";
        this.expiryDate = expiryDate != null ? expiryDate : LocalDate.now().plusMonths(12);
        this.status = status != null ? status : Status.ORDERED;
        this.createdBy = createdBy != null ? createdBy : "admin";
        this.notes = notes != null ? notes : "";
    }

    public BigDecimal calculateUnitLandedCost() {
        if (quantity <= 0) return unitCost;
        BigDecimal allocatedPerUnit = additionalCost.divide(BigDecimal.valueOf(quantity), 4, BigDecimal.ROUND_HALF_UP);
        return unitCost.add(allocatedPerUnit).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierInvoiceNo() {
        return supplierInvoiceNo;
    }

    public void setSupplierInvoiceNo(String supplierInvoiceNo) {
        this.supplierInvoiceNo = supplierInvoiceNo;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
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

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost != null ? unitCost.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getAdditionalCost() {
        return additionalCost;
    }

    public void setAdditionalCost(BigDecimal additionalCost) {
        this.additionalCost = additionalCost != null ? additionalCost.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        this.unpaidAmount = this.totalAmount.subtract(this.paidAmount).max(BigDecimal.ZERO);
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount != null ? paidAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        this.unpaidAmount = this.totalAmount.subtract(this.paidAmount).max(BigDecimal.ZERO);
    }

    public BigDecimal getUnpaidAmount() {
        return unpaidAmount;
    }

    public void setUnpaidAmount(BigDecimal unpaidAmount) {
        this.unpaidAmount = unpaidAmount != null ? unpaidAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentAccount() {
        return paymentAccount;
    }

    public void setPaymentAccount(String paymentAccount) {
        this.paymentAccount = paymentAccount;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
