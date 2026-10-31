# University Viva Examination Questions & Answers (`VIVA_QUESTIONS.md`)

This document prepares the student for viva examinations on the **ShelfLife** Java OOP project. It contains 35 anticipated questions covering OOP theory, system architecture, database design, transaction integrity, and Java runtime mechanics.

---

## Part 1: Core Object-Oriented Programming (OOP) Questions

### Q1: Why did you choose an interface (`DataStore`) instead of directly instantiating `CsvDataStore` in your services?
**Answer**: By defining the persistence layer as an interface (`DataStore`), we applied the **Dependency Inversion Principle (DIP)**. High-level services depend on an abstraction rather than a low-level concrete implementation. This decouples business logic from disk storage mechanics. If the supermarket later decides to switch from flat CSV files to an SQL database (such as MySQL or PostgreSQL), we only need to implement a new class (`SqlDataStore implements DataStore`) without rewriting or modifying a single line of business logic in `InventoryService`, `SalesService`, or the Swing UI.

### Q2: How is Encapsulation demonstrated in your entity models?
**Answer**: In [`Product.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/model/Product.java) and [`ProductBatch.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/model/ProductBatch.java), all internal fields are marked `private`. External classes cannot mutate state directly. Instead, they must invoke public setter methods that enforce domain invariants. For example, `setSellingPrice()` rejects negative numbers or null inputs, and `deductQuantity()` verifies that the requested quantity does not exceed the batch's current stock, preventing negative inventory corruption.

### Q3: Where is Polymorphism used in the application? Give two distinct examples.
**Answer**:
1. **Dynamic Method Dispatch through Interfaces**: The GUI communicates with service interfaces (`InventoryService`, `ReturnsService`). At runtime, the JVM polymorphically invokes methods on the concrete implementation classes (`InventoryServiceImpl`, `ReturnsServiceImpl`).
2. **Natural Sorting via `Comparable<ProductBatch>`**: [`ProductBatch.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/model/ProductBatch.java) implements `Comparable<ProductBatch>` and overrides `compareTo()`. When `eligibleBatches.sort(Comparator.naturalOrder())` is executed during checkout, Java polymorphically calls this comparison logic to sort batches strictly by earliest expiration date first (FEFO).

### Q4: Explain the difference between Method Overloading and Method Overriding in ShelfLife.
**Answer**:
- **Method Overloading (Compile-Time Polymorphism)**: Occurs within the same class where multiple methods share the same name with different parameter signatures. For example, in [`ProductBatch.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/model/ProductBatch.java), we provide multiple constructor overloads:
  - `ProductBatch(batchId, productId, qty, expiryDate)`
  - `ProductBatch(batchId, productId, qty, expiryDate, costPrice, supplierId, receivedDate)`
- **Method Overriding (Runtime Polymorphism)**: Occurs when a subclass provides a specific implementation of a method declared in its parent class or interface. For example, [`ProductInventoryPanel.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/ui/inventory/ProductInventoryPanel.java) overrides `refreshData()` from the [`Refreshable`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/ui/Refreshable.java) interface.

---

## Part 2: Business Logic & Expiry Engine (FEFO)

### Q5: What is First-Expiry-First-Out (FEFO), and why is it superior to FIFO in retail?
**Answer**: First-In-First-Out (FIFO) sells the batch that arrived at the store first. In retail and pharmacy, supplier delivery dates do not always match manufacturer expiry dates; a newer shipment could arrive with a shorter expiration window than older stock. FEFO ignores delivery dates and strictly prioritizes selling the batch with the **earliest expiry date**, drastically reducing stock spoilage, customer dissatisfaction, and wastage losses.

### Q6: What happens if a customer wants to buy 15 units of an item, but the earliest expiring batch only has 10 units?
**Answer**: The [`SalesServiceImpl`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/service/SalesServiceImpl.java) FEFO engine handles multi-batch allocation automatically:
1. It allocates all 10 units from the earliest expiring batch (reducing that batch to 0).
2. It allocates the remaining 5 units from the next earliest expiring batch.
3. It creates two distinct [`SaleItem`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/model/SaleItem.java) records referencing their respective `batchId`s, preserving complete audit traceability on the receipt.

### Q7: If a product expires today, does ShelfLife consider it expired today?
**Answer**: No. According to retail standards and our implementation in `ProductBatch.isExpired(LocalDate referenceDate)`:
```java
public boolean isExpired(LocalDate referenceDate) {
    return this.expiryDate.isBefore(referenceDate);
}
```
A product expiring on October 27th is valid until 11:59:59 PM on October 27th. It is only flagged as expired when `referenceDate` is strictly after October 27th (i.e. starting October 28th).

---

## Part 3: Financial Calculations & Precision

### Q8: Why did you use `BigDecimal` instead of `double` or `float` for monetary amounts?
**Answer**: `double` and `float` are binary floating-point representations based on IEEE 754. They cannot accurately represent decimal fractions like `0.10` or `0.05`, leading to cumulative rounding errors (e.g. `0.1 + 0.2 = 0.30000000000000004`). In financial systems and POS checkout, rounding drift causes balance discrepancies and accounting violations. `BigDecimal` provides arbitrary-precision arithmetic and explicit rounding control (`RoundingMode.HALF_UP`), ensuring monetary figures are accurate to two decimal places.

### Q9: How is Net Operating Profit calculated in ShelfLife?
**Answer**: Implemented in [`ReportServiceImpl.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/service/ReportServiceImpl.java):
$$\text{Net Profit} = \text{Gross Revenue} - \text{COGS} - \text{Wastage Losses} - \text{Refunds}$$
- **Gross Revenue**: Sum of all `PAID` sales.
- **Cost of Goods Sold (COGS)**: Sum of $(\text{quantity sold} \times \text{batch cost price})$ across all sale items.
- **Wastage Losses**: Financial cost of damaged or expired stock written off to `wastage.csv`.
- **Refunds**: Total cash returned to customers from processed returns.

---

## Part 4: Data Integrity, Transactions & Persistence

### Q10: How does your system guarantee data integrity without an SQL database engine?
**Answer**: In [`CsvDataStore.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/storage/CsvDataStore.java), we implemented two layers of protection:
1. **Atomic File Writes**: Files are written to temporary `.tmp` files first, flushed, and then atomically renamed via `File.renameTo()`. If writing fails, the original CSV file is never corrupted.
2. **Transactional Snapshot Engine**: For multi-file mutations (such as sales or returns modifying batches, sales, and line items), `executeTransaction` creates synchronous `.bak` backup copies before making any changes. If any exception occurs mid-transaction, all files are immediately restored from their `.bak` snapshots.

### Q11: What is RFC-4180 compliance, and why is it important for your CSV storage?
**Answer**: RFC-4180 is the standard specification for CSV files. If a user enters a product name containing commas or quotes (e.g., `Organic Honey, 500g "Raw"`), a naive `split(",")` would split the field into multiple columns, corrupting the row index. Our custom `parseCsvLine()` properly tracks opening and closing quotes, and `escapeCsv()` wraps fields with commas in quotes and doubles internal quotation marks (`""`).

### Q12: Why do you forbid hard-deleting a product that has historical sales?
**Answer**: Hard-deleting a product with prior sales violates referential integrity. Historical receipts and monthly financial reports referencing that `productId` would become orphaned, causing `NullPointerException`s during accounting audits. We implemented **Soft Deletion**: setting `active = false` via `setProductActive()`. The product remains in the database for historical reporting but cannot be added to new POS sales.

---

## Part 5: Security & Role-Based Access Control (RBAC)

### Q13: How does ShelfLife protect stored user passwords?
**Answer**: Passwords are never stored in plaintext. In [`PasswordUtil.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/util/PasswordUtil.java):
1. A cryptographically secure 16-byte random salt is generated via `SecureRandom`.
2. The salt and plaintext password are hashed using SHA-256 via `MessageDigest.getInstance("SHA-256")`.
3. The resulting 64-character hexadecimal hash and the unique salt are stored in `users.csv`.
4. Storing a unique salt per user prevents Rainbow Table attacks and guarantees that two identical passwords produce completely different stored hashes.

### Q14: How is Role-Based Access Control (RBAC) enforced between Cashiers and Admins?
**Answer**: In [`ShelfLifeApp.java`](file:///e:/Uni%20Learning/JAVA/2nd%20Sem/Shelf%20Life%20%28Inventory%20Management%20System%29/ShelfLife/src/shelflife/ui/ShelfLifeApp.java):
- When a user signs in, `applyUserSession()` checks `user.getRole()`.
- If the role is `CASHIER`, `rebuildSidebar()` hides all administrative buttons (Suppliers, Expiry, Reports, Reorder, Logs, Settings), rendering only **Billing POS**, **Returns**, and **Product Lookup**.
- In addition, `showCard()` enforces a security check: if a cashier attempts to access an administrative card programmatically, navigation is denied with a `Permission Denied` warning dialog.

---

## Part 6: Java Language & Swing GUI Questions

### Q15: Why is Java Swing's `CardLayout` ideal for this application?
**Answer**: `CardLayout` treats each panel as a distinct card in a stack. Only one card is visible at any given time. This allows instant, flicker-free switching between the 9 system modules without opening and closing multiple separate desktop windows. It maintains a clean, centralized single-window desktop workspace.

### Q16: Why did you use `synchronized` methods in `InventoryServiceImpl` and `CsvDataStore`?
**Answer**: In a multi-threaded desktop application (where Swing UI events run on the Event Dispatch Thread and background tasks may perform auto-backups or reports), concurrent writes to in-memory caches or CSV files could cause race conditions. Marking core data access methods as `synchronized` ensures thread-safe execution by acquiring the monitor lock on the service instance.

### Q17: What is the Event Dispatch Thread (EDT) in Java Swing?
**Answer**: Swing GUI components are not thread-safe. All visual rendering, event handling (such as button clicks), and UI component updates must execute on a dedicated thread called the **Event Dispatch Thread (EDT)**. We use `SwingUtilities.invokeLater()` to launch the main application frame onto the EDT.

---

## Quick Reference Summary Table for Examiners

| Topic | Implementation Class / Method | Architectural Benefit |
| :--- | :--- | :--- |
| **Separation of Concerns** | UI $\rightarrow$ Service $\rightarrow$ Storage | Clean layering; changes in UI do not break storage |
| **Defensive Copying** | `Collections.unmodifiableList(new ArrayList<>(cache))` | Prevents external code from modifying internal service state |
| **Custom Exceptions** | `SaleException` | Provides domain-specific error handling for checkout deficits |
| **PKR Currency** | `CurrencyUtil.format(BigDecimal)` | Uniform `Rs. #,##0.00` representation across all tables & receipts |
| **Atomic File Renaming** | `CsvDataStore.atomicWrite` | Protects files from corruption during power interruptions |
| **Salted Hashing** | `PasswordUtil.hashPassword` | Prevents credential leaks and protects user passwords |
