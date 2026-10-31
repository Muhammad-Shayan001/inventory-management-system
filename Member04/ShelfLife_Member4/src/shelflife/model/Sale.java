package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {
    private String saleId;
    private LocalDateTime saleDateTime;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;

    public Sale(String saleId, LocalDateTime saleDateTime, BigDecimal totalAmount, String paymentMethod, String paymentStatus) {
        this.saleId = saleId;
        this.saleDateTime = saleDateTime;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    public String getSaleId() { return saleId; }
    public void setSaleId(String saleId) { this.saleId = saleId; }

    public LocalDateTime getSaleDateTime() { return saleDateTime; }
    public void setSaleDateTime(LocalDateTime saleDateTime) { this.saleDateTime = saleDateTime; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
