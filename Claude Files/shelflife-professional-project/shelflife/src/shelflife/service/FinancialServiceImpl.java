package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;
import shelflife.util.CurrencyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Standard implementation of FinancialService.
 * Strictly enforces all commercial accounting rules:
 * 1. Never double-counts stock purchases as expenses.
 * 2. COGS is derived from actual batch costs allocated via FEFO.
 * 3. Cash collected is separated from sales revenue.
 * 4. Net Operating Profit = Gross Profit - Operating Expenses.
 * 5. Inventory Valuation uses remaining batch quantities * batch cost price.
 */
public class FinancialServiceImpl implements FinancialService {

    private final DataStore dataStore;
    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final CapitalService capitalService;
    private final ExpenseService expenseService;
    private final CashAccountService cashAccountService;
    private final CustomerCreditService customerCreditService;
    private final SupplierPayableService supplierPayableService;
    private final PurchaseService purchaseService;

    public FinancialServiceImpl(DataStore dataStore, InventoryService inventoryService,
                                SalesService salesService, CapitalService capitalService,
                                ExpenseService expenseService, CashAccountService cashAccountService,
                                CustomerCreditService customerCreditService,
                                SupplierPayableService supplierPayableService,
                                PurchaseService purchaseService) {
        if (dataStore == null || inventoryService == null) {
            throw new IllegalArgumentException("Core dependencies cannot be null.");
        }
        this.dataStore = dataStore;
        this.inventoryService = inventoryService;
        this.salesService = salesService;
        this.capitalService = capitalService;
        this.expenseService = expenseService;
        this.cashAccountService = cashAccountService;
        this.customerCreditService = customerCreditService;
        this.supplierPayableService = supplierPayableService;
        this.purchaseService = purchaseService;
    }

    @Override
    public void reload() {
        inventoryService.reload();
        if (salesService != null) salesService.reload();
        if (capitalService != null) capitalService.reload();
        if (expenseService != null) expenseService.reload();
        if (cashAccountService != null) cashAccountService.reload();
        if (customerCreditService != null) customerCreditService.reload();
        if (supplierPayableService != null) supplierPayableService.reload();
        if (purchaseService != null) purchaseService.reload();
    }

    @Override
    public synchronized BigDecimal getCurrentInventoryValuation() {
        BigDecimal valuation = BigDecimal.ZERO;
        for (ProductBatch b : inventoryService.getAllBatches()) {
            if (b.getQuantity() > 0 && !b.isExpired() && !b.isRecalled()) {
                BigDecimal unitCost = b.getCostPrice();
                if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0) {
                    valuation = valuation.add(unitCost.multiply(BigDecimal.valueOf(b.getQuantity())));
                }
            }
        }
        return CurrencyUtil.roundMoney(valuation);
    }

    @Override
    public synchronized BigDecimal getCostOfGoodsSold(LocalDate start, LocalDate end) {
        BigDecimal cogs = BigDecimal.ZERO;
        try {
            List<Sale> allSales = dataStore.loadSales();
            Set<String> targetSaleIds = new HashSet<>();
            for (Sale s : allSales) {
                LocalDate d = s.getSaleDateTime().toLocalDate();
                if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                    if (s.getPaymentStatus() == PaymentStatus.PAID) {
                        targetSaleIds.add(s.getSaleId());
                    }
                }
            }

            for (SaleItem item : dataStore.loadSaleItems()) {
                if (targetSaleIds.contains(item.getSaleId())) {
                    BigDecimal unitCost = item.getCostPrice();
                    if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0) {
                        cogs = cogs.add(unitCost.multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to compute COGS: " + e.getMessage());
        }
        return CurrencyUtil.roundMoney(cogs);
    }

    @Override
    public synchronized BigDecimal getNetSalesRevenue(LocalDate start, LocalDate end) {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;

        try {
            for (Sale s : dataStore.loadSales()) {
                LocalDate d = s.getSaleDateTime().toLocalDate();
                if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                    if (s.getPaymentStatus() == PaymentStatus.PAID) {
                        gross = gross.add(s.getTotalAmount());
                    }
                }
            }

            for (SaleReturn r : dataStore.loadReturns()) {
                LocalDate d = r.getReturnDate() != null ? r.getReturnDate().toLocalDate() : LocalDate.now();
                if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                    if (r.getTotalRefund() != null) {
                        refunds = refunds.add(r.getTotalRefund());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to compute Net Sales Revenue: " + e.getMessage());
        }

        BigDecimal net = gross.subtract(refunds);
        return CurrencyUtil.roundMoney(net.max(BigDecimal.ZERO));
    }

    @Override
    public synchronized BigDecimal getGrossProfit(LocalDate start, LocalDate end) {
        BigDecimal netRevenue = getNetSalesRevenue(start, end);
        BigDecimal cogs = getCostOfGoodsSold(start, end);
        return CurrencyUtil.roundMoney(netRevenue.subtract(cogs));
    }

    @Override
    public synchronized BigDecimal getNetOperatingProfit(LocalDate start, LocalDate end) {
        BigDecimal grossProfit = getGrossProfit(start, end);
        BigDecimal operatingExpenses = expenseService != null ? 
                expenseService.getTotalExpensesBetween(start, end) : BigDecimal.ZERO;
        return CurrencyUtil.roundMoney(grossProfit.subtract(operatingExpenses));
    }

    @Override
    public synchronized BigDecimal getCashCollected(LocalDate start, LocalDate end) {
        BigDecimal collected = BigDecimal.ZERO;
        try {
            // Cash from sale payments (excluding store credit)
            for (PaymentLine pl : dataStore.loadPaymentLines()) {
                LocalDate d = pl.getTimestamp() != null ? pl.getTimestamp().toLocalDate() : LocalDate.now();
                if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                    if (pl.getPaymentMethod() != PaymentMethod.STORE_CREDIT) {
                        collected = collected.add(pl.getAmount());
                    }
                }
            }

            // Customer Khata cash collections
            if (customerCreditService != null) {
                for (Customer c : customerCreditService.getAllCustomers()) {
                    for (CustomerLedgerEntry le : customerCreditService.getCustomerLedger(c.getCustomerId())) {
                        LocalDate d = le.getTimestamp().toLocalDate();
                        if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                            if ("PAYMENT".equalsIgnoreCase(le.getTransactionType())) {
                                collected = collected.add(le.getCredit());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to total sales if payment lines not yet migrated
            return getNetSalesRevenue(start, end);
        }
        return CurrencyUtil.roundMoney(collected);
    }

    @Override
    public synchronized BigDecimal getTotalCustomerReceivables() {
        BigDecimal sum = BigDecimal.ZERO;
        if (customerCreditService != null) {
            for (Customer c : customerCreditService.getAllCustomers()) {
                if (c.getCurrentBalance() != null) {
                    sum = sum.add(c.getCurrentBalance());
                }
            }
        }
        return CurrencyUtil.roundMoney(sum);
    }

    @Override
    public synchronized BigDecimal getTotalSupplierPayables() {
        BigDecimal sum = BigDecimal.ZERO;
        if (supplierPayableService != null) {
            for (SupplierPayable p : supplierPayableService.getAllPayables()) {
                if (p.getBalanceDue() != null) {
                    sum = sum.add(p.getBalanceDue());
                }
            }
        }
        return CurrencyUtil.roundMoney(sum);
    }

    @Override
    public synchronized Map<String, Object> getInvestmentRecoverySummary() {
        Map<String, Object> map = new LinkedHashMap<>();

        BigDecimal capitalContributed = capitalService != null ? capitalService.getTotalCapitalContributed() : BigDecimal.ZERO;
        BigDecimal ownerWithdrawals = capitalService != null ? capitalService.getTotalOwnerWithdrawals() : BigDecimal.ZERO;
        BigDecimal netCapital = capitalService != null ? capitalService.getNetOwnerCapital() : BigDecimal.ZERO;

        BigDecimal totalStockPurchased = purchaseService != null ? purchaseService.getTotalPurchaseValue() : BigDecimal.ZERO;
        BigDecimal inventoryAtCost = getCurrentInventoryValuation();
        BigDecimal cogs = getCostOfGoodsSold(null, null);

        BigDecimal netSalesRevenue = getNetSalesRevenue(null, null);
        BigDecimal grossProfit = netSalesRevenue.subtract(cogs);
        BigDecimal expenses = expenseService != null ? expenseService.getTotalExpenses() : BigDecimal.ZERO;
        BigDecimal netProfit = grossProfit.subtract(expenses);

        BigDecimal cashCollected = getCashCollected(null, null);
        BigDecimal customerReceivables = getTotalCustomerReceivables();
        BigDecimal supplierPayables = getTotalSupplierPayables();
        BigDecimal currentCashBalance = cashAccountService != null ? cashAccountService.getTotalCashAndBankBalance() : BigDecimal.ZERO;

        map.put("capitalContributed", capitalContributed);
        map.put("ownerWithdrawals", ownerWithdrawals);
        map.put("netCapital", netCapital);
        map.put("totalStockPurchased", totalStockPurchased);
        map.put("inventoryAtCost", inventoryAtCost);
        map.put("cogs", cogs);
        map.put("netSalesRevenue", netSalesRevenue);
        map.put("grossProfit", grossProfit);
        map.put("operatingExpenses", expenses);
        map.put("netProfit", netProfit);
        map.put("cashCollected", cashCollected);
        map.put("customerReceivables", customerReceivables);
        map.put("supplierPayables", supplierPayables);
        map.put("currentCashBalance", currentCashBalance);

        return map;
    }

    @Override
    public synchronized Map<String, BigDecimal> getCashFlowStatement(LocalDate start, LocalDate end) {
        Map<String, BigDecimal> cf = new LinkedHashMap<>();

        // Operating Cash Flow
        BigDecimal collections = getCashCollected(start, end);
        BigDecimal supplierPayments = BigDecimal.ZERO;
        if (supplierPayableService != null) {
            try {
                for (SupplierPayment sp : dataStore.loadSupplierPayments()) {
                    LocalDate d = sp.getPaymentDate();
                    if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                        supplierPayments = supplierPayments.add(sp.getAmount());
                    }
                }
            } catch (Exception ignored) {}
        }
        BigDecimal operatingExpensesPaid = expenseService != null ? expenseService.getTotalExpensesBetween(start, end) : BigDecimal.ZERO;
        BigDecimal netOperatingCash = collections.subtract(supplierPayments).subtract(operatingExpensesPaid);

        // Financing Cash Flow
        BigDecimal capitalIn = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        if (capitalService != null) {
            for (CapitalTransaction t : capitalService.getAllTransactions()) {
                LocalDate d = t.getDateTime().toLocalDate();
                if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) {
                    if (t.getType() == CapitalTransaction.Type.CONTRIBUTION) {
                        capitalIn = capitalIn.add(t.getAmount());
                    } else if (t.getType() == CapitalTransaction.Type.WITHDRAWAL) {
                        withdrawals = withdrawals.add(t.getAmount());
                    }
                }
            }
        }
        BigDecimal netFinancingCash = capitalIn.subtract(withdrawals);
        BigDecimal netCashMovement = netOperatingCash.add(netFinancingCash);

        cf.put("cashCollections", collections);
        cf.put("supplierPayments", supplierPayments);
        cf.put("operatingExpenses", operatingExpensesPaid);
        cf.put("netOperatingCash", netOperatingCash);
        cf.put("capitalIntroduced", capitalIn);
        cf.put("ownerWithdrawals", withdrawals);
        cf.put("netFinancingCash", netFinancingCash);
        cf.put("netCashMovement", netCashMovement);

        return cf;
    }
}
