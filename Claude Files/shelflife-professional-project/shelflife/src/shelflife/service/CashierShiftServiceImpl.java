package shelflife.service;

import shelflife.model.CashierShift;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementation of CashierShiftService.
 */
public class CashierShiftServiceImpl implements CashierShiftService {

    private final DataStore dataStore;
    private final List<CashierShift> shifts = new ArrayList<>();
    private CashierShift activeShift = null;

    public CashierShiftServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            shifts.clear();
            shifts.addAll(dataStore.loadCashierShifts());
            activeShift = null;
            for (CashierShift s : shifts) {
                if ("OPEN".equalsIgnoreCase(s.getStatus())) {
                    activeShift = s;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load cashier shifts: " + e.getMessage());
        }
    }

    @Override
    public synchronized CashierShift getCurrentOpenShift(String cashierUsername) {
        if (activeShift != null && "OPEN".equalsIgnoreCase(activeShift.getStatus())) {
            return activeShift;
        }
        for (CashierShift s : shifts) {
            if ("OPEN".equalsIgnoreCase(s.getStatus()) &&
                (cashierUsername == null || s.getCashierUsername().equalsIgnoreCase(cashierUsername.trim()))) {
                activeShift = s;
                return s;
            }
        }
        return null;
    }

    @Override
    public synchronized CashierShift openShift(String cashierUsername, BigDecimal openingFloat, String notes) throws Exception {
        CashierShift current = getCurrentOpenShift(cashierUsername);
        if (current != null) {
            throw new IllegalArgumentException("An active shift is already open for " + current.getCashierUsername() + " (Shift ID: " + current.getShiftId() + "). Please close it first.");
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String shiftId = "SHIFT-" + stamp + "-" + (int)(Math.random() * 900 + 100);

        BigDecimal floatAmt = openingFloat != null ? openingFloat : BigDecimal.ZERO;
        CashierShift newShift = new CashierShift(shiftId, cashierUsername != null ? cashierUsername : "cashier",
                LocalDateTime.now(), null, floatAmt, BigDecimal.ZERO, BigDecimal.ZERO, floatAmt, BigDecimal.ZERO, BigDecimal.ZERO, notes, "OPEN");

        dataStore.executeTransaction(() -> {
            shifts.add(newShift);
            dataStore.saveCashierShifts(shifts);
            activeShift = newShift;
        });

        return newShift;
    }

    @Override
    public synchronized void recordCashSale(BigDecimal amount) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (activeShift != null && "OPEN".equalsIgnoreCase(activeShift.getStatus())) {
            dataStore.executeTransaction(() -> {
                activeShift.setCashSales(activeShift.getCashSales().add(amount));
                activeShift.setExpectedCash(activeShift.calculateExpectedCash());
                dataStore.saveCashierShifts(shifts);
            });
        }
    }

    @Override
    public synchronized void recordCashRefund(BigDecimal amount) throws Exception {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        if (activeShift != null && "OPEN".equalsIgnoreCase(activeShift.getStatus())) {
            dataStore.executeTransaction(() -> {
                activeShift.setCashRefunds(activeShift.getCashRefunds().add(amount));
                activeShift.setExpectedCash(activeShift.calculateExpectedCash());
                dataStore.saveCashierShifts(shifts);
            });
        }
    }

    @Override
    public synchronized CashierShift closeShift(String shiftId, BigDecimal actualCashCounted, String closeNotes) throws Exception {
        CashierShift s = null;
        for (CashierShift item : shifts) {
            if (item.getShiftId().equalsIgnoreCase(shiftId.trim())) {
                s = item;
                break;
            }
        }
        if (s == null) {
            throw new IllegalArgumentException("Shift not found: " + shiftId);
        }

        final CashierShift shiftToClose = s;
        dataStore.executeTransaction(() -> {
            shiftToClose.reconcile(actualCashCounted, closeNotes);
            dataStore.saveCashierShifts(shifts);
            if (activeShift != null && activeShift.getShiftId().equalsIgnoreCase(shiftToClose.getShiftId())) {
                activeShift = null;
            }
        });

        return shiftToClose;
    }

    @Override
    public synchronized List<CashierShift> getAllShifts() {
        return Collections.unmodifiableList(new ArrayList<>(shifts));
    }
}
