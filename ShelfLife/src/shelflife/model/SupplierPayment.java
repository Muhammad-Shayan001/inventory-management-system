package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Record of payment disbursed to a supplier against a payable account.
 */
public class SupplierPayment {
    private String paymentId;
    private String payableId;
    private String supplierId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
    private String recordedBy;

    public SupplierPayment() {
        this.paymentDate = LocalDate.now();
        this.amount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public SupplierPayment(String paymentId, String payableId, String supplierId,
                           LocalDate paymentDate, BigDecimal amount, String paymentMethod,
                           String referenceNumber, String notes, String recordedBy) {
        this.paymentId = paymentId;
        this.payableId = payableId;
        this.supplierId = supplierId;
        this.paymentDate = paymentDate != null ? paymentDate : LocalDate.now();
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.referenceNumber = referenceNumber != null ? referenceNumber : "";
        this.notes = notes != null ? notes : "";
        this.recordedBy = recordedBy != null ? recordedBy : "admin";
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }
}
