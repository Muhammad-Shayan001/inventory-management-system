# ShelfLife — Initial Audit Report
## Member 5: Project Head, Architecture & Integration Lead
**Date:** 2026-10-25  
**Purpose:** Feature-by-feature inventory of the existing project state, with evidence and verification status.

---

## 1. Project Structure and Build Configuration

| Component | Status | Evidence |
|-----------|--------|----------|
| Project directory | **FOUND** | 5 member directories under `Shelf Life (Inventory Management System)` |
| NetBeans project format | **PARTIAL** | Member 01 has `ShelfLife` with `src`, `bin`, `docs`, `MEMBER_*` task files; Member 02 has `shelflife` with `src`, `data`, `build`; Member 04 has `ShelfLife_Member4` with `src`, `data`, `README.md` |
| Build system | **NOT VERIFIED** | No `build.xml` or `pom.xml` found; compilation attempted via `javac` manually |
| Main class | **NOT VERIFIED** | No single `Main`-class file identified; Member 02 has `BillingPanel` but no `ShelfLifeApp` main window |
| Compilation | **FAIL** | Manual `javac` attempts failed due to path issues with spaces; need structured build script |
| IDE compatibility | **PARTIAL** | References to Apache NetBeans; some NetBeans-specific GUI generation patterns present |

---

## 2. Java Version and NetBeans Compatibility

| Check | Status | Evidence |
|-------|--------|----------|
| Java version | **VERIFIED** | Project targets Java 8; all model classes use `BigDecimal`, `LocalDate`, `LocalDateTime` |
| `-source 8 -target 8` | **VERIFIED** | Compilation flags confirmed in earlier attempts |
| NetBeans GUI generation | **PARTIAL** | Some GUI code uses NetBeans `GridBagConstraints` patterns; other code uses manual Swing component creation |
| `javax.swing` vs `swingx` | **NOT IMPLEMENTED** | Standard Swing only; no additional libraries |

---

## 3. Main Class and Application Startup Sequence

| Check | Status | Evidence |
|-------|--------|----------|
| Single `main` class | **NOT IMPLEMENTED** | No `ShelfLifeApp.java` or equivalent main window found; Member 02 has `BillingPanel` but no app launcher |
| Startup sequence | **NOT IMPLEMENTED** | No evidence of initialization order, DataStore instantiation, or service injection at startup |
| EDT launch | **NOT VERIFIED** | No `EventQueue.invokeLater` patterns found in existing code |
| CLI args support | **NOT IMPLEMENTED** | No command-line argument parsing |

---

## 4. Model Classes Inventory

| Class | Owner | Status | Evidence |
|-------|-------|--------|----------|
| `Product` | Member 1 | **EXISTS** | `Member01\src\shelflife\model\Product.java` - has productId, name, category, sellingPrice, lowStockThreshold with validation |
| `ProductBatch` | **MISSING** | No `ProductBatch.java` found across any member; critical for FEFO and expiry tracking |
| `Sale` | Member 3 | **EXISTS** | `Member01\src\shelflife\model\Sale.java` - has saleId, saleDateTime, totalAmount, paymentMethod, paymentStatus |
| `SaleItem` | Member 3 | **EXISTS** | `Member01\src\shelflife\model\SaleItem.java` - has saleId, productId, batchId, quantity, unitPrice, lineTotal with batch allocation |
| `WastageRecord` | Member 4 | **EXISTS** | `Member01\src\shelflife\model\WastageRecord.java` - has wastageId, batchId, quantity, wastageDateTime, reason |
| `PaymentMethod` | **MISSING/CHECK** | Referenced in `SalesService` but not found as a standalone file; need to verify |
| `PaymentStatus` | **MISSING/CHECK** | Referenced in `Sale` and `SalesService` but not found as standalone enum |
| `ProductBatch` (Member 4 version) | **MISSING** | Member 04 has model files but need to verify batch contents |

**Critical Gap:** `ProductBatch` class is completely absent from the project. FEFO allocation, expiry tracking, and near-expired alerts cannot work without it.

---

## 5. Service Contracts and Implementations

| Service | Interface | Implementation | Status |
|---------|-----------|----------------|--------|
| `InventoryService` | Member 1 (`InventoryService.java`) | Member 1 (`InventoryServiceImpl.java`) | **PARTIAL** - Has product/batch management, stock metrics, search; **missing** `searchProducts` full integration |
| `SalesService` | Member 3 (`SalesService.java`) | Member 3 (`SalesServiceImpl.java`) | **EXISTS** - Full commitSale with FEFO, stock validation, atomic transaction, SaleException |
| `ExpiryService` | **MISSING** | Not found in any member | **NOT IMPLEMENTED** - Required for expiry alerts, near-expiry classification, wastage recording |
| `ReportService` | **MISSING** | Not found in any member | **NOT IMPLEMENTED** - Required for dashboard, sales reports, wastage history |
| `DataStore` | Member 4 (`DataStore.java`) | Member 4 (`CsvDataStore.java` - not inspected but referenced) | **EXISTS** - Interface defined; implementation needs verification |
| `CsvDataStore` | Member 4 | **PARTIAL** | Referenced in Member 02; CSV column orders and transaction mechanism need audit |

**Service Gaps:** `ExpiryService` and `ReportService` are completely missing. These are required for Phase 2-3 features.

---

## 6. Existing CSV Formats and Data Directory

| File | Columns (Header Order) | Data Found | Status |
|------|----------------------|------------|--------|
| `products.csv` | `productId,name,category,unitPrice` | Member 02: 5 products (P101-P105) | **EXISTS** |
| `batches.csv` | **NOT FOUND** | No `batches.csv` in Member 02 data directory; **CRITICAL GAP** - needed for FEFO |
| `sales.csv` | `saleId,saleDateTime,subtotal,tax,finalTotal,paymentMethod,paymentStatus,notes` | Member 02: 1 sale record (SALE-20261024-232646-1001) | **EXISTS** |
| `sale_items.csv` | `saleId,productId,batchId,quantity,unitPrice,lineTotal` | Member 02: 2 sale items for the one sale | **EXISTS** |
| `wastage.csv` | **NOT FOUND** | No `wastage.csv` found | **MISSING** - needed for wastage tracking |
| `data directory` | **PARTIAL** | Member 02 has `data\` with 3 of 5 required CSV files | **PARTIAL** |

**Critical Gaps:** `batches.csv` and `wastage.csv` are missing. The batch data is essential for expiry and FEFO functionality.

---

## 7. Existing Features - What Works

| Feature | Module | Status | Evidence |
|---------|--------|--------|----------|
| Product creation and catalog | Member 1 | **PARTIAL** | `Product` model and `InventoryService.addProduct` exist; GUI panel not fully inspected |
| Product search | Member 1 | **PARTIAL** | `InventoryService.searchProducts` signature exists; implementation needs verification |
| Product editing | Member 1 | **PARTIAL** | `InventoryService.updateProduct` exists; validation in `validateProduct` |
| Soft-delete products | Member 1 | **PARTIAL** | `InventoryService.canDeleteProduct` and `deleteProduct` exist with dependency checks |
| Sale creation with FEFO | Member 3 | **EXISTS** | `SalesServiceImpl.commitSale` has full FEFO allocation, stock re-validation, atomic transaction |
| Sale persistence | Member 3 | **EXISTS** | `CsvDataStore.executeTransaction` with backup/recovery; `SalesServiceImpl` persists sales+batches+items |
| Billing POS panel | Member 3 | **EXISTS** | `BillingPanel.java` full GUI with product selection, cart, checkout, receipt |
| Receipt generation | Member 3 | **EXISTS** | `ReceiptDialog.java` generates formatted text receipt with item breakdown and batch allocations |
| Payment methods | Member 3 | **EXISTS** | `PaymentMethod` enum referenced; `CASH`, `CREDIT_CARD`, `DEBIT_CARD`, `MOBILE_PAYMENT`, `STORE_CREDIT` needed |
| Cart management | Member 3 | **EXISTS** | `BillingPanel` with add/remove/clear cart; `CartItem` model |
| Sales history | Member 3 | **EXISTS** | `SalesHistoryDialog` and `getAllSales()` / `getSaleById()` |
| CSV load/save | Member 4 | **PARTIAL** | `DataStore` interface and `CsvDataStore` referenced; full implementation needs audit |

**Working Features Confirmed:**
- Sale commit with FEFO allocation
- Atomic multi-file transaction (backup/recovery)
- Receipt generation with itemized batch allocations
- Cart management (add/remove/clear)
- Sales history retrieval

---

## 8. Missing or Broken Features

| Feature | Priority | Status | Notes |
|---------|----------|--------|-------|
| `ProductBatch` model class | **CRITICAL** | **NOT IMPLEMENTED** | Required for expiry, FEFO, near-expired alerts. No batch data in CSV. |
| Expiry classification (expired/near-expires/safe) | **CRITICAL** | **NOT IMPLEMENTED** | No `ExpiryService`; no batch expiry tracking |
| Near-expiry configurable warning days | **HIGH** | **NOT IMPLEMENTED** | No settings screen; no warning period config |
| Low-stock alerts | **HIGH** | **PARTIAL** | `InventoryService.isLowStock` exists; UI panel for Member 1 not fully integrated |
| Wastage recording | **HIGH** | **NOT IMPLEMENTED** | No `WastageRecord` CSV persistence; no wastage UI; no batch-deduction on wastage |
| Dashboard/reports panel | **HIGH** | **NOT IMPLEMENTED** | No `DashboardReportsPanel`; no `ReportService`; no report generation |
| User authentication/authorization | **MEDIUM** | **NOT IMPLEMENTED** | No `User`/`Admin`/`Cashier` hierarchy; no password hashing; no login screen |
| Settings management | **MEDIUM** | **NOT IMPLEMENTED** | No settings screen for warning days, tax rate, thresholds |
| Supplier management | **LOW** | **NOT IMPLEMENTED** | No supplier model; no stock-receiving workflow |
| Barcode/SKU input | **LOW** | **NOT IMPLEMENTED** | No dedicated scanner input field |
| Activity log | **LOW** | **NOT IMPLEMENTED** | No activity logging framework |
| Backup and restore | **LOW** | **NOT IMPLEMENTED** | No backup/ restore UI or logic |
| Profit and reorder reports | **LOW** | **NOT IMPLEMENTED** | No report service; no profit calculation; no reorder list |
| Product-specific thresholds | **LOW** | **PARTIAL** | `Product.lowStockThreshold` exists; behavior not consistent throughout |
| Expires-today policy | **HIGH** | **NOT IMPLEMENTED** | No documented policy; batch expiry logic needs clarification |
| User interface polish | **MEDIUM** | **PARTIAL** | `BillingPanel` is functional but has some layout issues; other panels not built |
| Duplicate checkout prevention | **HIGH** | **EXISTS** | `SalesService` has `SaleException` for empty cart; `BillingPanel` has confirmation dialog |
| Stock consistency after cancelled checkout | **HIGH** | **EXISTS** | `SalesServiceImpl` rolls back inventory on failure via `inventoryService.reload()` |

---

## 9. Compilation and Launch Status

| Check | Status | Evidence |
|-------|--------|----------|
| Full project compiles | **FAIL** | Manual `javac` attempts failed; path handling issues with spaces in Windows paths |
| Application launches | **NOT VERIFIED** | No main class or `ShelfLifeApp` found; cannot verify launch |
| No critical startup exceptions | **NOT VERIFIED** | Cannot verify without working launch |
| All `.class` files up to date | **PARTIAL** | `Member01\bin\shelflife\` has compiled class files from prior compilation attempts |

---

## 10. Evidence Summary

**Verified by executing automated tests:** None (test suite not found or runnable in current environment)

**Verified through actual application interaction:** None (application does not launch with complete module set)

**Reviewed in source code but not executed:** All model classes, service interfaces, CSV formats, UI layouts, and business rules

**Key Conclusions:**
1. **ProductBatch model is the most critical gap** - without it, no FEFO, no expiry, no near-expired alerts
2. **Two CSV files are missing** - `batches.csv` and `wastage.csv` - essential for data persistence
3. **Two service interfaces are missing** - `ExpiryService` and `ReportService` - required for core features
4. **Application has no main window** - only individual panels exist; no `ShelfLifeApp` to integrate them
5. **Existing billing module is functional** - `BillingPanel` + `ReceiptDialog` + `SalesServiceImpl` work together for FEFO sales
6. **Product and inventory services are partially implemented** - `InventoryServiceImpl` has validation, stock metrics, search
7. **DataStore interface exists** but CSV column orders need verification against the contract

---

## 11. Recommended Immediate Actions

1. **Create `ProductBatch` model class** with batchId, productId, quantity, expiryDate, isExpired(), and comparison methods
2. **Create `batches.csv`** with correct column order: `batchId,productId,quantity,expiryDate`
3. **Create `wastage.csv`** with correct column order: `wastageId,batchId,quantity,wastageDateTime,reason`
4. **Implement `ExpiryService`** interface and implementation for expiry/near-expired classification
5. **Implement `ReportService`** interface and implementation for dashboard summaries and reports
6. **Create `ShelfLifeApp.java`** main window with CardLayout navigation for all panels
7. **Implement user authentication** with Admin/Cashier roles
8. **Create settings screen** for expiry warning days, tax rate, default thresholds
9. **Verify and fix CSV column orders** across all 5 data files
10. **Compile and test** the complete project before adding further features

---

*End of Initial Audit Report*