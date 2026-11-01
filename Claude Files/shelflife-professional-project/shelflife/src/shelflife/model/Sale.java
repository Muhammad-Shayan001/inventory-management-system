package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a completed sales transaction with line items and financial totals.
 * Uses exact BigDecimal arithmetic for all monetary values.
 */
public class Sale {
    private String saleId;
    private LocalDateTime saleDateTime;
    private final List<SaleItem> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String notes;
    private String cashierUsername;
    private String customerId;
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private int reprintCount = 0;
    private final List<PaymentLine> paymentLines = new ArrayList<>();

    public Sale() {
        this.saleDateTime = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paymentMethod = PaymentMethod.CASH;
        this.paymentStatus = PaymentStatus.PAID;
    }

    public Sale(String saleId, LocalDateTime saleDateTime, BigDecimal totalAmount,
                PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
        this.saleId = saleId;
        this.saleDateTime = saleDateTime != null ? saleDateTime : LocalDateTime.now();
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.subtotal = this.totalAmount;
        this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CASH;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PAID;
    }

    public Sale(String saleId, LocalDateTime saleDateTime, BigDecimal subtotal, BigDecimal tax,
                BigDecimal totalAmount, PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
        this.saleId = saleId;
        this.saleDateTime = saleDateTime != null ? saleDateTime : LocalDateTime.now();
        this.subtotal = subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.tax = tax != null ? tax.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CASH;
        this.paymentStatus = paymentStatus != null ? paymentStatus : PaymentStatus.PAID;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        this.saleId = saleId;
    }

    public LocalDateTime getSaleDateTime() {
        return saleDateTime;
    }

    public void setSaleDateTime(LocalDateTime saleDateTime) {
        this.saleDateTime = saleDateTime;
    }

    public List<SaleItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(SaleItem item) {
        if (item != null) {
            items.add(item);
            recalculateTotals();
        }
    }

    public void setItems(List<SaleItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        recalculateTotals();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        if (subtotal != null) {
            this.subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        if (tax != null) {
            this.tax = tax.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        if (totalAmount != null) {
            this.totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal getFinalTotal() {
        return totalAmount;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        setTotalAmount(finalTotal);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public int getReprintCount() {
        return reprintCount;
    }

    public void setReprintCount(int reprintCount) {
        this.reprintCount = reprintCount;
    }

    public void incrementReprintCount() {
        this.reprintCount++;
    }

    public boolean isDuplicateReceipt() {
        return reprintCount > 0;
    }

    public List<PaymentLine> getPaymentLines() {
        return paymentLines;
    }

    public void setPaymentLines(List<PaymentLine> lines) {
        this.paymentLines.clear();
        if (lines != null) {
            this.paymentLines.addAll(lines);
        }
    }

    public void addPaymentLine(PaymentLine line) {
        if (line != null) {
            this.paymentLines.add(line);
        }
    }

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (SaleItem item : items) {
            if (item != null && item.getLineTotal() != null) {
                sum = sum.add(item.getLineTotal());
            }
        }
        this.subtotal = sum;
        if (this.tax == null) {
            this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        this.totalAmount = this.subtotal.add(this.tax).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Sale[" + saleId + " | Time: " + saleDateTime + " | Items: " + items.size() + 
                " | Total: Rs " + (totalAmount != null ? totalAmount.toPlainString() : "0.00") + 
                " | " + paymentMethod + " / " + paymentStatus + "]";
    }
}
