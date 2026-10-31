package shelflife.storage;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.SaleItem;

import java.util.List;

/**
 * Shared storage contract managed by Member 5.
 * Defines shared local storage and transactional recovery.
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

    /**
     * Executes a transactional block atomically.
     * If operation throws an exception, all storage state is restored
     * to prevent inconsistent or corrupted records.
     */
    void executeTransaction(TransactionOperation operation) throws Exception;
}
