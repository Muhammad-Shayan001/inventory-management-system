package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Supplier;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of SupplierService handling vendor metadata and stock receiving operations.
 */
public class SupplierServiceImpl implements SupplierService {

    private final InventoryService inventoryService;
    private final DataStore dataStore;
    private final List<Supplier> supplierCache = new CopyOnWriteArrayList<>();

    public SupplierServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        if (inventoryService == null) {
            throw new IllegalArgumentException("InventoryService cannot be null.");
        }
        if (dataStore == null) {
            throw new IllegalArgumentException("DataStore cannot be null.");
        }
        this.inventoryService = inventoryService;
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            supplierCache.clear();
            List<Supplier> loaded = dataStore.loadSuppliers();
            if (loaded != null) {
                supplierCache.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Error loading suppliers: " + e.getMessage());
        }
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return Collections.unmodifiableList(new ArrayList<>(supplierCache));
    }

    @Override
    public List<Supplier> getActiveSuppliers() {
        List<Supplier> list = new ArrayList<>();
        for (Supplier s : supplierCache) {
            if (s.isActive()) {
                list.add(s);
            }
        }
        return list;
    }

    @Override
    public Supplier getSupplierById(String supplierId) {
        if (supplierId == null) return null;
        for (Supplier s : supplierCache) {
            if (s.getSupplierId().equalsIgnoreCase(supplierId.trim())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public synchronized void addSupplier(Supplier supplier) throws Exception {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null.");
        }
        for (Supplier s : supplierCache) {
            if (s.getSupplierId().equalsIgnoreCase(supplier.getSupplierId())) {
                throw new IllegalArgumentException("Supplier ID already exists: " + supplier.getSupplierId());
            }
        }
        supplierCache.add(supplier);
        dataStore.saveSuppliers(new ArrayList<>(supplierCache));
    }

    @Override
    public synchronized void updateSupplier(Supplier supplier) throws Exception {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null.");
        }
        for (int i = 0; i < supplierCache.size(); i++) {
            if (supplierCache.get(i).getSupplierId().equalsIgnoreCase(supplier.getSupplierId())) {
                supplierCache.set(i, supplier);
                dataStore.saveSuppliers(new ArrayList<>(supplierCache));
                return;
            }
        }
        throw new IllegalArgumentException("Supplier not found: " + supplier.getSupplierId());
    }

    @Override
    public synchronized void deactivateSupplier(String supplierId) throws Exception {
        Supplier s = getSupplierById(supplierId);
        if (s == null) {
            throw new IllegalArgumentException("Supplier not found: " + supplierId);
        }
        s.setActive(false);
        updateSupplier(s);
    }

    @Override
    public synchronized ProductBatch receiveStock(String productId, String batchId, int quantity, LocalDate expiryDate, BigDecimal costPrice, String supplierId) throws Exception {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be empty.");
        }
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Received quantity must be greater than zero.");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null.");
        }
        if (expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot receive already-expired stock (Expiry: " + expiryDate + ").");
        }

        Product product = inventoryService.getProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        String sid = (supplierId != null && !supplierId.trim().isEmpty()) ? supplierId.trim().toUpperCase() : "SUP-01";
        BigDecimal cost = costPrice != null ? costPrice : BigDecimal.ZERO;

        ProductBatch batch = new ProductBatch(batchId, productId, quantity, expiryDate, cost, sid, LocalDate.now());
        inventoryService.addBatch(batch);
        return batch;
    }

    @Override
    public java.util.Map<String, Object> getSupplierPerformance(String supplierId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        if (supplierId == null) return stats;

        Supplier s = getSupplierById(supplierId);
        stats.put("supplierId", supplierId);
        stats.put("supplierName", s != null ? s.getName() : supplierId);

        List<ProductBatch> batches = new ArrayList<>();
        Set<String> batchIds = new HashSet<>();
        Map<String, BigDecimal> batchCostMap = new HashMap<>();
        int totalUnitsReceived = 0;

        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getSupplierId() != null && b.getSupplierId().equalsIgnoreCase(supplierId.trim())) {
                batches.add(b);
                batchIds.add(b.getBatchId());
                batchCostMap.put(b.getBatchId(), b.getCostPrice());
                totalUnitsReceived += b.getQuantity();
            }
        }

        int totalUnitsSold = 0;
        try {
            for (shelflife.model.SaleItem item : dataStore.loadSaleItems()) {
                if (item.getBatchId() != null && batchIds.contains(item.getBatchId())) {
                    totalUnitsSold += item.getQuantity();
                }
            }
        } catch (Exception e) {}

        int totalUnitsWasted = 0;
        BigDecimal totalWastedCost = BigDecimal.ZERO;
        try {
            for (shelflife.model.WastageRecord w : dataStore.loadWastageRecords()) {
                if (batchIds.contains(w.getBatchId())) {
                    totalUnitsWasted += w.getQuantity();
                    BigDecimal cost = batchCostMap.getOrDefault(w.getBatchId(), BigDecimal.ZERO);
                    totalWastedCost = totalWastedCost.add(cost.multiply(BigDecimal.valueOf(w.getQuantity())));
                }
            }
        } catch (Exception e) {}

        int totalIntake = totalUnitsReceived + totalUnitsSold + totalUnitsWasted;
        double wastageRate = totalIntake > 0 ? ((double) totalUnitsWasted / totalIntake) * 100.0 : 0.0;

        stats.put("batchCount", batches.size());
        stats.put("totalIntakeUnits", totalIntake);
        stats.put("unitsSold", totalUnitsSold);
        stats.put("unitsWasted", totalUnitsWasted);
        stats.put("wastedCost", totalWastedCost);
        stats.put("wastageRatePercent", Math.round(wastageRate * 10.0) / 10.0);

        return stats;
    }

    @Override
    public List<java.util.Map<String, Object>> getAllSupplierPerformanceReports() {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        for (Supplier s : getAllSuppliers()) {
            list.add(getSupplierPerformance(s.getSupplierId()));
        }
        return list;
    }
}
