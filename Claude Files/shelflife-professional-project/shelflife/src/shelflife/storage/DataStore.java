package shelflife.storage;

import shelflife.model.*;

import java.io.File;
import java.util.List;

/**
 * Storage contract for ShelfLife local persistence.
 * All service operations persist through this interface with rollback protection.
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

    List<Supplier> loadSuppliers() throws Exception;
    void saveSuppliers(List<Supplier> suppliers) throws Exception;

    List<User> loadUsers() throws Exception;
    void saveUsers(List<User> users) throws Exception;

    List<SaleReturn> loadReturns() throws Exception;
    void saveReturns(List<SaleReturn> returns) throws Exception;

    List<SaleReturnItem> loadReturnItems() throws Exception;
    void saveReturnItems(List<SaleReturnItem> items) throws Exception;

    List<ActivityLogEntry> loadActivityLogs() throws Exception;
    void saveActivityLogs(List<ActivityLogEntry> logs) throws Exception;

    AppSettings loadSettings() throws Exception;
    void saveSettings(AppSettings settings) throws Exception;

    List<HeldBill> loadHeldBills() throws Exception;
    void saveHeldBills(List<HeldBill> bills) throws Exception;

    List<HeldBillItem> loadHeldBillItems() throws Exception;
    void saveHeldBillItems(List<HeldBillItem> items) throws Exception;

    List<PaymentLine> loadPaymentLines() throws Exception;
    void savePaymentLines(List<PaymentLine> lines) throws Exception;

    List<Customer> loadCustomers() throws Exception;
    void saveCustomers(List<Customer> customers) throws Exception;

    List<CustomerLedgerEntry> loadCustomerLedger() throws Exception;
    void saveCustomerLedger(List<CustomerLedgerEntry> entries) throws Exception;

    List<SupplierPayable> loadSupplierPayables() throws Exception;
    void saveSupplierPayables(List<SupplierPayable> payables) throws Exception;

    List<SupplierPayment> loadSupplierPayments() throws Exception;
    void saveSupplierPayments(List<SupplierPayment> payments) throws Exception;

    List<CashierShift> loadCashierShifts() throws Exception;
    void saveCashierShifts(List<CashierShift> shifts) throws Exception;

    List<PriceOverrideLog> loadPriceOverrides() throws Exception;
    void savePriceOverrides(List<PriceOverrideLog> overrides) throws Exception;

    List<CapitalTransaction> loadCapitalTransactions() throws Exception;
    void saveCapitalTransactions(List<CapitalTransaction> transactions) throws Exception;

    List<Expense> loadExpenses() throws Exception;
    void saveExpenses(List<Expense> expenses) throws Exception;

    List<Account> loadAccounts() throws Exception;
    void saveAccounts(List<Account> accounts) throws Exception;

    List<AccountTransaction> loadAccountTransactions() throws Exception;
    void saveAccountTransactions(List<AccountTransaction> transactions) throws Exception;

    List<PurchaseOrder> loadPurchaseOrders() throws Exception;
    void savePurchaseOrders(List<PurchaseOrder> orders) throws Exception;

    File createBackup(File targetDirectory) throws Exception;
    void restoreBackup(File backupDirectory) throws Exception;

    /**
     * Executes an operation within an atomic transaction.
     * Backs up storage files prior to execution and automatically rolls back
     * if any exception occurs.
     */
    void executeTransaction(TransactionOperation operation) throws Exception;

    /**
     * Returns the base directory where data files are stored.
     */
    File getDataDirectory();
}
