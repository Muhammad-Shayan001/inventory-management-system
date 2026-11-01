package shelflife.service;

import shelflife.model.*;
import shelflife.storage.DataStore;
import shelflife.util.CurrencyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Service contract and implementation for business financial metrics,
 * Investment Recovery, Profit & Loss, Inventory Valuation, and Cash Flow statement.
 */
public interface FinancialService {

    /**
     * Calculates the current cost value of unsold inventory: Sum(batch.quantity * batch.costPrice).
     * Only non-expired, non-recalled stock is valued at full cost.
     */
    BigDecimal getCurrentInventoryValuation();

    /**
     * Calculates total Cost of Goods Sold (COGS) for a given date window.
     * Uses actual batch cost stored in SaleItem at checkout time.
     */
    BigDecimal getCostOfGoodsSold(LocalDate start, LocalDate end);

    /**
     * Gross Sales - Discounts - Refunds.
     */
    BigDecimal getNetSalesRevenue(LocalDate start, LocalDate end);

    /**
     * Net Sales Revenue - COGS.
     */
    BigDecimal getGrossProfit(LocalDate start, LocalDate end);

    /**
     * Gross Profit - Operating Expenses.
     */
    BigDecimal getNetOperatingProfit(LocalDate start, LocalDate end);

    /**
     * Cash actually collected from sales and customer khata in period.
     */
    BigDecimal getCashCollected(LocalDate start, LocalDate end);

    /**
     * Outstanding customer receivables (Khata).
     */
    BigDecimal getTotalCustomerReceivables();

    /**
     * Outstanding supplier payables.
     */
    BigDecimal getTotalSupplierPayables();

    /**
     * Complete Investment & Capital Recovery Summary.
     */
    Map<String, Object> getInvestmentRecoverySummary();

    /**
     * Multi-period Cash Flow breakdown (Operating, Investing, Financing).
     */
    Map<String, BigDecimal> getCashFlowStatement(LocalDate start, LocalDate end);

    void reload();
}
