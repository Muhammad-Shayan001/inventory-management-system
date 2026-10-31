package shelflife.service;

import shelflife.model.ProductBatch;
import shelflife.model.WastageRecord;
import shelflife.storage.DataStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Standard implementation of ExpiryService.
 * Handles expiry status evaluation and atomic stock wastage recording.
 */
public class ExpiryServiceImpl implements ExpiryService {

    private final InventoryService inventoryService;
    private final DataStore dataStore;

    public ExpiryServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        if (inventoryService == null || dataStore == null) {
            throw new IllegalArgumentException("InventoryService and DataStore cannot be null.");
        }
        this.inventoryService = inventoryService;
        this.dataStore = dataStore;
    }

    @Override
    public List<ProductBatch> getExpiredBatches() {
        List<ProductBatch> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.isExpired(today)) {
                result.add(b);
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    @Override
    public List<ProductBatch> getNearExpiryBatches(int days) {
        List<ProductBatch> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(days);
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (!b.isExpired(today) && b.getQuantity() > 0) {
                LocalDate exp = b.getExpiryDate();
                if (!exp.isBefore(today) && !exp.isAfter(cutoff)) {
                    result.add(b);
                }
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    @Override
    public List<ProductBatch> getEligibleBatches(String productId) {
        List<ProductBatch> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ProductBatch b : inventoryService.getBatchesByProductId(productId)) {
            if (!b.isExpired(today) && b.getQuantity() > 0) {
                result.add(b);
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    @Override
    public void recordWastage(WastageRecord record) throws Exception {
        if (record == null) {
            throw new IllegalArgumentException("Wastage record cannot be null.");
        }

        String batchId = record.getBatchId();
        ProductBatch batch = inventoryService.getBatchById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("Cannot record wastage: Batch '" + batchId + "' not found.");
        }

        if (record.getQuantity() <= 0) {
            throw new IllegalArgumentException("Wastage quantity must be greater than zero.");
        }

        if (record.getQuantity() > batch.getQuantity()) {
            throw new IllegalArgumentException("Wastage quantity (" + record.getQuantity() + 
                    ") cannot exceed available batch quantity (" + batch.getQuantity() + ").");
        }

        if (record.getWastageDateTime() == null) {
            record.setWastageDateTime(LocalDateTime.now());
        }

        // Execute atomic write transaction: save updated batch and record wastage log
        dataStore.executeTransaction(() -> {
            int remaining = batch.getQuantity() - record.getQuantity();
            batch.setQuantity(remaining);

            List<ProductBatch> allBatches = inventoryService.getAllBatches();
            for (ProductBatch b : allBatches) {
                if (b.getBatchId().equalsIgnoreCase(batchId)) {
                    b.setQuantity(remaining);
                }
            }
            dataStore.saveBatches(allBatches);

            List<WastageRecord> wastageRecords = dataStore.loadWastageRecords();
            wastageRecords.add(record);
            dataStore.saveWastageRecords(wastageRecords);
        });

        inventoryService.reload();
    }

    @Override
    public List<WastageRecord> getWastageRecordsByBatchId(String batchId) {
        List<WastageRecord> result = new ArrayList<>();
        if (batchId == null) return result;
        for (WastageRecord r : getAllWastageRecords()) {
            if (r.getBatchId().equalsIgnoreCase(batchId.trim())) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<WastageRecord> getAllWastageRecords() {
        try {
            return dataStore.loadWastageRecords();
        } catch (Exception e) {
            System.err.println("Warning: Failed to load wastage records: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void recallBatch(String batchId, String reason, String adminUsername) throws Exception {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be empty.");
        }
        ProductBatch target = null;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getBatchId().equalsIgnoreCase(batchId.trim())) {
                target = b;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }

        final ProductBatch batchToRecall = target;
        dataStore.executeTransaction(() -> {
            List<ProductBatch> allBatches = dataStore.loadBatches();
            for (ProductBatch b : allBatches) {
                if (b.getBatchId().equalsIgnoreCase(batchToRecall.getBatchId())) {
                    b.setRecalled(true);
                    break;
                }
            }
            dataStore.saveBatches(allBatches);

            // Audit log
            List<shelflife.model.ActivityLogEntry> logs = dataStore.loadActivityLogs();
            String stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            logs.add(new shelflife.model.ActivityLogEntry("LOG-" + stamp, LocalDateTime.now(),
                    adminUsername != null ? adminUsername : "admin",
                    "BATCH_RECALL", "Recalled batch " + batchToRecall.getBatchId() + " (Reason: " + (reason != null ? reason : "None") + ")",
                    batchToRecall.getBatchId()));
            dataStore.saveActivityLogs(logs);
        });

        inventoryService.reload();
    }

    @Override
    public void unrecallBatch(String batchId, String adminUsername) throws Exception {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be empty.");
        }
        ProductBatch target = null;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getBatchId().equalsIgnoreCase(batchId.trim())) {
                target = b;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }

        final ProductBatch batchToUnrecall = target;
        dataStore.executeTransaction(() -> {
            List<ProductBatch> allBatches = dataStore.loadBatches();
            for (ProductBatch b : allBatches) {
                if (b.getBatchId().equalsIgnoreCase(batchToUnrecall.getBatchId())) {
                    b.setRecalled(false);
                    break;
                }
            }
            dataStore.saveBatches(allBatches);

            // Audit log
            List<shelflife.model.ActivityLogEntry> logs = dataStore.loadActivityLogs();
            String stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
            logs.add(new shelflife.model.ActivityLogEntry("LOG-" + stamp, LocalDateTime.now(),
                    adminUsername != null ? adminUsername : "admin",
                    "BATCH_UNRECALL", "Unrecalled batch " + batchToUnrecall.getBatchId() + " returned to sellable status",
                    batchToUnrecall.getBatchId()));
            dataStore.saveActivityLogs(logs);
        });

        inventoryService.reload();
    }

    @Override
    public int getSuggestedClearanceDiscount(ProductBatch batch) {
        if (batch == null || batch.isExpired() || batch.isRecalled()) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
        if (days <= 7) return 30;
        if (days <= 14) return 20;
        if (days <= 30) return 10;
        return 0;
    }

    @Override
    public String calculateBatchWasteRisk(ProductBatch batch) {
        if (batch == null || batch.getQuantity() <= 0) return "LOW";
        LocalDate today = LocalDate.now();
        if (batch.isExpired(today)) return "HIGH (Expired)";
        if (batch.isRecalled()) return "HIGH (Recalled)";

        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpiryDate());
        if (daysUntilExpiry <= 7) return "HIGH (Expires in " + daysUntilExpiry + " days)";

        double avgDailySales = calculateAverageDailySales(batch.getProductId(), 30);
        if (avgDailySales <= 0.001) {
            return "HIGH (Zero recent velocity; " + batch.getQuantity() + " units at risk)";
        }

        double daysToSell = batch.getQuantity() / avgDailySales;
        if (daysToSell > daysUntilExpiry * 1.5) {
            return "HIGH (Est. " + (int) Math.ceil(daysToSell) + "d to sell vs " + daysUntilExpiry + "d left)";
        } else if (daysToSell > daysUntilExpiry) {
            return "MEDIUM (Est. " + (int) Math.ceil(daysToSell) + "d to sell vs " + daysUntilExpiry + "d left)";
        } else {
            return "LOW (" + (int) Math.ceil(daysToSell) + "d to clear)";
        }
    }

    private double calculateAverageDailySales(String productId, int daysWindow) {
        if (productId == null || daysWindow <= 0) return 0.0;
        try {
            LocalDate cutoff = LocalDate.now().minusDays(daysWindow);
            List<shelflife.model.Sale> allSales = dataStore.loadSales();
            Set<String> matchingSaleIds = new HashSet<>();
            for (shelflife.model.Sale s : allSales) {
                if (s.getSaleDateTime().toLocalDate().isAfter(cutoff) || s.getSaleDateTime().toLocalDate().isEqual(cutoff)) {
                    matchingSaleIds.add(s.getSaleId());
                }
            }

            int soldUnits = 0;
            for (shelflife.model.SaleItem item : dataStore.loadSaleItems()) {
                if (item.getProductId().equalsIgnoreCase(productId) && matchingSaleIds.contains(item.getSaleId())) {
                    soldUnits += item.getQuantity();
                }
            }
            return (double) soldUnits / daysWindow;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getBatchTraceability(String batchId) {
        Map<String, Object> data = new HashMap<>();
        if (batchId == null) return data;

        ProductBatch batch = null;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getBatchId().equalsIgnoreCase(batchId.trim())) {
                batch = b;
                break;
            }
        }
        data.put("batch", batch);

        int soldUnits = 0;
        List<shelflife.model.SaleItem> items = new ArrayList<>();
        try {
            for (shelflife.model.SaleItem item : dataStore.loadSaleItems()) {
                if (item.getBatchId() != null && item.getBatchId().equalsIgnoreCase(batchId.trim())) {
                    items.add(item);
                    soldUnits += item.getQuantity();
                }
            }
        } catch (Exception e) {}
        data.put("sales", items);
        data.put("unitsSold", soldUnits);

        int wastedUnits = 0;
        List<WastageRecord> wastes = getWastageRecordsByBatchId(batchId);
        for (WastageRecord w : wastes) {
            wastedUnits += w.getQuantity();
        }
        data.put("wastage", wastes);
        data.put("unitsWasted", wastedUnits);

        // Supplier name
        String supplierName = "Unknown";
        if (batch != null && batch.getSupplierId() != null) {
            try {
                for (shelflife.model.Supplier s : dataStore.loadSuppliers()) {
                    if (s.getSupplierId().equalsIgnoreCase(batch.getSupplierId())) {
                        supplierName = s.getName();
                        break;
                    }
                }
            } catch (Exception e) {}
        }
        data.put("supplierName", supplierName);

        return data;
    }
}
