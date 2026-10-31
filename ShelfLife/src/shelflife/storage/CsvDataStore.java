package shelflife.storage;

import shelflife.model.*;
import shelflife.util.PasswordUtil;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enterprise-grade local CSV persistence engine conforming to the DataStore contract.
 * Features:
 * 1. UTF-8 file reading/writing with RFC-4180 quote-aware CSV tokenization.
 * 2. Atomic write-to-temp and file replacement preventing file corruption.
 * 3. File backup snapshot and automatic rollback support for multi-file ACID transactions.
 * 4. Backward-compatible parsing across older and extended schema records.
 * 5. Full data backup creation and safe restoration with pre-restore safety snapshots.
 */
public class CsvDataStore implements DataStore {

    private final File dataDir;
    private final File productsFile;
    private final File batchesFile;
    private final File salesFile;
    private final File saleItemsFile;
    private final File wastageFile;
    private final File suppliersFile;
    private final File usersFile;
    private final File returnsFile;
    private final File returnItemsFile;
    private final File activityLogsFile;
    private final File settingsFile;

    private final File heldBillsFile;
    private final File heldBillItemsFile;
    private final File salePaymentsFile;
    private final File customersFile;
    private final File customerLedgerFile;
    private final File supplierPayablesFile;
    private final File supplierPaymentsFile;
    private final File cashierShiftsFile;
    private final File priceOverridesFile;

    private final File capitalTransactionsFile;
    private final File expensesFile;
    private final File accountsFile;
    private final File accountTransactionsFile;
    private final File purchaseOrdersFile;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public CsvDataStore(File dataDir) {
        if (dataDir == null) {
            throw new IllegalArgumentException("Data directory cannot be null.");
        }
        this.dataDir = dataDir;
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.productsFile = new File(dataDir, "products.csv");
        this.batchesFile = new File(dataDir, "batches.csv");
        this.salesFile = new File(dataDir, "sales.csv");
        this.saleItemsFile = new File(dataDir, "sale_items.csv");
        this.wastageFile = new File(dataDir, "wastage.csv");
        this.suppliersFile = new File(dataDir, "suppliers.csv");
        this.usersFile = new File(dataDir, "users.csv");
        this.returnsFile = new File(dataDir, "returns.csv");
        this.returnItemsFile = new File(dataDir, "return_items.csv");
        this.activityLogsFile = new File(dataDir, "activity_logs.csv");
        this.settingsFile = new File(dataDir, "settings.csv");

        this.heldBillsFile = new File(dataDir, "held_bills.csv");
        this.heldBillItemsFile = new File(dataDir, "held_bill_items.csv");
        this.salePaymentsFile = new File(dataDir, "sale_payments.csv");
        this.customersFile = new File(dataDir, "customers.csv");
        this.customerLedgerFile = new File(dataDir, "customer_ledger.csv");
        this.supplierPayablesFile = new File(dataDir, "supplier_payables.csv");
        this.supplierPaymentsFile = new File(dataDir, "supplier_payments.csv");
        this.cashierShiftsFile = new File(dataDir, "cashier_shifts.csv");
        this.priceOverridesFile = new File(dataDir, "price_overrides.csv");

        this.capitalTransactionsFile = new File(dataDir, "capital_transactions.csv");
        this.expensesFile = new File(dataDir, "expenses.csv");
        this.accountsFile = new File(dataDir, "accounts.csv");
        this.accountTransactionsFile = new File(dataDir, "account_transactions.csv");
        this.purchaseOrdersFile = new File(dataDir, "purchase_orders.csv");

        initStorageFiles();
    }

    @Override
    public File getDataDirectory() {
        return dataDir;
    }

    private void initStorageFiles() {
        try {
            ensureHeader(productsFile, "productId,name,category,sellingPrice,lowStockThreshold,barcode,sku,active");
            ensureHeader(batchesFile, "batchId,productId,quantity,expiryDate,costPrice,supplierId,receivedDate,recalled");
            ensureHeader(salesFile, "saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus,notes,cashierUsername,customerId,discountAmount,reprintCount");
            ensureHeader(saleItemsFile, "saleId,productId,batchId,quantity,unitPrice,lineTotal,costPrice");
            ensureHeader(wastageFile, "wastageId,batchId,quantity,wastageDateTime,reason");
            ensureHeader(suppliersFile, "supplierId,name,contactPerson,phone,email,address,active");
            ensureHeader(usersFile, "userId,username,passwordHash,salt,role,fullName,active,mustChangePassword");
            ensureHeader(returnsFile, "returnId,originalSaleId,returnDateTime,totalRefund,processedBy,reason");
            ensureHeader(returnItemsFile, "returnItemId,returnId,productId,productName,batchId,quantity,unitPrice,lineRefund,disposition");
            ensureHeader(activityLogsFile, "logId,timestamp,username,action,details,recordId");
            ensureHeader(settingsFile, "shopName,currencyCode,currencySymbol,expiryWarningDays,defaultLowStockThreshold,taxRatePercent");

            ensureHeader(heldBillsFile, "heldBillId,createdAt,cashierUsername,customerName,note,itemCount,estimatedTotal");
            ensureHeader(heldBillItemsFile, "heldItemId,heldBillId,productId,productName,quantity,unitPrice,discountAmount,batchId");
            ensureHeader(salePaymentsFile, "paymentLineId,saleId,paymentMethod,amount,referenceNote,timestamp");
            ensureHeader(customersFile, "customerId,fullName,phoneNumber,address,creditLimit,currentBalance,active,createdAt");
            ensureHeader(customerLedgerFile, "entryId,customerId,timestamp,transactionType,referenceId,debit,credit,runningBalance,notes,cashierUsername");
            ensureHeader(supplierPayablesFile, "payableId,supplierId,purchaseRef,invoiceDate,dueDate,totalAmount,paidAmount,balanceDue,status,notes");
            ensureHeader(supplierPaymentsFile, "paymentId,payableId,supplierId,paymentDate,amount,paymentMethod,referenceNumber,notes,recordedBy");
            ensureHeader(cashierShiftsFile, "shiftId,cashierUsername,openedAt,closedAt,openingFloat,cashSales,cashRefunds,expectedCash,actualCash,cashDifference,notes,status");
            ensureHeader(priceOverridesFile, "overrideId,timestamp,cashierUsername,adminApprover,productId,originalPrice,overriddenPrice,reason");

            ensureHeader(capitalTransactionsFile, "transactionId,type,amount,dateTime,investorName,paymentMethod,destinationAccount,description,createdBy");
            ensureHeader(expensesFile, "expenseId,category,description,amount,expenseDate,createdAt,paymentMethod,paymentAccount,paidTo,referenceNumber,createdBy,notes");
            ensureHeader(accountsFile, "accountId,name,type,openingBalance,currentBalance,active");
            ensureHeader(accountTransactionsFile, "transactionId,accountId,dateTime,type,debit,credit,runningBalance,referenceId,description,createdBy");
            ensureHeader(purchaseOrdersFile, "purchaseId,supplierId,supplierInvoiceNo,orderDate,receivedDate,productId,batchId,quantity,unitCost,additionalCost,totalAmount,paidAmount,paymentMethod,paymentAccount,expiryDate,status,createdBy,notes");

            seedInitialDataIfEmpty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV storage files in: " + dataDir.getAbsolutePath(), e);
        }
    }

    private void seedInitialDataIfEmpty() throws IOException {
        // Seed default users if users file is empty (required for first-run authentication)
        List<String> userLines = readLines(usersFile);
        if (userLines.size() <= 1) {
            List<User> defaultUsers = new ArrayList<>();
            String adminSalt = PasswordUtil.generateSalt();
            String adminHash = PasswordUtil.hashPassword("admin123", adminSalt);
            defaultUsers.add(new User("USER-01", "admin", adminHash, adminSalt, Role.ADMIN, "System Administrator", true, false));

            String cashierSalt = PasswordUtil.generateSalt();
            String cashierHash = PasswordUtil.hashPassword("cashier123", cashierSalt);
            defaultUsers.add(new User("USER-02", "cashier", cashierHash, cashierSalt, Role.CASHIER, "Primary Cashier", true, false));

            try {
                saveUsers(defaultUsers);
            } catch (Exception e) {
                System.out.println("Warning: could not seed default users: " + e.getMessage());
            }
        }

        // NOTE: Default suppliers seeding intentionally removed.
        // The suppliers.csv should remain header-only; no hardcoded sample supplier data
        // will be inserted on startup. Users must add suppliers through the UI.

        // Seed default settings if settings file is empty (required for app configuration)
        List<String> settingsLines = readLines(settingsFile);
        if (settingsLines.size() <= 1) {
            try {
                saveSettings(new AppSettings());
            } catch (Exception e) {
                System.out.println("Warning: could not seed settings: " + e.getMessage());
            }
        }
    }

    private void ensureHeader(File file, String header) throws IOException {
        if (!file.exists() || file.length() == 0) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                pw.println(header);
            }
        }
    }

    // =========================================================================
    // Product Persistence
    // =========================================================================

    @Override
    public synchronized List<Product> loadProducts() throws Exception {
        List<Product> products = new ArrayList<>();
        List<String> lines = readLines(productsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 5) {
                try {
                    String id = tokens.get(0).trim();
                    String name = tokens.get(1).trim();
                    String cat = tokens.get(2).trim();
                    BigDecimal price = new BigDecimal(tokens.get(3).trim()).setScale(2, RoundingMode.HALF_UP);
                    int threshold = Integer.parseInt(tokens.get(4).trim());
                    String barcode = tokens.size() > 5 ? tokens.get(5).trim() : "";
                    String sku = tokens.size() > 6 ? tokens.get(6).trim() : "";
                    boolean active = tokens.size() > 7 ? Boolean.parseBoolean(tokens.get(7).trim()) : true;

                    products.add(new Product(id, name, cat, price, threshold, barcode, sku, active));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed product record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return products;
    }

    @Override
    public synchronized void saveProducts(List<Product> products) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("productId,name,category,sellingPrice,lowStockThreshold,barcode,sku,active");
        if (products != null) {
            for (Product p : products) {
                lines.add(String.format("%s,%s,%s,%s,%d,%s,%s,%b",
                        escapeCsv(p.getProductId()),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getCategory()),
                        p.getSellingPrice().toPlainString(),
                        p.getLowStockThreshold(),
                        escapeCsv(p.getBarcode()),
                        escapeCsv(p.getSku()),
                        p.isActive()
                ));
            }
        }
        atomicWrite(productsFile, lines);
    }

    // =========================================================================
    // ProductBatch Persistence
    // =========================================================================

    @Override
    public synchronized List<ProductBatch> loadBatches() throws Exception {
        List<ProductBatch> batches = new ArrayList<>();
        List<String> lines = readLines(batchesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 4) {
                try {
                    String batchId = tokens.get(0).trim();
                    String productId = tokens.get(1).trim();
                    int quantity = Integer.parseInt(tokens.get(2).trim());
                    LocalDate expiry = LocalDate.parse(tokens.get(3).trim(), DATE_FMT);

                    BigDecimal costPrice = tokens.size() > 4 && !tokens.get(4).trim().isEmpty() ? 
                            new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    String supplierId = tokens.size() > 5 && !tokens.get(5).trim().isEmpty() ? tokens.get(5).trim() : "SUP-01";
                    LocalDate receivedDate = tokens.size() > 6 && !tokens.get(6).trim().isEmpty() ? LocalDate.parse(tokens.get(6).trim(), DATE_FMT) : LocalDate.now();
                    boolean recalled = tokens.size() > 7 && !tokens.get(7).trim().isEmpty() ? Boolean.parseBoolean(tokens.get(7).trim()) : false;

                    batches.add(new ProductBatch(batchId, productId, quantity, expiry, costPrice, supplierId, receivedDate, recalled));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed batch record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return batches;
    }

    @Override
    public synchronized void saveBatches(List<ProductBatch> batches) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("batchId,productId,quantity,expiryDate,costPrice,supplierId,receivedDate,recalled");
        if (batches != null) {
            for (ProductBatch b : batches) {
                lines.add(String.format("%s,%s,%d,%s,%s,%s,%s,%b",
                        escapeCsv(b.getBatchId()),
                        escapeCsv(b.getProductId()),
                        b.getQuantity(),
                        b.getExpiryDate().format(DATE_FMT),
                        b.getCostPrice().toPlainString(),
                        escapeCsv(b.getSupplierId()),
                        b.getReceivedDate().format(DATE_FMT),
                        b.isRecalled()
                ));
            }
        }
        atomicWrite(batchesFile, lines);
    }

    // =========================================================================
    // Sales Persistence
    // =========================================================================

    @Override
    public synchronized List<Sale> loadSales() throws Exception {
        List<Sale> sales = new ArrayList<>();
        List<String> lines = readLines(salesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 5) {
                try {
                    String saleId = tokens.get(0).trim();
                    LocalDateTime dt = parseDateTime(tokens.get(1).trim());
                    BigDecimal total = new BigDecimal(tokens.get(2).trim()).setScale(2, RoundingMode.HALF_UP);
                    PaymentMethod method = parsePaymentMethod(tokens.get(3).trim());
                    PaymentStatus status = parsePaymentStatus(tokens.get(4).trim());

                    Sale sale = new Sale(saleId, dt, total, method, status);
                    if (tokens.size() > 5 && !tokens.get(5).trim().isEmpty()) {
                        sale.setNotes(tokens.get(5).trim());
                    }
                    if (tokens.size() > 6 && !tokens.get(6).trim().isEmpty()) {
                        sale.setCashierUsername(tokens.get(6).trim());
                    }
                    if (tokens.size() > 7 && !tokens.get(7).trim().isEmpty()) {
                        sale.setCustomerId(tokens.get(7).trim());
                    }
                    if (tokens.size() > 8 && !tokens.get(8).trim().isEmpty()) {
                        sale.setDiscountAmount(new BigDecimal(tokens.get(8).trim()).setScale(2, RoundingMode.HALF_UP));
                    }
                    if (tokens.size() > 9 && !tokens.get(9).trim().isEmpty()) {
                        sale.setReprintCount(Integer.parseInt(tokens.get(9).trim()));
                    }
                    sales.add(sale);
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed sale record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return sales;
    }

    @Override
    public synchronized void saveSales(List<Sale> sales) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,saleDateTime,totalAmount,paymentMethod,paymentStatus,notes,cashierUsername,customerId,discountAmount,reprintCount");
        if (sales != null) {
            for (Sale s : sales) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%d",
                        escapeCsv(s.getSaleId()),
                        s.getSaleDateTime().format(DATE_TIME_FMT),
                        s.getTotalAmount().toPlainString(),
                        s.getPaymentMethod().name(),
                        s.getPaymentStatus().name(),
                        escapeCsv(s.getNotes() != null ? s.getNotes() : ""),
                        escapeCsv(s.getCashierUsername() != null ? s.getCashierUsername() : ""),
                        escapeCsv(s.getCustomerId() != null ? s.getCustomerId() : ""),
                        s.getDiscountAmount().toPlainString(),
                        s.getReprintCount()
                ));
            }
        }
        atomicWrite(salesFile, lines);
    }

    // =========================================================================
    // Sale Items Persistence
    // =========================================================================

    @Override
    public synchronized List<SaleItem> loadSaleItems() throws Exception {
        List<SaleItem> items = new ArrayList<>();
        List<String> lines = readLines(saleItemsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 6) {
                try {
                    String saleId = tokens.get(0).trim();
                    String productId = tokens.get(1).trim();
                    String batchId = tokens.get(2).trim();
                    int qty = Integer.parseInt(tokens.get(3).trim());
                    BigDecimal price = new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal lineTotal = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);

                    SaleItem it = new SaleItem(saleId, productId, batchId, qty, price, lineTotal);
                    if (tokens.size() > 6 && !tokens.get(6).trim().isEmpty()) {
                        it.setCostPrice(new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP));
                    }
                    items.add(it);
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed sale item at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return items;
    }

    @Override
    public synchronized void saveSaleItems(List<SaleItem> saleItems) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("saleId,productId,batchId,quantity,unitPrice,lineTotal,costPrice");
        if (saleItems != null) {
            for (SaleItem item : saleItems) {
                lines.add(String.format("%s,%s,%s,%d,%s,%s,%s",
                        escapeCsv(item.getSaleId()),
                        escapeCsv(item.getProductId()),
                        escapeCsv(item.getBatchId()),
                        item.getQuantity(),
                        item.getUnitPrice().toPlainString(),
                        item.getLineTotal().toPlainString(),
                        item.getCostPrice().toPlainString()
                ));
            }
        }
        atomicWrite(saleItemsFile, lines);
    }

    // =========================================================================
    // Wastage Persistence
    // =========================================================================

    @Override
    public synchronized List<WastageRecord> loadWastageRecords() throws Exception {
        List<WastageRecord> records = new ArrayList<>();
        List<String> lines = readLines(wastageFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 5) {
                try {
                    String wid = tokens.get(0).trim();
                    String batchId = tokens.get(1).trim();
                    int qty = Integer.parseInt(tokens.get(2).trim());
                    LocalDateTime dt = parseDateTime(tokens.get(3).trim());
                    String reason = tokens.get(4).trim();
                    records.add(new WastageRecord(wid, batchId, qty, dt, reason));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed wastage record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return records;
    }

    @Override
    public synchronized void saveWastageRecords(List<WastageRecord> wastageRecords) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("wastageId,batchId,quantity,wastageDateTime,reason");
        if (wastageRecords != null) {
            for (WastageRecord r : wastageRecords) {
                lines.add(String.format("%s,%s,%d,%s,%s",
                        escapeCsv(r.getWastageId()),
                        escapeCsv(r.getBatchId()),
                        r.getQuantity(),
                        r.getWastageDateTime().format(DATE_TIME_FMT),
                        escapeCsv(r.getReason())
                ));
            }
        }
        atomicWrite(wastageFile, lines);
    }

    // =========================================================================
    // Supplier Persistence
    // =========================================================================

    @Override
    public synchronized List<Supplier> loadSuppliers() throws Exception {
        List<Supplier> suppliers = new ArrayList<>();
        List<String> lines = readLines(suppliersFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 7) {
                try {
                    String id = tokens.get(0).trim();
                    String name = tokens.get(1).trim();
                    String contact = tokens.get(2).trim();
                    String phone = tokens.get(3).trim();
                    String email = tokens.get(4).trim();
                    String address = tokens.get(5).trim();
                    boolean active = Boolean.parseBoolean(tokens.get(6).trim());
                    suppliers.add(new Supplier(id, name, contact, phone, email, address, active));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed supplier record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return suppliers;
    }

    @Override
    public synchronized void saveSuppliers(List<Supplier> suppliers) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("supplierId,name,contactPerson,phone,email,address,active");
        if (suppliers != null) {
            for (Supplier s : suppliers) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%b",
                        escapeCsv(s.getSupplierId()),
                        escapeCsv(s.getName()),
                        escapeCsv(s.getContactPerson()),
                        escapeCsv(s.getPhone()),
                        escapeCsv(s.getEmail()),
                        escapeCsv(s.getAddress()),
                        s.isActive()
                ));
            }
        }
        atomicWrite(suppliersFile, lines);
    }

    // =========================================================================
    // User Persistence
    // =========================================================================

    @Override
    public synchronized List<User> loadUsers() throws Exception {
        List<User> users = new ArrayList<>();
        List<String> lines = readLines(usersFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 8) {
                try {
                    String uid = tokens.get(0).trim();
                    String username = tokens.get(1).trim();
                    String hash = tokens.get(2).trim();
                    String salt = tokens.get(3).trim();
                    Role role = Role.valueOf(tokens.get(4).trim().toUpperCase());
                    String fullName = tokens.get(5).trim();
                    boolean active = Boolean.parseBoolean(tokens.get(6).trim());
                    boolean mustChange = Boolean.parseBoolean(tokens.get(7).trim());
                    users.add(new User(uid, username, hash, salt, role, fullName, active, mustChange));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed user record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return users;
    }

    @Override
    public synchronized void saveUsers(List<User> users) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("userId,username,passwordHash,salt,role,fullName,active,mustChangePassword");
        if (users != null) {
            for (User u : users) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%b,%b",
                        escapeCsv(u.getUserId()),
                        escapeCsv(u.getUsername()),
                        escapeCsv(u.getPasswordHash()),
                        escapeCsv(u.getSalt()),
                        u.getRole().name(),
                        escapeCsv(u.getFullName()),
                        u.isActive(),
                        u.isMustChangePassword()
                ));
            }
        }
        atomicWrite(usersFile, lines);
    }

    // =========================================================================
    // Returns & Return Items Persistence
    // =========================================================================

    @Override
    public synchronized List<SaleReturn> loadReturns() throws Exception {
        List<SaleReturn> returns = new ArrayList<>();
        List<String> lines = readLines(returnsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 6) {
                try {
                    String retId = tokens.get(0).trim();
                    String saleId = tokens.get(1).trim();
                    LocalDateTime dt = parseDateTime(tokens.get(2).trim());
                    BigDecimal refund = new BigDecimal(tokens.get(3).trim()).setScale(2, RoundingMode.HALF_UP);
                    String user = tokens.get(4).trim();
                    String reason = tokens.get(5).trim();
                    returns.add(new SaleReturn(retId, saleId, dt, refund, user, reason));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed return at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return returns;
    }

    @Override
    public synchronized void saveReturns(List<SaleReturn> returns) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("returnId,originalSaleId,returnDateTime,totalRefund,processedBy,reason");
        if (returns != null) {
            for (SaleReturn r : returns) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s",
                        escapeCsv(r.getReturnId()),
                        escapeCsv(r.getOriginalSaleId()),
                        r.getReturnDate().format(DATE_TIME_FMT),
                        r.getTotalRefund().toPlainString(),
                        escapeCsv(r.getProcessedBy()),
                        escapeCsv(r.getReason())
                ));
            }
        }
        atomicWrite(returnsFile, lines);
    }

    @Override
    public synchronized List<SaleReturnItem> loadReturnItems() throws Exception {
        List<SaleReturnItem> items = new ArrayList<>();
        List<String> lines = readLines(returnItemsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 9) {
                try {
                    String retItemId = tokens.get(0).trim();
                    String retId = tokens.get(1).trim();
                    String prodId = tokens.get(2).trim();
                    String prodName = tokens.get(3).trim();
                    String batchId = tokens.get(4).trim();
                    int qty = Integer.parseInt(tokens.get(5).trim());
                    BigDecimal unitPrice = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal refund = new BigDecimal(tokens.get(7).trim()).setScale(2, RoundingMode.HALF_UP);
                    String disp = tokens.get(8).trim();
                    items.add(new SaleReturnItem(retItemId, retId, prodId, prodName, batchId, qty, unitPrice, refund, disp));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed return item at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return items;
    }

    @Override
    public synchronized void saveReturnItems(List<SaleReturnItem> items) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("returnItemId,returnId,productId,productName,batchId,quantity,unitPrice,lineRefund,disposition");
        if (items != null) {
            for (SaleReturnItem it : items) {
                lines.add(String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s",
                        escapeCsv(it.getReturnItemId()),
                        escapeCsv(it.getReturnId()),
                        escapeCsv(it.getProductId()),
                        escapeCsv(it.getProductName()),
                        escapeCsv(it.getBatchId()),
                        it.getQuantity(),
                        it.getUnitPrice().toPlainString(),
                        it.getLineRefund().toPlainString(),
                        escapeCsv(it.getDisposition())
                ));
            }
        }
        atomicWrite(returnItemsFile, lines);
    }

    // =========================================================================
    // Activity Logs Persistence
    // =========================================================================

    @Override
    public synchronized List<ActivityLogEntry> loadActivityLogs() throws Exception {
        List<ActivityLogEntry> logs = new ArrayList<>();
        List<String> lines = readLines(activityLogsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 6) {
                try {
                    String lid = tokens.get(0).trim();
                    LocalDateTime ts = parseDateTime(tokens.get(1).trim());
                    String user = tokens.get(2).trim();
                    String act = tokens.get(3).trim();
                    String details = tokens.get(4).trim();
                    String recId = tokens.get(5).trim();
                    logs.add(new ActivityLogEntry(lid, ts, user, act, details, recId));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed log at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return logs;
    }

    @Override
    public synchronized void saveActivityLogs(List<ActivityLogEntry> logs) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("logId,timestamp,username,action,details,recordId");
        if (logs != null) {
            for (ActivityLogEntry l : logs) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s",
                        escapeCsv(l.getLogId()),
                        l.getTimestamp().format(DATE_TIME_FMT),
                        escapeCsv(l.getUsername()),
                        escapeCsv(l.getAction()),
                        escapeCsv(l.getDetails()),
                        escapeCsv(l.getRecordId())
                ));
            }
        }
        atomicWrite(activityLogsFile, lines);
    }

    // =========================================================================
    // Settings Persistence
    // =========================================================================

    @Override
    public synchronized AppSettings loadSettings() throws Exception {
        List<String> lines = readLines(settingsFile);
        if (lines.size() >= 2) {
            String line = lines.get(1).trim();
            if (!line.isEmpty()) {
                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 6) {
                    try {
                        String shop = tokens.get(0).trim();
                        String curr = tokens.get(1).trim();
                        String sym = tokens.get(2).trim();
                        int warn = Integer.parseInt(tokens.get(3).trim());
                        int thresh = Integer.parseInt(tokens.get(4).trim());
                        BigDecimal tax = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                        return new AppSettings(shop, curr, sym, warn, thresh, tax);
                    } catch (Exception ex) {
                        System.out.println("Warning: Malformed settings record, returning defaults: " + ex.getMessage());
                    }
                }
            }
        }
        return new AppSettings();
    }

    @Override
    public synchronized void saveSettings(AppSettings settings) throws Exception {
        if (settings == null) {
            settings = new AppSettings();
        }
        List<String> lines = new ArrayList<>();
        lines.add("shopName,currencyCode,currencySymbol,expiryWarningDays,defaultLowStockThreshold,taxRatePercent");
        lines.add(String.format("%s,%s,%s,%d,%d,%s",
                escapeCsv(settings.getShopName()),
                escapeCsv(settings.getCurrencyCode()),
                escapeCsv(settings.getCurrencySymbol()),
                settings.getExpiryWarningDays(),
                settings.getDefaultLowStockThreshold(),
                settings.getTaxRatePercent().toPlainString()
        ));
        atomicWrite(settingsFile, lines);
    }

    // =========================================================================
    // Held Bills Persistence
    // =========================================================================

    @Override
    public synchronized List<HeldBill> loadHeldBills() throws Exception {
        List<HeldBill> list = new ArrayList<>();
        List<String> lines = readLines(heldBillsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 7) {
                try {
                    String heldBillId = tokens.get(0).trim();
                    LocalDateTime createdAt = parseDateTime(tokens.get(1).trim());
                    String cashier = tokens.get(2).trim();
                    String customer = tokens.get(3).trim();
                    String note = tokens.get(4).trim();
                    int itemCount = Integer.parseInt(tokens.get(5).trim());
                    BigDecimal total = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    list.add(new HeldBill(heldBillId, createdAt, cashier, customer, note, itemCount, total));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed held bill at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveHeldBills(List<HeldBill> bills) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("heldBillId,createdAt,cashierUsername,customerName,note,itemCount,estimatedTotal");
        if (bills != null) {
            for (HeldBill b : bills) {
                lines.add(String.format("%s,%s,%s,%s,%s,%d,%s",
                        escapeCsv(b.getHeldBillId()),
                        b.getCreatedAt().format(DATE_TIME_FMT),
                        escapeCsv(b.getCashierUsername()),
                        escapeCsv(b.getCustomerName()),
                        escapeCsv(b.getNote()),
                        b.getItemCount(),
                        b.getEstimatedTotal().toPlainString()
                ));
            }
        }
        atomicWrite(heldBillsFile, lines);
    }

    @Override
    public synchronized List<HeldBillItem> loadHeldBillItems() throws Exception {
        List<HeldBillItem> list = new ArrayList<>();
        List<String> lines = readLines(heldBillItemsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 7) {
                try {
                    String id = tokens.get(0).trim();
                    String billId = tokens.get(1).trim();
                    String prodId = tokens.get(2).trim();
                    String prodName = tokens.get(3).trim();
                    int qty = Integer.parseInt(tokens.get(4).trim());
                    BigDecimal price = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal disc = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    String batchId = tokens.size() > 7 ? tokens.get(7).trim() : "";
                    list.add(new HeldBillItem(id, billId, prodId, prodName, qty, price, disc, batchId));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed held bill item at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveHeldBillItems(List<HeldBillItem> items) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("heldItemId,heldBillId,productId,productName,quantity,unitPrice,discountAmount,batchId");
        if (items != null) {
            for (HeldBillItem it : items) {
                lines.add(String.format("%s,%s,%s,%s,%d,%s,%s,%s",
                        escapeCsv(it.getHeldItemId()),
                        escapeCsv(it.getHeldBillId()),
                        escapeCsv(it.getProductId()),
                        escapeCsv(it.getProductName()),
                        it.getQuantity(),
                        it.getUnitPrice().toPlainString(),
                        it.getDiscountAmount().toPlainString(),
                        escapeCsv(it.getBatchId() != null ? it.getBatchId() : "")
                ));
            }
        }
        atomicWrite(heldBillItemsFile, lines);
    }

    // =========================================================================
    // Payment Lines Persistence
    // =========================================================================

    @Override
    public synchronized List<PaymentLine> loadPaymentLines() throws Exception {
        List<PaymentLine> list = new ArrayList<>();
        List<String> lines = readLines(salePaymentsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 5) {
                try {
                    String id = tokens.get(0).trim();
                    String saleId = tokens.get(1).trim();
                    PaymentMethod method = parsePaymentMethod(tokens.get(2).trim());
                    BigDecimal amt = new BigDecimal(tokens.get(3).trim()).setScale(2, RoundingMode.HALF_UP);
                    String ref = tokens.get(4).trim();
                    LocalDateTime ts = tokens.size() > 5 ? parseDateTime(tokens.get(5).trim()) : LocalDateTime.now();
                    list.add(new PaymentLine(id, saleId, method, amt, ref, ts));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed payment line at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void savePaymentLines(List<PaymentLine> lines) throws Exception {
        List<String> fileLines = new ArrayList<>();
        fileLines.add("paymentLineId,saleId,paymentMethod,amount,referenceNote,timestamp");
        if (lines != null) {
            for (PaymentLine pl : lines) {
                fileLines.add(String.format("%s,%s,%s,%s,%s,%s",
                        escapeCsv(pl.getPaymentLineId()),
                        escapeCsv(pl.getSaleId()),
                        pl.getPaymentMethod().name(),
                        pl.getAmount().toPlainString(),
                        escapeCsv(pl.getReferenceNote()),
                        pl.getTimestamp().format(DATE_TIME_FMT)
                ));
            }
        }
        atomicWrite(salePaymentsFile, fileLines);
    }

    // =========================================================================
    // Customer & Customer Ledger Persistence
    // =========================================================================

    @Override
    public synchronized List<Customer> loadCustomers() throws Exception {
        List<Customer> list = new ArrayList<>();
        List<String> lines = readLines(customersFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 8) {
                try {
                    String id = tokens.get(0).trim();
                    String name = tokens.get(1).trim();
                    String phone = tokens.get(2).trim();
                    String addr = tokens.get(3).trim();
                    BigDecimal limit = new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal bal = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    boolean act = Boolean.parseBoolean(tokens.get(6).trim());
                    LocalDate created = LocalDate.parse(tokens.get(7).trim(), DATE_FMT);
                    list.add(new Customer(id, name, phone, addr, limit, bal, act, created));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed customer at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveCustomers(List<Customer> customers) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("customerId,fullName,phoneNumber,address,creditLimit,currentBalance,active,createdAt");
        if (customers != null) {
            for (Customer c : customers) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%b,%s",
                        escapeCsv(c.getCustomerId()),
                        escapeCsv(c.getFullName()),
                        escapeCsv(c.getPhoneNumber()),
                        escapeCsv(c.getAddress()),
                        c.getCreditLimit().toPlainString(),
                        c.getCurrentBalance().toPlainString(),
                        c.isActive(),
                        c.getCreatedAt().format(DATE_FMT)
                ));
            }
        }
        atomicWrite(customersFile, lines);
    }

    @Override
    public synchronized List<CustomerLedgerEntry> loadCustomerLedger() throws Exception {
        List<CustomerLedgerEntry> list = new ArrayList<>();
        List<String> lines = readLines(customerLedgerFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 10) {
                try {
                    String id = tokens.get(0).trim();
                    String custId = tokens.get(1).trim();
                    LocalDateTime ts = parseDateTime(tokens.get(2).trim());
                    String type = tokens.get(3).trim();
                    String ref = tokens.get(4).trim();
                    BigDecimal debit = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal credit = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal bal = new BigDecimal(tokens.get(7).trim()).setScale(2, RoundingMode.HALF_UP);
                    String notes = tokens.get(8).trim();
                    String user = tokens.get(9).trim();
                    list.add(new CustomerLedgerEntry(id, custId, ts, type, ref, debit, credit, bal, notes, user));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed customer ledger entry at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveCustomerLedger(List<CustomerLedgerEntry> entries) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("entryId,customerId,timestamp,transactionType,referenceId,debit,credit,runningBalance,notes,cashierUsername");
        if (entries != null) {
            for (CustomerLedgerEntry e : entries) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(e.getEntryId()),
                        escapeCsv(e.getCustomerId()),
                        e.getTimestamp().format(DATE_TIME_FMT),
                        escapeCsv(e.getTransactionType()),
                        escapeCsv(e.getReferenceId()),
                        e.getDebit().toPlainString(),
                        e.getCredit().toPlainString(),
                        e.getRunningBalance().toPlainString(),
                        escapeCsv(e.getNotes()),
                        escapeCsv(e.getCashierUsername())
                ));
            }
        }
        atomicWrite(customerLedgerFile, lines);
    }

    // =========================================================================
    // Supplier Payables & Payments Persistence
    // =========================================================================

    @Override
    public synchronized List<SupplierPayable> loadSupplierPayables() throws Exception {
        List<SupplierPayable> list = new ArrayList<>();
        List<String> lines = readLines(supplierPayablesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 9) {
                try {
                    String id = tokens.get(0).trim();
                    String supId = tokens.get(1).trim();
                    String ref = tokens.get(2).trim();
                    LocalDate invDate = LocalDate.parse(tokens.get(3).trim(), DATE_FMT);
                    LocalDate dueDate = LocalDate.parse(tokens.get(4).trim(), DATE_FMT);
                    BigDecimal total = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal paid = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal bal = new BigDecimal(tokens.get(7).trim()).setScale(2, RoundingMode.HALF_UP);
                    String status = tokens.get(8).trim();
                    String notes = tokens.size() > 9 ? tokens.get(9).trim() : "";
                    list.add(new SupplierPayable(id, supId, ref, invDate, dueDate, total, paid, bal, status, notes));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed supplier payable at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveSupplierPayables(List<SupplierPayable> payables) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("payableId,supplierId,purchaseRef,invoiceDate,dueDate,totalAmount,paidAmount,balanceDue,status,notes");
        if (payables != null) {
            for (SupplierPayable p : payables) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(p.getPayableId()),
                        escapeCsv(p.getSupplierId()),
                        escapeCsv(p.getPurchaseRef()),
                        p.getInvoiceDate().format(DATE_FMT),
                        p.getDueDate().format(DATE_FMT),
                        p.getTotalAmount().toPlainString(),
                        p.getPaidAmount().toPlainString(),
                        p.getBalanceDue().toPlainString(),
                        escapeCsv(p.getStatus()),
                        escapeCsv(p.getNotes())
                ));
            }
        }
        atomicWrite(supplierPayablesFile, lines);
    }

    @Override
    public synchronized List<SupplierPayment> loadSupplierPayments() throws Exception {
        List<SupplierPayment> list = new ArrayList<>();
        List<String> lines = readLines(supplierPaymentsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 8) {
                try {
                    String id = tokens.get(0).trim();
                    String payId = tokens.get(1).trim();
                    String supId = tokens.get(2).trim();
                    LocalDate dt = LocalDate.parse(tokens.get(3).trim(), DATE_FMT);
                    BigDecimal amt = new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP);
                    String method = tokens.get(5).trim();
                    String ref = tokens.get(6).trim();
                    String notes = tokens.get(7).trim();
                    String user = tokens.size() > 8 ? tokens.get(8).trim() : "admin";
                    list.add(new SupplierPayment(id, payId, supId, dt, amt, method, ref, notes, user));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed supplier payment at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveSupplierPayments(List<SupplierPayment> payments) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("paymentId,payableId,supplierId,paymentDate,amount,paymentMethod,referenceNumber,notes,recordedBy");
        if (payments != null) {
            for (SupplierPayment sp : payments) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(sp.getPaymentId()),
                        escapeCsv(sp.getPayableId()),
                        escapeCsv(sp.getSupplierId()),
                        sp.getPaymentDate().format(DATE_FMT),
                        sp.getAmount().toPlainString(),
                        escapeCsv(sp.getPaymentMethod()),
                        escapeCsv(sp.getReferenceNumber()),
                        escapeCsv(sp.getNotes()),
                        escapeCsv(sp.getRecordedBy())
                ));
            }
        }
        atomicWrite(supplierPaymentsFile, lines);
    }

    // =========================================================================
    // Cashier Shifts Persistence
    // =========================================================================

    @Override
    public synchronized List<CashierShift> loadCashierShifts() throws Exception {
        List<CashierShift> list = new ArrayList<>();
        List<String> lines = readLines(cashierShiftsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 11) {
                try {
                    String id = tokens.get(0).trim();
                    String cashier = tokens.get(1).trim();
                    LocalDateTime openedAt = parseDateTime(tokens.get(2).trim());
                    LocalDateTime closedAt = (tokens.get(3).trim().isEmpty() || "null".equalsIgnoreCase(tokens.get(3).trim())) ? null : parseDateTime(tokens.get(3).trim());
                    BigDecimal openingFloat = new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal cashSales = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal cashRefunds = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal expCash = new BigDecimal(tokens.get(7).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal actCash = new BigDecimal(tokens.get(8).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal diff = new BigDecimal(tokens.get(9).trim()).setScale(2, RoundingMode.HALF_UP);
                    String notes = tokens.get(10).trim();
                    String status = tokens.size() > 11 ? tokens.get(11).trim() : "CLOSED";
                    list.add(new CashierShift(id, cashier, openedAt, closedAt, openingFloat, cashSales, cashRefunds, expCash, actCash, diff, notes, status));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed shift at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveCashierShifts(List<CashierShift> shifts) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("shiftId,cashierUsername,openedAt,closedAt,openingFloat,cashSales,cashRefunds,expectedCash,actualCash,cashDifference,notes,status");
        if (shifts != null) {
            for (CashierShift s : shifts) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(s.getShiftId()),
                        escapeCsv(s.getCashierUsername()),
                        s.getOpenedAt().format(DATE_TIME_FMT),
                        s.getClosedAt() != null ? s.getClosedAt().format(DATE_TIME_FMT) : "",
                        s.getOpeningFloat().toPlainString(),
                        s.getCashSales().toPlainString(),
                        s.getCashRefunds().toPlainString(),
                        s.getExpectedCash().toPlainString(),
                        s.getActualCash().toPlainString(),
                        s.getCashDifference().toPlainString(),
                        escapeCsv(s.getNotes()),
                        escapeCsv(s.getStatus())
                ));
            }
        }
        atomicWrite(cashierShiftsFile, lines);
    }

    // =========================================================================
    // Price Override Log Persistence
    // =========================================================================

    @Override
    public synchronized List<PriceOverrideLog> loadPriceOverrides() throws Exception {
        List<PriceOverrideLog> list = new ArrayList<>();
        List<String> lines = readLines(priceOverridesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 8) {
                try {
                    String id = tokens.get(0).trim();
                    LocalDateTime ts = parseDateTime(tokens.get(1).trim());
                    String cashier = tokens.get(2).trim();
                    String admin = tokens.get(3).trim();
                    String prod = tokens.get(4).trim();
                    BigDecimal orig = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal ovr = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    String reason = tokens.get(7).trim();
                    list.add(new PriceOverrideLog(id, ts, cashier, admin, prod, orig, ovr, reason));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed price override at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void savePriceOverrides(List<PriceOverrideLog> overrides) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("overrideId,timestamp,cashierUsername,adminApprover,productId,originalPrice,overriddenPrice,reason");
        if (overrides != null) {
            for (PriceOverrideLog o : overrides) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(o.getOverrideId()),
                        o.getTimestamp().format(DATE_TIME_FMT),
                        escapeCsv(o.getCashierUsername()),
                        escapeCsv(o.getAdminApprover()),
                        escapeCsv(o.getProductId()),
                        o.getOriginalPrice().toPlainString(),
                        o.getOverriddenPrice().toPlainString(),
                        escapeCsv(o.getReason())
                ));
            }
        }
        atomicWrite(priceOverridesFile, lines);
    }

    // =========================================================================
    // CapitalTransaction Persistence
    // =========================================================================

    @Override
    public synchronized List<CapitalTransaction> loadCapitalTransactions() throws Exception {
        List<CapitalTransaction> list = new ArrayList<>();
        List<String> lines = readLines(capitalTransactionsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 8) {
                try {
                    String id = tokens.get(0).trim();
                    CapitalTransaction.Type type = CapitalTransaction.Type.valueOf(tokens.get(1).trim().toUpperCase());
                    BigDecimal amount = new BigDecimal(tokens.get(2).trim()).setScale(2, RoundingMode.HALF_UP);
                    LocalDateTime dt = parseDateTime(tokens.get(3).trim());
                    String investor = tokens.get(4).trim();
                    String method = tokens.get(5).trim();
                    String account = tokens.get(6).trim();
                    String desc = tokens.get(7).trim();
                    String user = tokens.size() > 8 ? tokens.get(8).trim() : "admin";
                    list.add(new CapitalTransaction(id, type, amount, dt, investor, method, account, desc, user));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed capital transaction at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveCapitalTransactions(List<CapitalTransaction> transactions) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("transactionId,type,amount,dateTime,investorName,paymentMethod,destinationAccount,description,createdBy");
        if (transactions != null) {
            for (CapitalTransaction t : transactions) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(t.getTransactionId()),
                        t.getType().name(),
                        t.getAmount().toPlainString(),
                        t.getDateTime().format(DATE_TIME_FMT),
                        escapeCsv(t.getInvestorName()),
                        escapeCsv(t.getPaymentMethod()),
                        escapeCsv(t.getDestinationAccount()),
                        escapeCsv(t.getDescription()),
                        escapeCsv(t.getCreatedBy())
                ));
            }
        }
        atomicWrite(capitalTransactionsFile, lines);
    }

    // =========================================================================
    // Expense Persistence
    // =========================================================================

    @Override
    public synchronized List<Expense> loadExpenses() throws Exception {
        List<Expense> list = new ArrayList<>();
        List<String> lines = readLines(expensesFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 10) {
                try {
                    String id = tokens.get(0).trim();
                    String cat = tokens.get(1).trim();
                    String desc = tokens.get(2).trim();
                    BigDecimal amount = new BigDecimal(tokens.get(3).trim()).setScale(2, RoundingMode.HALF_UP);
                    LocalDate date = LocalDate.parse(tokens.get(4).trim(), DATE_FMT);
                    LocalDateTime created = parseDateTime(tokens.get(5).trim());
                    String method = tokens.get(6).trim();
                    String account = tokens.get(7).trim();
                    String paidTo = tokens.get(8).trim();
                    String ref = tokens.get(9).trim();
                    String user = tokens.size() > 10 ? tokens.get(10).trim() : "admin";
                    String notes = tokens.size() > 11 ? tokens.get(11).trim() : "";
                    list.add(new Expense(id, cat, desc, amount, date, created, method, account, paidTo, ref, user, notes));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed expense record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveExpenses(List<Expense> expenses) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("expenseId,category,description,amount,expenseDate,createdAt,paymentMethod,paymentAccount,paidTo,referenceNumber,createdBy,notes");
        if (expenses != null) {
            for (Expense e : expenses) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(e.getExpenseId()),
                        escapeCsv(e.getCategory()),
                        escapeCsv(e.getDescription()),
                        e.getAmount().toPlainString(),
                        e.getExpenseDate().format(DATE_FMT),
                        e.getCreatedAt().format(DATE_TIME_FMT),
                        escapeCsv(e.getPaymentMethod()),
                        escapeCsv(e.getPaymentAccount()),
                        escapeCsv(e.getPaidTo()),
                        escapeCsv(e.getReferenceNumber()),
                        escapeCsv(e.getCreatedBy()),
                        escapeCsv(e.getNotes())
                ));
            }
        }
        atomicWrite(expensesFile, lines);
    }

    // =========================================================================
    // Account Persistence
    // =========================================================================

    @Override
    public synchronized List<Account> loadAccounts() throws Exception {
        List<Account> list = new ArrayList<>();
        List<String> lines = readLines(accountsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 6) {
                try {
                    String id = tokens.get(0).trim();
                    String name = tokens.get(1).trim();
                    Account.Type type = Account.Type.valueOf(tokens.get(2).trim().toUpperCase());
                    BigDecimal openBal = new BigDecimal(tokens.get(3).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal currBal = new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP);
                    boolean active = Boolean.parseBoolean(tokens.get(5).trim());
                    list.add(new Account(id, name, type, openBal, currBal, active));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed account record at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveAccounts(List<Account> accounts) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("accountId,name,type,openingBalance,currentBalance,active");
        if (accounts != null) {
            for (Account a : accounts) {
                lines.add(String.format("%s,%s,%s,%s,%s,%b",
                        escapeCsv(a.getAccountId()),
                        escapeCsv(a.getName()),
                        a.getType().name(),
                        a.getOpeningBalance().toPlainString(),
                        a.getCurrentBalance().toPlainString(),
                        a.isActive()
                ));
            }
        }
        atomicWrite(accountsFile, lines);
    }

    // =========================================================================
    // AccountTransaction Persistence
    // =========================================================================

    @Override
    public synchronized List<AccountTransaction> loadAccountTransactions() throws Exception {
        List<AccountTransaction> list = new ArrayList<>();
        List<String> lines = readLines(accountTransactionsFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 8) {
                try {
                    String id = tokens.get(0).trim();
                    String accId = tokens.get(1).trim();
                    LocalDateTime dt = parseDateTime(tokens.get(2).trim());
                    AccountTransaction.Type type = AccountTransaction.Type.valueOf(tokens.get(3).trim().toUpperCase());
                    BigDecimal debit = new BigDecimal(tokens.get(4).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal credit = new BigDecimal(tokens.get(5).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal bal = new BigDecimal(tokens.get(6).trim()).setScale(2, RoundingMode.HALF_UP);
                    String ref = tokens.get(7).trim();
                    String desc = tokens.size() > 8 ? tokens.get(8).trim() : "";
                    String user = tokens.size() > 9 ? tokens.get(9).trim() : "admin";
                    list.add(new AccountTransaction(id, accId, dt, type, debit, credit, bal, ref, desc, user));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed account transaction at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void saveAccountTransactions(List<AccountTransaction> transactions) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("transactionId,accountId,dateTime,type,debit,credit,runningBalance,referenceId,description,createdBy");
        if (transactions != null) {
            for (AccountTransaction t : transactions) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(t.getTransactionId()),
                        escapeCsv(t.getAccountId()),
                        t.getDateTime().format(DATE_TIME_FMT),
                        t.getType().name(),
                        t.getDebit().toPlainString(),
                        t.getCredit().toPlainString(),
                        t.getRunningBalance().toPlainString(),
                        escapeCsv(t.getReferenceId()),
                        escapeCsv(t.getDescription()),
                        escapeCsv(t.getCreatedBy())
                ));
            }
        }
        atomicWrite(accountTransactionsFile, lines);
    }

    // =========================================================================
    // PurchaseOrder Persistence
    // =========================================================================

    @Override
    public synchronized List<PurchaseOrder> loadPurchaseOrders() throws Exception {
        List<PurchaseOrder> list = new ArrayList<>();
        List<String> lines = readLines(purchaseOrdersFile);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            List<String> tokens = parseCsvLine(line);
            if (tokens.size() >= 16) {
                try {
                    String id = tokens.get(0).trim();
                    String supplierId = tokens.get(1).trim();
                    String invoiceNo = tokens.get(2).trim();
                    LocalDate orderDate = LocalDate.parse(tokens.get(3).trim(), DATE_FMT);
                    LocalDate receivedDate = !tokens.get(4).trim().isEmpty() ? LocalDate.parse(tokens.get(4).trim(), DATE_FMT) : null;
                    String productId = tokens.get(5).trim();
                    String batchId = tokens.get(6).trim();
                    int qty = Integer.parseInt(tokens.get(7).trim());
                    BigDecimal unitCost = new BigDecimal(tokens.get(8).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal addCost = new BigDecimal(tokens.get(9).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal total = new BigDecimal(tokens.get(10).trim()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal paid = new BigDecimal(tokens.get(11).trim()).setScale(2, RoundingMode.HALF_UP);
                    String method = tokens.get(12).trim();
                    String account = tokens.get(13).trim();
                    LocalDate expiry = !tokens.get(14).trim().isEmpty() ? LocalDate.parse(tokens.get(14).trim(), DATE_FMT) : null;
                    PurchaseOrder.Status status = PurchaseOrder.Status.valueOf(tokens.get(15).trim().toUpperCase());
                    String user = tokens.size() > 16 ? tokens.get(16).trim() : "admin";
                    String notes = tokens.size() > 17 ? tokens.get(17).trim() : "";
                    list.add(new PurchaseOrder(id, supplierId, invoiceNo, orderDate, receivedDate, productId,
                            batchId, qty, unitCost, addCost, total, paid, method, account, expiry, status, user, notes));
                } catch (Exception ex) {
                    System.out.println("Warning: Skipping malformed purchase order at line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }
        return list;
    }

    @Override
    public synchronized void savePurchaseOrders(List<PurchaseOrder> orders) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("purchaseId,supplierId,supplierInvoiceNo,orderDate,receivedDate,productId,batchId,quantity,unitCost,additionalCost,totalAmount,paidAmount,paymentMethod,paymentAccount,expiryDate,status,createdBy,notes");
        if (orders != null) {
            for (PurchaseOrder p : orders) {
                lines.add(String.format("%s,%s,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(p.getPurchaseId()),
                        escapeCsv(p.getSupplierId()),
                        escapeCsv(p.getSupplierInvoiceNo()),
                        p.getOrderDate().format(DATE_FMT),
                        p.getReceivedDate() != null ? p.getReceivedDate().format(DATE_FMT) : "",
                        escapeCsv(p.getProductId()),
                        escapeCsv(p.getBatchId()),
                        p.getQuantity(),
                        p.getUnitCost().toPlainString(),
                        p.getAdditionalCost().toPlainString(),
                        p.getTotalAmount().toPlainString(),
                        p.getPaidAmount().toPlainString(),
                        escapeCsv(p.getPaymentMethod()),
                        escapeCsv(p.getPaymentAccount()),
                        p.getExpiryDate() != null ? p.getExpiryDate().format(DATE_FMT) : "",
                        p.getStatus().name(),
                        escapeCsv(p.getCreatedBy()),
                        escapeCsv(p.getNotes())
                ));
            }
        }
        atomicWrite(purchaseOrdersFile, lines);
    }

    // =========================================================================
    // Backup & Restore Engine
    // =========================================================================

    @Override
    public synchronized File createBackup(File targetDirectory) throws Exception {
        if (targetDirectory == null) {
            targetDirectory = new File(dataDir.getParentFile(), "backups");
        }
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        File backupDir = new File(targetDirectory, "ShelfLife_Backup_" + stamp);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        File[] filesToBackup = {
                productsFile, batchesFile, salesFile, saleItemsFile,
                wastageFile, suppliersFile, usersFile, returnsFile,
                returnItemsFile, activityLogsFile, settingsFile,
                heldBillsFile, heldBillItemsFile, salePaymentsFile,
                customersFile, customerLedgerFile, supplierPayablesFile,
                supplierPaymentsFile, cashierShiftsFile, priceOverridesFile,
                capitalTransactionsFile, expensesFile, accountsFile,
                accountTransactionsFile, purchaseOrdersFile
        };

        File manifest = new File(backupDir, "manifest.txt");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(manifest), StandardCharsets.UTF_8))) {
            pw.println("ShelfLife Database Backup Archive");
            pw.println("Created: " + LocalDateTime.now().format(DATE_TIME_FMT));
            pw.println("Source Directory: " + dataDir.getAbsolutePath());
            pw.println("----------------------------------------");

            for (File src : filesToBackup) {
                if (src.exists()) {
                    File dest = new File(backupDir, src.getName());
                    copyFile(src, dest);
                    pw.println(src.getName() + " -> Copied successfully (" + src.length() + " bytes)");
                }
            }
        }

        return backupDir;
    }

    @Override
    public synchronized void restoreBackup(File backupDirectory) throws Exception {
        if (backupDirectory == null || !backupDirectory.exists() || !backupDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid backup directory: " + (backupDirectory != null ? backupDirectory.getAbsolutePath() : "null"));
        }

        // Validate presence of required data files in backup
        File backupProducts = new File(backupDirectory, "products.csv");
        File backupBatches = new File(backupDirectory, "batches.csv");
        if (!backupProducts.exists() || !backupBatches.exists()) {
            throw new StorageException("Backup directory is invalid or missing critical inventory files (products.csv/batches.csv).");
        }

        // Create safety backup of current live state first
        File safetyDir = new File(dataDir.getParentFile(), "safety_backup_before_restore_" + System.currentTimeMillis());
        createBackup(safetyDir);

        try {
            File[] filesToRestore = {
                    productsFile, batchesFile, salesFile, saleItemsFile,
                    wastageFile, suppliersFile, usersFile, returnsFile,
                    returnItemsFile, activityLogsFile, settingsFile,
                    heldBillsFile, heldBillItemsFile, salePaymentsFile,
                    customersFile, customerLedgerFile, supplierPayablesFile,
                    supplierPaymentsFile, cashierShiftsFile, priceOverridesFile,
                    capitalTransactionsFile, expensesFile, accountsFile,
                    accountTransactionsFile, purchaseOrdersFile
            };

            for (File target : filesToRestore) {
                File source = new File(backupDirectory, target.getName());
                if (source.exists()) {
                    copyFile(source, target);
                }
            }
        } catch (Exception ex) {
            // Roll back to safety backup
            restoreBackup(safetyDir);
            throw new StorageException("Failed during file restore. Rolled back to pre-restore safety snapshot.", ex);
        }
    }

    // =========================================================================
    // Transactional Snapshot Engine
    // =========================================================================

    @Override
    public synchronized void executeTransaction(TransactionOperation operation) throws Exception {
        if (operation == null) return;

        List<File> targets = Arrays.asList(
                productsFile, batchesFile, salesFile, saleItemsFile,
                wastageFile, suppliersFile, usersFile, returnsFile,
                returnItemsFile, activityLogsFile, settingsFile,
                heldBillsFile, heldBillItemsFile, salePaymentsFile,
                customersFile, customerLedgerFile, supplierPayablesFile,
                supplierPaymentsFile, cashierShiftsFile, priceOverridesFile,
                capitalTransactionsFile, expensesFile, accountsFile,
                accountTransactionsFile, purchaseOrdersFile
        );
        Map<File, File> backups = new HashMap<>();

        // Create .bak backup snapshots of existing data files
        for (File f : targets) {
            if (f.exists()) {
                File bak = new File(f.getParentFile(), f.getName() + ".bak");
                copyFile(f, bak);
                backups.put(f, bak);
            }
        }

        try {
            operation.execute();
            // Transaction succeeded: clean up backup snapshots
            for (File bak : backups.values()) {
                if (bak.exists()) {
                    bak.delete();
                }
            }
        } catch (Exception ex) {
            // Transaction failed: roll back all files to previous snapshot state
            for (Map.Entry<File, File> entry : backups.entrySet()) {
                File original = entry.getKey();
                File bak = entry.getValue();
                if (bak.exists()) {
                    try {
                        copyFile(bak, original);
                        bak.delete();
                    } catch (IOException rollbackEx) {
                        System.err.println("Fatal: Failed to restore backup " + bak.getName() + ": " + rollbackEx.getMessage());
                    }
                }
            }
            throw ex;
        }
    }

    // =========================================================================
    // Low-Level File Helpers & Safe CSV Parser
    // =========================================================================

    private void atomicWrite(File file, List<String> lines) throws IOException {
        File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            for (String l : lines) {
                pw.println(l);
            }
        }

        if (file.exists()) {
            if (!file.delete()) {
                System.gc();
                file.delete();
            }
        }

        if (!tempFile.renameTo(file)) {
            copyFile(tempFile, file);
            tempFile.delete();
        }
    }

    private void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private List<String> readLines(File file) throws IOException {
        List<String> list = new ArrayList<>();
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        return list;
    }

    public static List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) return tokens;

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    public static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(text);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(text, DATE_TIME_FMT);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }

    private PaymentMethod parsePaymentMethod(String text) {
        if (text == null) return PaymentMethod.CASH;
        try {
            return PaymentMethod.valueOf(text.trim().toUpperCase());
        } catch (Exception e) {
            for (PaymentMethod m : PaymentMethod.values()) {
                if (m.getDisplayName().equalsIgnoreCase(text.trim())) {
                    return m;
                }
            }
            return PaymentMethod.CASH;
        }
    }

    private PaymentStatus parsePaymentStatus(String text) {
        if (text == null) return PaymentStatus.PAID;
        try {
            return PaymentStatus.valueOf(text.trim().toUpperCase());
        } catch (Exception e) {
            for (PaymentStatus s : PaymentStatus.values()) {
                if (s.getDisplayName().equalsIgnoreCase(text.trim())) {
                    return s;
                }
            }
            return PaymentStatus.PAID;
        }
    }
}
