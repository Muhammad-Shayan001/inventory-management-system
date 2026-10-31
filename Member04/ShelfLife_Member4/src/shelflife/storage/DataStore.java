package shelflife.storage;

import shelflife.model.*;
import java.util.List;

public interface DataStore {
    void loadAll() throws StorageException;

    List<Product> getProducts();
    List<ProductBatch> getBatches();
    List<Sale> getSales();
    List<SaleItem> getSaleItems();
    List<WastageRecord> getWastageRecords();

    void saveProducts(List<Product> products) throws StorageException;
    void saveBatches(List<ProductBatch> batches) throws StorageException;
    void saveSales(List<Sale> sales) throws StorageException;
    void saveSaleItems(List<SaleItem> saleItems) throws StorageException;
    void saveWastageRecords(List<WastageRecord> wastageRecords) throws StorageException;

    void commitSale(Sale sale, List<SaleItem> items, List<ProductBatch> updatedBatches) throws StorageException;
    void commitWastage(WastageRecord wastage, List<ProductBatch> updatedBatches) throws StorageException;
}
