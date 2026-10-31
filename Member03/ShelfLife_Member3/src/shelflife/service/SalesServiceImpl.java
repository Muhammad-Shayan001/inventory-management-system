package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;
import shelflife.storage.StorageException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Robust implementation of SalesService for ShelfLife (Member 3).
 *
 * Responsibilities:
 * 1. Final sellable stock validation at confirmation time.
 * 2. Exclusion of expired batches (a batch expiring today is sellable).
 * 3. FEFO (First-Expiring, First-Out) batch allocation across eligible batches.
 * 4. Creation of individual SaleItem records per batch allocation.
 * 5. Immutable snapshot of unit prices charged.
 * 6. High-precision financial arithmetic with BigDecimal.
 * 7. Transactional commit through DataStore with rollback protection on failure.
 */
public class SalesServiceImpl implements SalesService {
    private final InventoryService inventoryService;
    private final DataStore dataStore;
    private final List<Sale> salesCache = new ArrayList<>();
    private final Map<String, List<SaleItem>> saleItemsMap = new HashMap<>();

    private static final DateTimeFormatter ID_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger COUNTER = new AtomicInteger(1001);

    public SalesServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore cannot be null");
        reload();
    }

    @Override
    public synchronized Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod) throws SaleException {
        String methodStr = paymentMethod != null ? paymentMethod.getDisplayName() : PaymentMethod.CASH.getDisplayName();
        return commitSale(productQuantities, methodStr);
    }

    @Override
    public synchronized Sale commitSale(Map<String, Integer> productQuantities, String paymentMethod) throws SaleException {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new SaleException("Cannot commit sale: Temporary cart is empty.");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new SaleException("Cannot commit sale: Payment method must be specified.");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime saleTime = LocalDateTime.now();
        String saleId = generateSaleId(saleTime);

        Sale sale = new Sale(saleId, saleTime, BigDecimal.ZERO, paymentMethod.trim(), PaymentStatus.PAID.name());
        List<SaleItem> generatedItems = new ArrayList<>();
        List<ProductBatch> batchesToUpdate = new ArrayList<>();

        // 1. Final Stock Validation & FEFO Allocation Plan
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productId = entry.getKey();
            int requestedQty = entry.getValue() != null ? entry.getValue() : 0;

            if (requestedQty <= 0) {
                throw new SaleException("Invalid quantity " + requestedQty + " requested for product ID: " + productId);
            }

            Product product = inventoryService.getProductById(productId);
            if (product == null) {
                throw new SaleException("Product not found in inventory catalog: " + productId);
            }

            // Retrieve all batches for this product
            List<ProductBatch> productBatches = inventoryService.getBatchesByProductId(productId);
            if (productBatches == null || productBatches.isEmpty()) {
                // Fallback check in all batches
                productBatches = new ArrayList<>();
                for (ProductBatch b : inventoryService.getAllBatches()) {
                    if (b.getProductId().equalsIgnoreCase(productId)) {
                        productBatches.add(b);
                    }
                }
            }

            // Filter out expired batches and batches with zero remaining quantity
            List<ProductBatch> eligibleBatches = new ArrayList<>();
            int totalSellableStock = 0;
            for (ProductBatch b : productBatches) {
                // Team rule: Expired when expiryDate.isBefore(today). Expiring today is NOT expired.
                if (!b.isExpired(today) && b.getQuantity() > 0) {
                    eligibleBatches.add(b);
                    totalSellableStock += b.getQuantity();
                }
            }

            // Re-validate stock at confirmation time
            if (totalSellableStock < requestedQty) {
                throw new SaleException("Insufficient sellable stock for '" + product.getName() + "' (ID: " + productId + "). " +
                        "Requested: " + requestedQty + ", Available (non-expired): " + totalSellableStock);
            }

            // Sort eligible batches by FEFO:
            // 1st Priority: Expiry Date ascending (earliest expiry first)
            // 2nd Priority (Tie-breaker): Batch ID ascending for deterministic stable ordering
            Collections.sort(eligibleBatches, new Comparator<ProductBatch>() {
                @Override
                public int compare(ProductBatch b1, ProductBatch b2) {
                    int dateComp = b1.getExpiryDate().compareTo(b2.getExpiryDate());
                    if (dateComp != 0) {
                        return dateComp;
                    }
                    return b1.getBatchId().compareTo(b2.getBatchId());
                }
            });

            // Allocate requested quantity across sorted batches
            int remainingToAllocate = requestedQty;
            for (ProductBatch batch : eligibleBatches) {
                if (remainingToAllocate <= 0) {
                    break;
                }
                int take = Math.min(batch.getQuantity(), remainingToAllocate);

                // Clone/record staged deduction for this batch
                ProductBatch updatedBatch = new ProductBatch(
                        batch.getBatchId(),
                        batch.getProductId(),
                        batch.getQuantity() - take,
                        batch.getExpiryDate()
                );
                batchesToUpdate.add(updatedBatch);

                // Create a distinct SaleItem record identifying this specific batch allocation
                // Store the actual unit price charged at time of sale
                BigDecimal unitPrice = product.getSellingPrice();
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(take)).setScale(2, RoundingMode.HALF_UP);

                SaleItem item = new SaleItem(
                        saleId,
                        product.getProductId(),
                        product.getName(),
                        batch.getBatchId(),
                        take,
                        unitPrice
                );
                item.setLineTotal(lineTotal);
                generatedItems.add(item);
                sale.addItem(item);

                remainingToAllocate -= take;
            }

            if (remainingToAllocate > 0) {
                throw new SaleException("FEFO allocation could not satisfy requested quantity for " + product.getName());
            }
        }

        // 2. Finalize financial totals using exact BigDecimal arithmetic
        sale.recalculateTotals();

        // 3. Atomically commit changes to persistent storage
        try {
            dataStore.commitSale(sale, generatedItems, batchesToUpdate);
        } catch (StorageException ex) {
            // Roll back any in-memory state and refresh inventory from persistent store
            inventoryService.reload();
            throw new SaleException("Failed to commit sale to persistent storage: " + ex.getMessage(), ex);
        }

        // 4. Update in-memory state and reload inventory cache
        inventoryService.reload();
        salesCache.add(sale);
        saleItemsMap.put(saleId, generatedItems);

        return sale;
    }

    @Override
    public synchronized List<Sale> getAllSales() {
        return Collections.unmodifiableList(new ArrayList<>(salesCache));
    }

    @Override
    public synchronized Sale getSaleById(String saleId) {
        if (saleId == null) return null;
        for (Sale s : salesCache) {
            if (s.getSaleId().equalsIgnoreCase(saleId.trim())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public synchronized List<SaleItem> getSaleItemsBySaleId(String saleId) {
        if (saleId == null) return Collections.emptyList();
        List<SaleItem> items = saleItemsMap.get(saleId.trim().toUpperCase());
        if (items != null) {
            return Collections.unmodifiableList(items);
        }
        for (Sale s : salesCache) {
            if (s.getSaleId().equalsIgnoreCase(saleId.trim())) {
                return s.getItems();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public synchronized void reload() {
        salesCache.clear();
        saleItemsMap.clear();

        try {
            dataStore.loadAll();
            List<Sale> storedSales = dataStore.getAllSales();
            List<SaleItem> storedItems = dataStore.getAllSaleItems();

            // Index items by saleId
            Map<String, List<SaleItem>> itemsBySale = new HashMap<>();
            if (storedItems != null) {
                for (SaleItem item : storedItems) {
                    String sid = item.getSaleId();
                    if (sid != null) {
                        itemsBySale.computeIfAbsent(sid.trim().toUpperCase(), k -> new ArrayList<>()).add(item);
                    }
                }
            }

            if (storedSales != null) {
                for (Sale s : storedSales) {
                    List<SaleItem> items = itemsBySale.get(s.getSaleId().trim().toUpperCase());
                    if (items != null) {
                        s.setItems(items);
                        saleItemsMap.put(s.getSaleId().trim().toUpperCase(), items);
                    }
                    salesCache.add(s);
                }
            }
        } catch (Exception ignored) {
            // Fallback for tests or initial empty state
        }
    }

    private synchronized String generateSaleId(LocalDateTime timestamp) {
        String datePrefix = timestamp.format(ID_DATE_FMT);
        return "SALE-" + datePrefix + "-" + COUNTER.getAndIncrement();
    }
}
