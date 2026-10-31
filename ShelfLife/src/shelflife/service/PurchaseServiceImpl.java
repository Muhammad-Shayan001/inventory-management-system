package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;
import shelflife.util.CurrencyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Standard implementation of PurchaseService.
 * Coordinates stock intake, batch costing, supplier payables, and account deductions.
 */
public class PurchaseServiceImpl implements PurchaseService {

    private final DataStore dataStore;
    private final InventoryService inventoryService;
    private final SupplierPayableService payableService;
    private final CashAccountService cashAccountService;
    private final List<PurchaseOrder> purchases = new ArrayList<>();

    public PurchaseServiceImpl(DataStore dataStore, InventoryService inventoryService,
                               SupplierPayableService payableService, CashAccountService cashAccountService) {
        if (dataStore == null || inventoryService == null) {
            throw new IllegalArgumentException("Dependencies cannot be null.");
        }
        this.dataStore = dataStore;
        this.inventoryService = inventoryService;
        this.payableService = payableService;
        this.cashAccountService = cashAccountService;
        reload();
    }

    @Override
    public synchronized void reload() {
        purchases.clear();
        try {
            List<PurchaseOrder> loaded = dataStore.loadPurchaseOrders();
            if (loaded != null) {
                purchases.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load purchase orders: " + e.getMessage());
        }
    }

    @Override
    public synchronized PurchaseOrder createPurchaseOrder(String supplierId, String supplierInvoiceNo,
                                                          String productId, String batchId, int quantity,
                                                          BigDecimal unitCost, BigDecimal additionalCost,
                                                          BigDecimal paidAmount, String paymentMethod,
                                                          String paymentAccount, LocalDate expiryDate,
                                                          String createdBy, String notes) throws Exception {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative.");
        }

        String id = "PO-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 900 + 100);
        String finalBatchId = (batchId != null && !batchId.trim().isEmpty()) ? batchId.trim().toUpperCase() : "B-" + id;

        BigDecimal safeAddCost = additionalCost != null ? additionalCost : BigDecimal.ZERO;
        BigDecimal total = unitCost.multiply(BigDecimal.valueOf(quantity)).add(safeAddCost);
        BigDecimal safePaid = paidAmount != null ? paidAmount : BigDecimal.ZERO;

        PurchaseOrder po = new PurchaseOrder(
                id,
                supplierId != null ? supplierId : "SUP-DEFAULT",
                supplierInvoiceNo != null ? supplierInvoiceNo : "",
                LocalDate.now(),
                null,
                productId.trim().toUpperCase(),
                finalBatchId,
                quantity,
                unitCost,
                safeAddCost,
                total,
                safePaid,
                paymentMethod != null ? paymentMethod : "CASH",
                paymentAccount != null ? paymentAccount : "MAIN_DRAWER",
                expiryDate != null ? expiryDate : LocalDate.now().plusMonths(12),
                PurchaseOrder.Status.ORDERED,
                createdBy != null ? createdBy : "admin",
                notes != null ? notes : ""
        );

        dataStore.executeTransaction(() -> {
            List<PurchaseOrder> list = dataStore.loadPurchaseOrders();
            list.add(po);
            dataStore.savePurchaseOrders(list);
        });

        reload();
        return po;
    }

    @Override
    public synchronized PurchaseOrder receiveGoods(String purchaseId, String createdBy) throws Exception {
        PurchaseOrder po = getPurchase(purchaseId);
        if (po == null) {
            throw new IllegalArgumentException("Purchase order not found: " + purchaseId);
        }
        if (po.getStatus() == PurchaseOrder.Status.RECEIVED) {
            throw new IllegalStateException("Goods for purchase " + purchaseId + " have already been received.");
        }
        if (po.getStatus() == PurchaseOrder.Status.CANCELLED) {
            throw new IllegalStateException("Cannot receive goods for a cancelled purchase order.");
        }

        po.setStatus(PurchaseOrder.Status.RECEIVED);
        po.setReceivedDate(LocalDate.now());

        BigDecimal landedUnitCost = po.calculateUnitLandedCost();
        ProductBatch newBatch = new ProductBatch(
                po.getBatchId(),
                po.getProductId(),
                po.getQuantity(),
                po.getExpiryDate(),
                landedUnitCost,
                po.getSupplierId(),
                LocalDate.now(),
                false
        );

        dataStore.executeTransaction(() -> {
            // 1. Add batch to inventory
            List<ProductBatch> batches = dataStore.loadBatches();
            // Check if batchId already exists
            boolean exists = false;
            for (ProductBatch b : batches) {
                if (b.getBatchId().equalsIgnoreCase(newBatch.getBatchId())) {
                    b.setQuantity(b.getQuantity() + newBatch.getQuantity());
                    b.setCostPrice(landedUnitCost);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                batches.add(newBatch);
            }
            dataStore.saveBatches(batches);

            // 2. Update purchase order status
            List<PurchaseOrder> poList = dataStore.loadPurchaseOrders();
            for (int i = 0; i < poList.size(); i++) {
                if (poList.get(i).getPurchaseId().equalsIgnoreCase(po.getPurchaseId())) {
                    poList.set(i, po);
                    break;
                }
            }
            dataStore.savePurchaseOrders(poList);

            // 3. Record supplier payable for any unpaid amount
            BigDecimal unpaid = po.getUnpaidAmount();
            if (unpaid.compareTo(BigDecimal.ZERO) > 0 && payableService != null) {
                payableService.recordPayable(
                        po.getSupplierId(),
                        po.getPurchaseId(),
                        unpaid,
                        LocalDate.now().plusDays(30),
                        "Unpaid balance for purchase " + po.getPurchaseId()
                );
            }

            // 4. Record cash outflow if money was paid at intake
            if (po.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 && cashAccountService != null) {
                cashAccountService.recordOutflow(
                        po.getPaymentAccount(),
                        po.getPaidAmount(),
                        AccountTransaction.Type.SUPPLIER_PAYMENT,
                        po.getPurchaseId(),
                        "Payment for purchase " + po.getPurchaseId() + " (" + po.getSupplierId() + ")",
                        createdBy != null ? createdBy : "admin"
                );
            }
        });

        inventoryService.reload();
        reload();
        return po;
    }

    @Override
    public synchronized List<PurchaseOrder> getAllPurchases() {
        List<PurchaseOrder> copy = new ArrayList<>(purchases);
        copy.sort((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()));
        return Collections.unmodifiableList(copy);
    }

    @Override
    public synchronized PurchaseOrder getPurchase(String purchaseId) {
        if (purchaseId == null) return null;
        for (PurchaseOrder p : purchases) {
            if (p.getPurchaseId().equalsIgnoreCase(purchaseId.trim())) {
                return p;
            }
        }
        return null;
    }

    @Override
    public synchronized BigDecimal getTotalPurchaseValue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (PurchaseOrder p : purchases) {
            if (p.getStatus() == PurchaseOrder.Status.RECEIVED) {
                sum = sum.add(p.getTotalAmount());
            }
        }
        return CurrencyUtil.roundMoney(sum);
    }

    @Override
    public synchronized BigDecimal getTotalPurchasesBetween(LocalDate start, LocalDate end) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PurchaseOrder p : purchases) {
            if (p.getStatus() == PurchaseOrder.Status.RECEIVED && p.getReceivedDate() != null) {
                LocalDate d = p.getReceivedDate();
                if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                    sum = sum.add(p.getTotalAmount());
                }
            }
        }
        return CurrencyUtil.roundMoney(sum);
    }
}
