package shelflife.service;

import shelflife.model.HeldBill;
import shelflife.model.HeldBillItem;
import shelflife.ui.billing.CartItem;

import java.util.List;

/**
 * Service contract for holding, resuming, and managing suspended POS cart sessions.
 */
public interface HeldBillService {

    HeldBill holdBill(String cashierUsername, String customerName, String note, List<CartItem> items) throws Exception;

    List<HeldBill> getAllHeldBills();

    HeldBill getHeldBill(String heldBillId);

    List<HeldBillItem> getHeldBillItems(String heldBillId);

    /**
     * Resumes a held bill, removing it from storage and returning its items for checkout restoration.
     */
    List<HeldBillItem> resumeBill(String heldBillId) throws Exception;

    void discardBill(String heldBillId) throws Exception;

    void reload();
}
