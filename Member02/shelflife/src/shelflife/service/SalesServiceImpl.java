package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standard implementation of SalesService for ShelfLife Member 3.
 * Responsible for stock re-validation, FEFO batch allocation, financial calculations,
 * and atomic transactional commits.
 */
public class SalesServiceImpl implements SalesService {
    private final InventoryService inventoryService;
    private final DataStore dataStore;
    private final List<Sale> salesCache = new ArrayList<>();
    private static final AtomicInteger SALE_COUNTER = new AtomicInteger(1001);

    public SalesServiceImpl(InventoryService inventoryService, DataStore dataStore) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore cannot be null");
        reload();
    }

    @Override
    public synchronized Sale commitSale(Map<String, Integer> productQuantities, PaymentMethod paymentMethod) throws SaleException {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new SaleException("Cannot commit sale: Cart is empty.");
        }
        if (paymentMethod == null) {
            throw new SaleException("Cannot commit sale: Payment method must be specified.");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime saleTime = LocalDateTime.now();
        String saleId = generateSaleId(saleTime);

        // Pre-commit validation and allocation planning
        Sale sale = new Sale(saleId, saleTime, paymentMethod, PaymentStatus.PAID);
        List<ProductBatch> allBatchesToUpdate = new ArrayList<>();
        List<SaleItem> generatedItems = new ArrayList<>();
        int itemIndex = 1;

        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productId = entry.getKey();
            int requestedQty = entry.getValue() != null ? entry.getValue() : 0;

            if (requestedQty <= 0) {
                throw new SaleException("Invalid quantity " + requestedQty + " requested for product ID: " + productId);
            }

            Product product = inventoryService.getProductById(productId);
            if (product == null) {
                throw new SaleException("Product not found in catalog: " + productId);
            }

            // Fetch batches and filter out expired batches
            List<ProductBatch> productBatches = inventoryService.getBatchesForProduct(productId);
            List<ProductBatch> eligibleBatches = new ArrayList<>();
            int totalSellable = 0;

            for (ProductBatch b : productBatches) {
                if (!b.isExpired(today) && b.getQuantity() > 0) {
                    eligibleBatches.add(b);
                    totalSellable += b.getQuantity();
                }
            }

            // Re-validate stock at confirmation time
            if (totalSellable < requestedQty) {
                throw new SaleException("Insufficient sellable stock for '" + product.getName() + "' (ID: " + productId + "). " +
                        "Requested: " + requestedQty + ", Available (non-expired): " + totalSellable);
            }

            // Sort eligible batches by expiry date ascending (FEFO: First-Expired, First-Out)
            // Secondary sort by batch ID for deterministic allocation order
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

                // Create a distinct SaleItem identifying the specific batch allocation
                String itemId = "SI-" + saleId + "-" + String.format("%03d", itemIndex++);
                SaleItem item = new SaleItem(
                        itemId,
                        saleId,
                        product.getProductId(),
                        product.getName(),
                        batch.getBatchId(),
                        take,
                        product.getUnitPrice() // Store actual unit price charged at time of sale
                );
                generatedItems.add(item);
                sale.addItem(item);

                // Stage batch deduction
                batch.deductQuantity(take);
                allBatchesToUpdate.add(batch);

                remainingToAllocate -= take;
            }

            if (remainingToAllocate > 0) {
                throw new SaleException("FEFO allocation could not satisfy requested quantity for: " + product.getName());
            }
        }

        // Calculate monetary totals using BigDecimal
        sale.recalculateTotals();

        // Atomically persist changes through shared DataStore transaction
        try {
            dataStore.executeTransaction(() -> {
                // 1. Persist updated batch quantities
                List<ProductBatch> allCurrentBatches = dataStore.loadBatches();
                for (ProductBatch updated : allBatchesToUpdate) {
                    for (ProductBatch current : allCurrentBatches) {
                        if (current.getBatchId().equals(updated.getBatchId())) {
                            current.setQuantity(updated.getQuantity());
                        }
                    }
                }
                dataStore.saveBatches(allCurrentBatches);

                // 2. Persist new sale items
                List<SaleItem> existingItems = dataStore.loadSaleItems();
                existingItems.addAll(generatedItems);
                dataStore.saveSaleItems(existingItems);

                // 3. Persist new sale header
                List<Sale> existingSales = dataStore.loadSales();
                existingSales.add(sale);
                dataStore.saveSales(existingSales);
            });
        } catch (Exception ex) {
            // Rollback in-memory inventory state so GUI does not show phantom deductions
            inventoryService.reload();
            throw new SaleException("Sales transaction failed during persistence: " + ex.getMessage(), ex);
        }

        // Transaction succeeded: refresh inventory and cache
        inventoryService.reload();
        salesCache.add(sale);
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
            if (saleId.equalsIgnoreCase(s.getSaleId())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public synchronized void reload() {
        salesCache.clear();
        try {
            List<Sale> loaded = dataStore.loadSales();
            if (loaded != null) {
                salesCache.addAll(loaded);
            }
        } catch (Exception e) {
            // If storage load fails during initial boot, cache remains empty
        }
    }

    private String generateSaleId(LocalDateTime timestamp) {
        String ts = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return "SALE-" + ts + "-" + SALE_COUNTER.getAndIncrement();
    }
}
