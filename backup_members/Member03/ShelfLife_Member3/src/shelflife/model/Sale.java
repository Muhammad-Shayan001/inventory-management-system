package shelflife.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a completed sales transaction in the ShelfLife system.
 * Enforces strict financial calculations using BigDecimal.
 *
 * Primary Model Ownership: Member 3.
 */
public class Sale {
    private String saleId;
    private LocalDateTime saleDateTime;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private String paymentMethod;
    private String paymentStatus;
    private List<SaleItem> items = new ArrayList<>();
    private String notes;

    public Sale() {
        this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paymentStatus = PaymentStatus.PENDING.name();
    }

    /**
     * Standard 5-argument constructor matching the team contract specification.
     */
    public Sale(String saleId, LocalDateTime saleDateTime, BigDecimal totalAmount, String paymentMethod, String paymentStatus) {
        this();
        this.saleId = saleId;
        this.saleDateTime = saleDateTime;
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.subtotal = this.totalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    /**
     * Constructor using strong types for payment method and status.
     */
    public Sale(String saleId, LocalDateTime saleDateTime, PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
        this();
        this.saleId = saleId;
        this.saleDateTime = saleDateTime;
        this.paymentMethod = paymentMethod != null ? paymentMethod.getDisplayName() : PaymentMethod.CASH.getDisplayName();
        this.paymentStatus = paymentStatus != null ? paymentStatus.name() : PaymentStatus.PAID.name();
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    // Alias for getFinalTotal
    public BigDecimal getFinalTotal() {
        return getTotalAmount();
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        setTotalAmount(finalTotal);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax != null ? tax.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public List<SaleItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<SaleItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        recalculateTotals();
    }

    public void addItem(SaleItem item) {
        if (item != null) {
            this.items.add(item);
            recalculateTotals();
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Recalculates subtotal and totalAmount from items using exact BigDecimal arithmetic.
     */
    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (SaleItem item : items) {
            if (item.getLineTotal() != null) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sale sale = (Sale) o;
        return Objects.equals(saleId, sale.saleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId);
    }

    @Override
    public String toString() {
        return "Sale{" +
                "saleId='" + saleId + '\'' +
                ", saleDateTime=" + saleDateTime +
                ", totalAmount=" + totalAmount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", itemsCount=" + items.size() +
                '}';
    }
}
