package shelflife.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks a cashier's working shift and cash register reconciliation.
 * Formula:
 *   Expected Cash = Opening Float + Cash Sales - Cash Refunds
 *   Cash Difference = Actual Cash Counted - Expected Cash
 */
public class CashierShift {
    private String shiftId;
    private String cashierUsername;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingFloat;
    private BigDecimal cashSales;
    private BigDecimal cashRefunds;
    private BigDecimal expectedCash;
    private BigDecimal actualCash;
    private BigDecimal cashDifference;
    private String notes;
    private String status; // OPEN, CLOSED

    public CashierShift() {
        this.openedAt = LocalDateTime.now();
        this.openingFloat = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashSales = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashRefunds = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.expectedCash = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.actualCash = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashDifference = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.status = "OPEN";
    }

    public CashierShift(String shiftId, String cashierUsername, LocalDateTime openedAt,
                        LocalDateTime closedAt, BigDecimal openingFloat, BigDecimal cashSales,
                        BigDecimal cashRefunds, BigDecimal expectedCash, BigDecimal actualCash,
                        BigDecimal cashDifference, String notes, String status) {
        this.shiftId = shiftId;
        this.cashierUsername = cashierUsername;
        this.openedAt = openedAt != null ? openedAt : LocalDateTime.now();
        this.closedAt = closedAt;
        this.openingFloat = openingFloat != null ? openingFloat.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashSales = cashSales != null ? cashSales.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashRefunds = cashRefunds != null ? cashRefunds.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.expectedCash = expectedCash != null ? expectedCash.setScale(2, BigDecimal.ROUND_HALF_UP) : calculateExpectedCash();
        this.actualCash = actualCash != null ? actualCash.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashDifference = cashDifference != null ? cashDifference.setScale(2, BigDecimal.ROUND_HALF_UP) : this.actualCash.subtract(this.expectedCash);
        this.notes = notes != null ? notes : "";
        this.status = status != null ? status : "OPEN";
    }

    public BigDecimal calculateExpectedCash() {
        return openingFloat.add(cashSales).subtract(cashRefunds).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public void reconcile(BigDecimal actualCountedCash, String closeNotes) {
        this.closedAt = LocalDateTime.now();
        this.expectedCash = calculateExpectedCash();
        this.actualCash = actualCountedCash != null ? actualCountedCash.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
        this.cashDifference = this.actualCash.subtract(this.expectedCash);
        if (closeNotes != null && !closeNotes.trim().isEmpty()) {
            this.notes = (this.notes == null || this.notes.isEmpty()) ? closeNotes : this.notes + " | " + closeNotes;
        }
        this.status = "CLOSED";
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getCashierUsername() {
        return cashierUsername;
    }

    public void setCashierUsername(String cashierUsername) {
        this.cashierUsername = cashierUsername;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getOpeningFloat() {
        return openingFloat;
    }

    public void setOpeningFloat(BigDecimal openingFloat) {
        this.openingFloat = openingFloat != null ? openingFloat.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getCashSales() {
        return cashSales;
    }

    public void setCashSales(BigDecimal cashSales) {
        this.cashSales = cashSales != null ? cashSales.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getCashRefunds() {
        return cashRefunds;
    }

    public void setCashRefunds(BigDecimal cashRefunds) {
        this.cashRefunds = cashRefunds != null ? cashRefunds.setScale(2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(BigDecimal expectedCash) {
        this.expectedCash = expectedCash;
    }

    public BigDecimal getActualCash() {
        return actualCash;
    }

    public void setActualCash(BigDecimal actualCash) {
        this.actualCash = actualCash;
    }

    public BigDecimal getCashDifference() {
        return cashDifference;
    }

    public void setCashDifference(BigDecimal cashDifference) {
        this.cashDifference = cashDifference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
