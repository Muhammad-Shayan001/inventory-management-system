package shelflife.service;

import shelflife.model.PurchaseOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service contract for Purchase Orders, Goods Receiving, and Supplier Invoicing.
 *
 * Rules:
 * 1. PO creation does NOT increase inventory or cash.
 * 2. Only receiveGoods() creates physical batches and updates inventory.
 * 3. Handles partial payments, records supplier payables, and allocates freight/landed costs.
 */
public interface PurchaseService {

    PurchaseOrder createPurchaseOrder(String supplierId, String supplierInvoiceNo,
                                      String productId, String batchId, int quantity,
                                      BigDecimal unitCost, BigDecimal additionalCost,
                                      BigDecimal paidAmount, String paymentMethod,
                                      String paymentAccount, LocalDate expiryDate,
                                      String createdBy, String notes) throws Exception;

    PurchaseOrder receiveGoods(String purchaseId, String createdBy) throws Exception;

    List<PurchaseOrder> getAllPurchases();

    PurchaseOrder getPurchase(String purchaseId);

    BigDecimal getTotalPurchaseValue();

    BigDecimal getTotalPurchasesBetween(LocalDate start, LocalDate end);

    void reload();
}
