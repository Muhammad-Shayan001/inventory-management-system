package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Robust transactional implementation of customer returns and refund processing.
 */
public class ReturnsServiceImpl implements ReturnsService {

    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final ExpiryService expiryService;
    private final DataStore dataStore;

    private final List<SaleReturn> returnsCache = new CopyOnWriteArrayList<>();
    private final List<SaleReturnItem> returnItemsCache = new CopyOnWriteArrayList<>();

    public ReturnsServiceImpl(InventoryService inventoryService, SalesService salesService, ExpiryService expiryService, DataStore dataStore) {
        if (inventoryService == null) throw new IllegalArgumentException("InventoryService cannot be null.");
        if (salesService == null) throw new IllegalArgumentException("SalesService cannot be null.");
        if (expiryService == null) throw new IllegalArgumentException("ExpiryService cannot be null.");
        if (dataStore == null) throw new IllegalArgumentException("DataStore cannot be null.");

        this.inventoryService = inventoryService;
        this.salesService = salesService;
        this.expiryService = expiryService;
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            returnsCache.clear();
            returnItemsCache.clear();
            List<SaleReturn> loadedReturns = dataStore.loadReturns();
            if (loadedReturns != null) {
                returnsCache.addAll(loadedReturns);
            }
            List<SaleReturnItem> loadedItems = dataStore.loadReturnItems();
            if (loadedItems != null) {
                returnItemsCache.addAll(loadedItems);
            }
        } catch (Exception e) {
            System.err.println("Error loading returns data: " + e.getMessage());
        }
    }

    @Override
    public Sale getSaleForReturn(String saleId) throws Exception {
        if (saleId == null || saleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sale ID cannot be empty.");
        }
        Sale sale = salesService.getSaleById(saleId.trim().toUpperCase());
        if (sale == null) {
            throw new IllegalArgumentException("Sale not found: " + saleId);
        }
        return sale;
    }

    @Override
    public List<SaleReturn> getReturnsForSale(String saleId) {
        List<SaleReturn> list = new ArrayList<>();
        if (saleId == null) return list;
        for (SaleReturn r : returnsCache) {
            if (r.getOriginalSaleId().equalsIgnoreCase(saleId.trim())) {
                list.add(r);
            }
        }
        return list;
    }

    @Override
    public List<SaleReturn> getAllReturns() {
        return Collections.unmodifiableList(new ArrayList<>(returnsCache));
    }

    @Override
    public synchronized SaleReturn processReturn(String saleId, List<SaleReturnItem> itemsToReturn, String reason, String processedBy) throws Exception {
        if (itemsToReturn == null || itemsToReturn.isEmpty()) {
            throw new IllegalArgumentException("No items selected for return.");
        }

        Sale originalSale = getSaleForReturn(saleId);
        List<SaleItem> soldItems = originalSale.getItems();

        // Calculate already returned quantities per product and batch for this sale
        Map<String, Integer> alreadyReturnedPerItem = new HashMap<>();
        for (SaleReturn prevReturn : getReturnsForSale(saleId)) {
            for (SaleReturnItem prevItem : returnItemsCache) {
                if (prevItem.getReturnId().equalsIgnoreCase(prevReturn.getReturnId())) {
                    String key = prevItem.getProductId() + "#" + prevItem.getBatchId();
                    alreadyReturnedPerItem.put(key, alreadyReturnedPerItem.getOrDefault(key, 0) + prevItem.getQuantity());
                }
            }
        }

        // Validate that requested returns do not exceed original sold quantities
        BigDecimal totalRefund = BigDecimal.ZERO;
        String returnId = "RET-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + "-" + (int)(Math.random() * 9000 + 1000);
        List<SaleReturnItem> finalizedItems = new ArrayList<>();

        for (SaleReturnItem request : itemsToReturn) {
            if (request.getQuantity() <= 0) {
                throw new IllegalArgumentException("Return quantity must be greater than zero for " + request.getProductName());
            }

            String key = request.getProductId() + "#" + request.getBatchId();
            int previouslyReturned = alreadyReturnedPerItem.getOrDefault(key, 0);

            // Find matching original sale item
            int totalSold = 0;
            BigDecimal originalPrice = BigDecimal.ZERO;
            for (SaleItem si : soldItems) {
                if (si.getProductId().equalsIgnoreCase(request.getProductId()) && si.getBatchId().equalsIgnoreCase(request.getBatchId())) {
                    totalSold += si.getQuantity();
                    originalPrice = si.getUnitPrice();
                }
            }

            if (request.getQuantity() + previouslyReturned > totalSold) {
                throw new IllegalArgumentException(String.format(
                        "Cannot return %d units of '%s' (Batch: %s). Sold: %d, Already returned: %d, Max allowable: %d.",
                        request.getQuantity(), request.getProductName(), request.getBatchId(), totalSold, previouslyReturned, (totalSold - previouslyReturned)
                ));
            }

            BigDecimal lineRefund = originalPrice.multiply(BigDecimal.valueOf(request.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            totalRefund = totalRefund.add(lineRefund);

            String returnItemId = "RETI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            SaleReturnItem item = new SaleReturnItem(
                    returnItemId, returnId, request.getProductId(), request.getProductName(),
                    request.getBatchId(), request.getQuantity(), originalPrice, lineRefund, request.getDisposition()
            );
            finalizedItems.add(item);
        }

        SaleReturn saleReturn = new SaleReturn(returnId, saleId, LocalDateTime.now(), totalRefund, processedBy, reason);
        for (SaleReturnItem it : finalizedItems) {
            saleReturn.addItem(it);
        }

        // Transactionally execute inventory adjustment and persistence
        dataStore.executeTransaction(() -> {
            for (SaleReturnItem it : finalizedItems) {
                ProductBatch batch = inventoryService.getBatchById(it.getBatchId());
                if ("RESTOCK".equalsIgnoreCase(it.getDisposition())) {
                    if (batch != null && !batch.isExpired()) {
                        // Batch still valid: restock returned inventory
                        inventoryService.updateBatchQuantity(batch.getBatchId(), batch.getQuantity() + it.getQuantity());
                    } else {
                        // Batch is expired or not found: write off as wastage
                        String wid = "WST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        WastageRecord wr = new WastageRecord(wid, it.getBatchId(), it.getQuantity(), LocalDateTime.now(),
                                "Returned expired/damaged stock from sale " + saleId);
                        List<WastageRecord> records = new ArrayList<>(dataStore.loadWastageRecords());
                        records.add(wr);
                        dataStore.saveWastageRecords(records);
                    }
                } else {
                    // Damaged / spoiled write-off
                    String wid = "WST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    WastageRecord wr = new WastageRecord(wid, it.getBatchId(), it.getQuantity(), LocalDateTime.now(),
                            "Returned damaged stock from sale " + saleId + ": " + reason);
                    List<WastageRecord> records = new ArrayList<>(dataStore.loadWastageRecords());
                    records.add(wr);
                    dataStore.saveWastageRecords(records);
                }
            }

            List<SaleReturn> allReturns = new ArrayList<>(returnsCache);
            allReturns.add(saleReturn);
            dataStore.saveReturns(allReturns);

            List<SaleReturnItem> allItems = new ArrayList<>(returnItemsCache);
            allItems.addAll(finalizedItems);
            dataStore.saveReturnItems(allItems);
        });

        returnsCache.add(saleReturn);
        returnItemsCache.addAll(finalizedItems);

        return saleReturn;
    }
}
