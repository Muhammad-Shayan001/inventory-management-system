package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.model.WastageRecord;

import java.util.List;

/**
 * Shared storage contract for ShelfLife services.
 * Defines shared local storage and transactional recovery.
 * All modules must use this interface - no direct CSV file access from GUI handlers.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public interface DataStore {
    List<Product> loadProducts() throws Exception;
    void saveProducts(List<Product> products) throws Exception;

    List<ProductBatch> loadBatches() throws Exception;
    void saveBatches(List<ProductBatch> batches) throws Exception;

    List<Sale> loadSales() throws Exception;
    void saveSales(List<Sale> sales) throws Exception;

    List<SaleItem> loadSaleItems() throws Exception;
    void saveSaleItems(List<SaleItem> saleItems) throws Exception;

    List<WastageRecord> loadWastageRecords() throws Exception;
    void saveWastageRecords(List<WastageRecord> wastageRecords) throws Exception;

    /**
     * Executes a transactional block atomically.
     * If operation throws an exception, all storage state is restored
     * to prevent inconsistent or corrupted records.
     */
    void executeTransaction(TransactionOperation operation) throws Exception;
}