package shelflife.service;

import shelflife.model.HeldBill;
import shelflife.model.HeldBillItem;
import shelflife.storage.DataStore;
import shelflife.ui.billing.CartItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementation of HeldBillService managing held cart transactions with persistence.
 */
public class HeldBillServiceImpl implements HeldBillService {

    private final DataStore dataStore;
    private final List<HeldBill> heldBills = new ArrayList<>();
    private final List<HeldBillItem> heldBillItems = new ArrayList<>();

    public HeldBillServiceImpl(DataStore dataStore) {
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            heldBills.clear();
            heldBills.addAll(dataStore.loadHeldBills());
            heldBillItems.clear();
            heldBillItems.addAll(dataStore.loadHeldBillItems());
        } catch (Exception e) {
            System.err.println("Warning: Failed to load held bills: " + e.getMessage());
        }
    }

    @Override
    public synchronized HeldBill holdBill(String cashierUsername, String customerName, String note, List<CartItem> items) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cannot hold an empty cart.");
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        String heldBillId = "HOLD-" + stamp + "-" + (int) (Math.random() * 900 + 100);

        BigDecimal total = BigDecimal.ZERO;
        int totalUnits = 0;
        List<HeldBillItem> newItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            CartItem ci = items.get(i);
            String itemId = heldBillId + "-ITM" + (i + 1);
            HeldBillItem hbi = new HeldBillItem(itemId, heldBillId, ci.getProductId(),
                    ci.getProductName(), ci.getQuantity(), ci.getUnitPrice(), BigDecimal.ZERO, "");
            newItems.add(hbi);
            total = total.add(ci.getLineTotal());
            totalUnits += ci.getQuantity();
        }

        HeldBill bill = new HeldBill(heldBillId, LocalDateTime.now(), cashierUsername, customerName, note, totalUnits, total);
        bill.setItems(newItems);

        dataStore.executeTransaction(() -> {
            heldBills.add(bill);
            heldBillItems.addAll(newItems);
            dataStore.saveHeldBills(heldBills);
            dataStore.saveHeldBillItems(heldBillItems);
        });

        return bill;
    }

    @Override
    public synchronized List<HeldBill> getAllHeldBills() {
        return Collections.unmodifiableList(new ArrayList<>(heldBills));
    }

    @Override
    public synchronized HeldBill getHeldBill(String heldBillId) {
        if (heldBillId == null) return null;
        for (HeldBill b : heldBills) {
            if (b.getHeldBillId().equalsIgnoreCase(heldBillId.trim())) {
                b.setItems(getHeldBillItems(b.getHeldBillId()));
                return b;
            }
        }
        return null;
    }

    @Override
    public synchronized List<HeldBillItem> getHeldBillItems(String heldBillId) {
        List<HeldBillItem> list = new ArrayList<>();
        if (heldBillId == null) return list;
        for (HeldBillItem it : heldBillItems) {
            if (it.getHeldBillId().equalsIgnoreCase(heldBillId.trim())) {
                list.add(it);
            }
        }
        return list;
    }

    @Override
    public synchronized List<HeldBillItem> resumeBill(String heldBillId) throws Exception {
        HeldBill bill = getHeldBill(heldBillId);
        if (bill == null) {
            throw new IllegalArgumentException("Held bill not found: " + heldBillId);
        }

        List<HeldBillItem> itemsToRestore = getHeldBillItems(heldBillId);

        dataStore.executeTransaction(() -> {
            heldBills.removeIf(b -> b.getHeldBillId().equalsIgnoreCase(heldBillId.trim()));
            heldBillItems.removeIf(it -> it.getHeldBillId().equalsIgnoreCase(heldBillId.trim()));
            dataStore.saveHeldBills(heldBills);
            dataStore.saveHeldBillItems(heldBillItems);
        });

        return itemsToRestore;
    }

    @Override
    public synchronized void discardBill(String heldBillId) throws Exception {
        if (heldBillId == null) return;
        dataStore.executeTransaction(() -> {
            heldBills.removeIf(b -> b.getHeldBillId().equalsIgnoreCase(heldBillId.trim()));
            heldBillItems.removeIf(it -> it.getHeldBillId().equalsIgnoreCase(heldBillId.trim()));
            dataStore.saveHeldBills(heldBills);
            dataStore.saveHeldBillItems(heldBillItems);
        });
    }
}
