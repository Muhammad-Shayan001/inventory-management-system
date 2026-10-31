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
 * Sale and stock changes are persisted through the approved recoverable transaction mechanism.
 * Cancelled sales do not modify stock. Historical sales retain the actual price charged
 * and batch allocation. Payment information is recorded; no real card transaction is processed.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class Sale {
    private String saleId;
    private LocalDateTime saleDateTime;
    private List<SaleItem> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal finalTotal;
    private String paymentMethod;
    private String paymentStatus;

    public Sale() {
        this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.finalTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paymentStatus = "PENDING";
    }

    public Sale(String saleId, LocalDateTime saleDateTime, String paymentMethod, String paymentStatus) {
        this();
        this.saleId = saleId;
        this.saleDateTime = saleDateTime;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    public String getSaleId() {
        return saleId;
    }

    public void setSaleId(String saleId) {
        if (saleId == null || saleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sale ID cannot be null or empty.");
        }
        this.saleId = saleId.trim().toUpperCase();
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

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal != null ? finalTotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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

    /**
     * Recalculates subtotal and final total from items using exact BigDecimal arithmetic.
     */
    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (SaleItem item : items) {
            sum = sum.add(item.getLineTotal());
        }
        this.subtotal = sum;
        this.tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.finalTotal = this.subtotal.add(this.tax).setScale(2, RoundingMode.HALF_UP);
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
                ", dateTime=" + saleDateTime +
                ", itemsCount=" + items.size() +
                ", total=" + finalTotal +
                ", method=" + paymentMethod +
                ", status=" + paymentStatus +
                '}';
    }
}