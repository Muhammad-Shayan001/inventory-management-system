package shelflife.service;

import shelflife.model.CashierShift;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service contract for cashier shifts and cash drawer reconciliation.
 */
public interface CashierShiftService {

    CashierShift getCurrentOpenShift(String cashierUsername);

    CashierShift openShift(String cashierUsername, BigDecimal openingFloat, String notes) throws Exception;

    void recordCashSale(BigDecimal amount) throws Exception;

    void recordCashRefund(BigDecimal amount) throws Exception;

    CashierShift closeShift(String shiftId, BigDecimal actualCashCounted, String closeNotes) throws Exception;

    default CashierShift getActiveShiftForCashier(String cashierUsername) {
        return getCurrentOpenShift(cashierUsername);
    }

    default void recordCashSale(String shiftId, BigDecimal amount) throws Exception {
        recordCashSale(amount);
    }

    List<CashierShift> getAllShifts();

    void reload();
}
