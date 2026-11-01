package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tracks credit obligations to suppliers for inventory shipments received on credit.
 */
public class SupplierPayable {
    private String payableId;
    private String supplierId;
    private String purchaseRef;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String status; // UNPAID, PARTIAL, PAID, OVERDUE
    private String notes;

    public SupplierPayable() {
        this.invoiceDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        this.totalAmount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.paidAmount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.balanceDue = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.status = "UNPAID";
    }

    public SupplierPayable(String payableId, String supplierId, String purchaseRef,
                           LocalDate invoiceDate, LocalDate dueDate, BigDecimal totalAmount,
                           BigDecimal paidAmount, BigDecimal balanceDue, String status, String notes) {
        this.payableId = payableId;
        this.supplierId = supplierId;
        this.purchaseRef = purchaseRef != null ? purchaseRef : "";
        this.invoiceDate = invoiceDate != null ? invoiceDate : LocalDate.now();
        this.dueDate = dueDate != null ? dueDate : LocalDate.now().plusDays(30);
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.paidAmount = paidAmount != null ? paidAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.balanceDue = balanceDue != null ? balanceDue.setScale(2, BigDecimal.ROUND_HALF_UP) : this.totalAmount.subtract(this.paidAmount);
        this.status = status != null ? status : "UNPAID";
        this.notes = notes != null ? notes : "";
        updateStatus();
    }

    public void updateStatus() {
        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            status = "PAID";
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = "PARTIAL";
        } else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            status = "OVERDUE";
        } else {
            status = "UNPAID";
        }
    }

    public String getPayableId() {
        return payableId;
    }

    public void setPayableId(String payableId) {
        this.payableId = payableId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getPurchaseRef() {
        return purchaseRef;
    }

    public void setPurchaseRef(String purchaseRef) {
        this.purchaseRef = purchaseRef;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        updateStatus();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        this.balanceDue = this.totalAmount.subtract(this.paidAmount);
        updateStatus();
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount != null ? paidAmount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        this.balanceDue = this.totalAmount.subtract(this.paidAmount);
        updateStatus();
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue != null ? balanceDue.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        updateStatus();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
