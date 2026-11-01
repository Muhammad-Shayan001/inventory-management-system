package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Line representing an individual payment portion in a split or single payment transaction.
 */
public class PaymentLine {
    private String paymentLineId;
    private String saleId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String referenceNote;
    private LocalDateTime timestamp;

    public PaymentLine() {
        this.amount = BigDecimal.ZERO;
        this.timestamp = LocalDateTime.now();
    }

    public PaymentLine(String paymentLineId, String saleId, PaymentMethod paymentMethod,
                       BigDecimal amount, String referenceNote, LocalDateTime timestamp) {
        this.paymentLineId = paymentLineId;
        this.saleId = saleId;
        this.paymentMethod = paymentMethod;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.referenceNote = referenceNote != null ? referenceNote : "";
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public String getPaymentLineId() {
        return paymentLineId;
    }

    public void setPaymentLineId(String paymentLineId) {
        this.paymentLineId = paymentLineId;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceNote() {
        return referenceNote;
    }

    public void setReferenceNote(String referenceNote) {
        this.referenceNote = referenceNote;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
