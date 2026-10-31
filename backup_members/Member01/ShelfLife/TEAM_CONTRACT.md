# ShelfLife — Unified Team Contract & Architectural Specification

**Version:** 1.0 (Coordinated Draft for Members 1, 2, 3, 4, 5)  
**Date:** 2026-10-22  
**Target Environment:** Java 8+ / Apache NetBeans / Java Swing / Local UTF-8 CSV Persistence  

---

## 1. Architectural Overview & Boundaries

ShelfLife is a layered Java desktop system for food businesses tracking inventory by batch using First-Expiring, First-Out (FEFO), with billing integration and food waste tracking.

```
       [ Member 1: UI ]      [ Member 2: UI ]      [ Member 3: UI ]      [ Member 4: UI ]
   ProductInventoryPanel    ExpiryAlertsPanel        BillingPanel      DashboardReportsPanel
             │                      │                      │                     │
             ▼                      ▼                      ▼                     ▼
     [InventoryService]     [ExpiryService]         [SalesService]        [ReportService]
             │                      │                      │                     │
             └──────────────────────┴──────────┬───────────┴─────────────────────┘
                                               ▼
                                      [DataStore Interface]
                                               │
                                               ▼
                              [FileManager / CSV Persistence]
                               (Managed by Member 4 & 5)
```

---

## 2. Shared File Storage Formats (UTF-8 CSV)

Per `MEMBER_4_TASKS.txt`, file storage uses atomic writes, transaction journal, and standard UTF-8 CSV encoding. Date format is strictly `yyyy-MM-dd`; timestamps use ISO-8601 `yyyy-MM-ddTHH:mm:ss`.

| File Name | Exact Columns & Header Order | Managed By |
| :--- | :--- | :--- |
| `products.csv` | `productId,name,category,sellingPrice,lowStockThreshold` | Member 1 / Member 4 |
| `batches.csv` | `batchId,productId,quantity,expiryDate` | Member 1 / Member 4 |
| `sales.csv` | `saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus` | Member 3 / Member 4 |
| `sale_items.csv` | `saleId,productId,batchId,quantity,unitPrice,lineTotal` | Member 3 / Member 4 |
| `wastage.csv` | `wastageId,batchId,quantity,wastageDateTime,reason` | Member 2 & 4 / Member 4 |

---

## 3. Shared Model Classes (`shelflife.model`)

### 3.1 `Product.java` (Owned by Member 1)
* **Fields:**
  * `private String productId;`
  * `private String name;`
  * `private String category;`
  * `private BigDecimal sellingPrice;`
  * `private int lowStockThreshold;`
* **Constructor:**
  `public Product(String productId, String name, String category, BigDecimal sellingPrice, int lowStockThreshold)`
* **Rules:** `productId`, `name`, `category` must not be blank; `sellingPrice > 0`; `lowStockThreshold >= 0`.

### 3.2 `ProductBatch.java` (Owned by Member 1)
* **Fields:**
  * `private String batchId;`
  * `private String productId;`
  * `private int quantity;`
  * `private LocalDate expiryDate;`
* **Constructor:**
  `public ProductBatch(String batchId, String productId, int quantity, LocalDate expiryDate)`
* **Business Rule:**
  * `public boolean isExpired()`: `expiryDate.isBefore(LocalDate.now())`. A batch expiring today is **not** expired.
  * `quantity >= 0`.

### 3.3 `Sale.java` (Owned by Member 3)
* **Fields:** `saleId` (String), `saleDateTime` (LocalDateTime), `totalAmount` (BigDecimal), `paymentMethod` (String), `paymentStatus` (String).

### 3.4 `SaleItem.java` (Owned by Member 3)
* **Fields:** `saleId` (String), `productId` (String), `batchId` (String), `quantity` (int), `unitPrice` (BigDecimal), `lineTotal` (BigDecimal).

### 3.5 `WastageRecord.java` (Owned by Member 4)
* **Fields:** `wastageId` (String), `batchId` (String), `quantity` (int), `wastageDateTime` (LocalDateTime), `reason` (String).

---

## 4. Shared Storage Contract (`shelflife.storage.DataStore`)

Implemented by Member 4 (`shelflife.storage.FileManager` / `CsvDataStore`):

```java
package shelflife.storage;

import shelflife.model.*;
import java.util.List;

public interface DataStore {
    void loadAll();
    void saveAll();

    // Products
    List<Product> getAllProducts();
    Product getProductById(String productId);
    void addProduct(Product product);
    void updateProduct(Product product);
    void deleteProduct(String productId);

    // Batches
    List<ProductBatch> getAllBatches();
    List<ProductBatch> getBatchesByProductId(String productId);
    ProductBatch getBatchById(String batchId);
    void addBatch(ProductBatch batch);
    void updateBatch(ProductBatch batch);
    void deleteBatch(String batchId);

    // Historical records (used for validation & dependency checks)
    List<Sale> getAllSales();
    List<SaleItem> getAllSaleItems();
    List<WastageRecord> getAllWastageRecords();
}
```

---

## 5. Shared Service Contracts (`shelflife.service`)

### 5.1 `InventoryService.java` (Used by Members 1, 2, 3)

```java
package shelflife.service;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import java.util.List;

public interface InventoryService {
    // Product operations
    List<Product> getAllProducts();
    Product getProductById(String productId);
    List<Product> searchProducts(String query);
    void addProduct(Product product);
    void updateProduct(Product product);
    boolean canDeleteProduct(String productId);
    void deleteProduct(String productId);

    // Batch operations
    List<ProductBatch> getAllBatches();
    List<ProductBatch> getBatchesByProductId(String productId);
    ProductBatch getBatchById(String batchId);
    void addBatch(ProductBatch batch);
    void updateBatch(ProductBatch batch);
    void deleteBatch(String batchId);

    // Stock calculations
    int getTotalStock(String productId);
    boolean isLowStock(String productId);
}
```

---

## 6. UI Integration Standards

1. **Constructors**:
   * Member 1: `public ProductInventoryPanel(InventoryService inventoryService)`
   * Member 2: `public ExpiryAlertsPanel(InventoryService inventoryService, ExpiryService expiryService)`
   * Member 3: `public BillingPanel(InventoryService inventoryService, SalesService salesService)`
   * Member 4: `public DashboardReportsPanel(ReportService reportService)`
2. **Refresh Contract**:
   * Every panel implements `public void refreshData()` to allow Member 5's main application window to trigger synchronization after tab switches or state updates.
3. **No Direct File Access**:
   * Swing event handlers must **never** read or write CSV files directly. All persistence is delegated to services and `DataStore`.
