package shelflife.ui.reports;

import shelflife.model.*;
import shelflife.service.*;
import shelflife.ui.Refreshable;
import shelflife.ui.UITheme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Dashboard & Commercial Financial Reporting Panel for ShelfLife.
 *
 * Answers the critical retail business questions:
 * - What money entered the business (Capital introduced)?
 * - How much has been spent purchasing inventory?
 * - What is the current inventory asset value at cost?
 * - How much revenue has been generated vs. cash actually collected?
 * - What did sold goods cost (COGS) and what is Gross/Net Profit?
 * - How much is tied up in customer credit (Khata) and owed to suppliers?
 * - What is the actual cash and bank balance?
 */
public class DashboardReportsPanel extends JPanel implements Refreshable {

    private final ReportService reportService;
    private final FinancialService financialService;
    private final CapitalService capitalService;
    private final ExpenseService expenseService;
    private final CashAccountService cashAccountService;

    // 10 Key Financial & Operational Metric Cards
    private JLabel lblCapital;
    private JLabel lblPurchases;
    private JLabel lblInventoryValuation;
    private JLabel lblNetRevenue;
    private JLabel lblCogs;
    private JLabel lblGrossProfit;
    private JLabel lblNetProfit;
    private JLabel lblExpenses;
    private JLabel lblCustomerCredit;
    private JLabel lblCashBalance;

    // Reporting Tables
    private JTable tblSales;
    private DefaultTableModel modelSales;
    private JTable tblStock;
    private DefaultTableModel modelStock;
    private JTable tblWastage;
    private DefaultTableModel modelWastage;
    private JTable tblReturns;
    private DefaultTableModel modelReturns;
    private JTable tblCapital;
    private DefaultTableModel modelCapital;
    private JTable tblExpenses;
    private DefaultTableModel modelExpenses;
    private JTable tblCashFlow;
    private DefaultTableModel modelCashFlow;

    // Investment Recovery summary labels
    private JTextArea txtInvestmentRecovery;

    public DashboardReportsPanel(ReportService reportService) {
        this(reportService, null, null, null, null);
    }

    public DashboardReportsPanel(ReportService reportService, FinancialService financialService,
                                 CapitalService capitalService, ExpenseService expenseService,
                                 CashAccountService cashAccountService) {
        if (reportService == null) {
            throw new IllegalArgumentException("ReportService cannot be null.");
        }
        this.reportService = reportService;
        this.financialService = financialService;
        this.capitalService = capitalService;
        this.expenseService = expenseService;
        this.cashAccountService = cashAccountService;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(12, 15, 12, 15));
        setBackground(UITheme.BG_PAGE);

        initUI();
        refreshData();
    }

    private void initUI() {
        // Top Page header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Commercial Retail Dashboard & Business Finance");
        title.setFont(UITheme.FONT_PAGE_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel subtitle = new JLabel("Live Store KPIs • Real Batch COGS • Investment Recovery • Cash Flow • Ledger Integrity");
        subtitle.setFont(UITheme.FONT_SUBTITLE);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBox.setOpaque(false);

        JButton btnRecordExpense = UITheme.createSecondaryButton("+ Record Expense");
        btnRecordExpense.addActionListener(e -> showAddExpenseDialog());
        btnBox.add(btnRecordExpense);

        JButton btnAddCapital = UITheme.createSecondaryButton("+ Capital / Invest");
        btnAddCapital.addActionListener(e -> showAddCapitalDialog());
        btnBox.add(btnAddCapital);

        JButton btnRefresh = UITheme.createPrimaryButton("Refresh Dashboard");
        btnRefresh.addActionListener(e -> refreshData());
        btnBox.add(btnRefresh);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(btnBox, BorderLayout.EAST);
        headerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(0, 0, 10, 0)
        ));

        // 10 Prominent Commercial KPI Metric Cards (2 rows of 5)
        JPanel cardsPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        cardsPanel.setOpaque(false);

        lblCapital            = createKpiCard(cardsPanel, "Net Capital",         "Rs. 0.00", UITheme.PRIMARY);
        lblPurchases          = createKpiCard(cardsPanel, "Stock Purchases",     "Rs. 0.00", UITheme.PRIMARY);
        lblInventoryValuation = createKpiCard(cardsPanel, "Inventory at Cost",   "Rs. 0.00", new Color(0x0284C7)); // Sky blue
        lblNetRevenue         = createKpiCard(cardsPanel, "Net Revenue",         "Rs. 0.00", new Color(0x2563EB)); // Royal blue
        lblCogs               = createKpiCard(cardsPanel, "COGS (Cost of Sales)","Rs. 0.00", new Color(0xD97706)); // Amber
        lblGrossProfit        = createKpiCard(cardsPanel, "Gross Profit",        "Rs. 0.00", UITheme.SUCCESS);
        lblExpenses           = createKpiCard(cardsPanel, "Operating Expenses",  "Rs. 0.00", UITheme.WARNING);
        lblNetProfit          = createKpiCard(cardsPanel, "Net Operating Profit","Rs. 0.00", UITheme.SUCCESS);
        lblCustomerCredit     = createKpiCard(cardsPanel, "Customer Khata Due",  "Rs. 0.00", new Color(0xDC2626)); // Red
        lblCashBalance        = createKpiCard(cardsPanel, "Cash & Bank Balance", "Rs. 0.00", new Color(0x059669)); // Emerald

        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(cardsPanel, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // Tabbed reporting workspace
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);

        // 1. Investment Recovery & Capital Analysis Tab
        tabbedPane.addTab("Investment & Capital Recovery", createInvestmentRecoveryTab());

        // 2. Sales Tab
        JPanel pnlSales = new JPanel(new BorderLayout(10, 10));
        pnlSales.setOpaque(false);
        pnlSales.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelSales = new DefaultTableModel(new String[]{"Sale ID", "Date & Time", "Total Amount", "Payment Method", "Status"}, 0);
        tblSales = createStyledTable(modelSales);
        pnlSales.add(new JScrollPane(tblSales), BorderLayout.CENTER);
        pnlSales.add(createExportButton(tblSales, "sales_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Sales History", pnlSales);

        // 3. Inventory & Batch Valuation Tab
        JPanel pnlStock = new JPanel(new BorderLayout(10, 10));
        pnlStock.setOpaque(false);
        pnlStock.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelStock = new DefaultTableModel(new String[]{"Batch ID", "Product ID", "Product Name", "Quantity", "Cost Price", "Batch Value", "Expiry Date", "Status"}, 0);
        tblStock = createStyledTable(modelStock);
        pnlStock.add(new JScrollPane(tblStock), BorderLayout.CENTER);
        pnlStock.add(createExportButton(tblStock, "inventory_valuation_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Inventory Valuation", pnlStock);

        // 4. Operating Expenses Tab
        JPanel pnlExp = new JPanel(new BorderLayout(10, 10));
        pnlExp.setOpaque(false);
        pnlExp.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelExpenses = new DefaultTableModel(new String[]{"Expense ID", "Category", "Description", "Amount", "Date", "Paid Via", "Paid To"}, 0);
        tblExpenses = createStyledTable(modelExpenses);
        pnlExp.add(new JScrollPane(tblExpenses), BorderLayout.CENTER);
        pnlExp.add(createExportButton(tblExpenses, "expenses_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Operating Expenses", pnlExp);

        // 5. Cash Flow & Accounts Tab
        JPanel pnlCashFlow = new JPanel(new BorderLayout(10, 10));
        pnlCashFlow.setOpaque(false);
        pnlCashFlow.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelCashFlow = new DefaultTableModel(new String[]{"Cash Flow Category / Item", "Inflow (Debit)", "Outflow (Credit)", "Net Flow (PKR)"}, 0);
        tblCashFlow = createStyledTable(modelCashFlow);
        pnlCashFlow.add(new JScrollPane(tblCashFlow), BorderLayout.CENTER);
        pnlCashFlow.add(createExportButton(tblCashFlow, "cash_flow_statement.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Cash Flow Statement", pnlCashFlow);

        // 6. Capital Ledger Tab
        JPanel pnlCap = new JPanel(new BorderLayout(10, 10));
        pnlCap.setOpaque(false);
        pnlCap.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelCapital = new DefaultTableModel(new String[]{"Trans ID", "Type", "Amount", "Date & Time", "Investor", "Method", "Account", "Description"}, 0);
        tblCapital = createStyledTable(modelCapital);
        pnlCap.add(new JScrollPane(tblCapital), BorderLayout.CENTER);
        pnlCap.add(createExportButton(tblCapital, "capital_ledger_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Owner Capital Ledger", pnlCap);

        // 7. Wastage Tab
        JPanel pnlWastage = new JPanel(new BorderLayout(10, 10));
        pnlWastage.setOpaque(false);
        pnlWastage.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelWastage = new DefaultTableModel(new String[]{"Wastage ID", "Batch ID", "Quantity Discarded", "Date & Time", "Reason"}, 0);
        tblWastage = createStyledTable(modelWastage);
        pnlWastage.add(new JScrollPane(tblWastage), BorderLayout.CENTER);
        pnlWastage.add(createExportButton(tblWastage, "wastage_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Wastage Log", pnlWastage);

        // 8. Returns Tab
        JPanel pnlReturns = new JPanel(new BorderLayout(10, 10));
        pnlReturns.setOpaque(false);
        pnlReturns.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelReturns = new DefaultTableModel(new String[]{"Return ID", "Original Sale ID", "Date & Time", "Refund Amount", "Processed By", "Reason"}, 0);
        tblReturns = createStyledTable(modelReturns);
        pnlReturns.add(new JScrollPane(tblReturns), BorderLayout.CENTER);
        pnlReturns.add(createExportButton(tblReturns, "returns_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Returns & Refunds", pnlReturns);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createInvestmentRecoveryTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        txtInvestmentRecovery = new JTextArea();
        txtInvestmentRecovery.setEditable(false);
        txtInvestmentRecovery.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtInvestmentRecovery.setBackground(UITheme.CARD_BG);
        txtInvestmentRecovery.setForeground(UITheme.TEXT_MAIN);
        txtInvestmentRecovery.setBorder(new EmptyBorder(14, 16, 14, 16));

        JScrollPane scroll = new JScrollPane(txtInvestmentRecovery);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createKpiCard(JPanel parent, String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(new CompoundBorder(
                new CompoundBorder(
                        new shelflife.ui.theme.Styles.RoundedBorder(UITheme.BORDER, 1, 8),
                        BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor != null ? accentColor : UITheme.PRIMARY)
                ),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(UITheme.TEXT_MUTED);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblVal.setForeground(accentColor != null ? accentColor : UITheme.TEXT_MAIN);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);

        parent.add(card);
        return lblVal;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        UITheme.styleTable(table);
        return table;
    }

    private JButton createExportButton(JTable table, String defaultFilename) {
        JButton btnExport = UITheme.createSecondaryButton("Export to Excel CSV");
        btnExport.addActionListener(e -> exportTableToCSV(table, defaultFilename));
        return btnExport;
    }

    private void showAddCapitalDialog() {
        if (capitalService == null) {
            JOptionPane.showMessageDialog(this, "Capital service is not available.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<CapitalTransaction.Type> cmbType = new JComboBox<>(CapitalTransaction.Type.values());
        JTextField txtAmount = new JTextField();
        JTextField txtInvestor = new JTextField("Owner");
        JComboBox<String> cmbMethod = new JComboBox<>(new String[]{"CASH", "BANK", "EASYPAISA", "JAZZCASH"});
        JTextField txtDesc = new JTextField();

        Object[] fields = {
                "Transaction Type:", cmbType,
                "Amount (PKR):", txtAmount,
                "Investor Name:", txtInvestor,
                "Payment Method:", cmbMethod,
                "Description / Notes:", txtDesc
        };

        int res = JOptionPane.showConfirmDialog(this, fields, "Record Owner Capital Transaction", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amt = new BigDecimal(txtAmount.getText().trim());
                CapitalTransaction.Type type = (CapitalTransaction.Type) cmbType.getSelectedItem();
                if (type == CapitalTransaction.Type.CONTRIBUTION) {
                    capitalService.recordContribution(amt, txtInvestor.getText().trim(), (String) cmbMethod.getSelectedItem(), "MAIN_DRAWER", txtDesc.getText().trim(), "admin");
                    if (cashAccountService != null) {
                        cashAccountService.recordInflow("ACC-01", amt, AccountTransaction.Type.CAPITAL_IN, "", "Capital Contribution by " + txtInvestor.getText().trim(), "admin");
                    }
                } else {
                    capitalService.recordWithdrawal(amt, txtInvestor.getText().trim(), (String) cmbMethod.getSelectedItem(), "MAIN_DRAWER", txtDesc.getText().trim(), "admin");
                    if (cashAccountService != null) {
                        cashAccountService.recordOutflow("ACC-01", amt, AccountTransaction.Type.OWNER_WITHDRAWAL, "", "Owner Withdrawal by " + txtInvestor.getText().trim(), "admin");
                    }
                }
                JOptionPane.showMessageDialog(this, "Capital transaction recorded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to record transaction: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddExpenseDialog() {
        if (expenseService == null) {
            JOptionPane.showMessageDialog(this, "Expense service is not available.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> cmbCat = new JComboBox<>(new String[]{"RENT", "UTILITIES", "SALARIES", "TRANSPORT", "PACKAGING", "MAINTENANCE", "MARKETING", "OTHER"});
        JTextField txtDesc = new JTextField();
        JTextField txtAmount = new JTextField();
        JTextField txtPaidTo = new JTextField();
        JComboBox<String> cmbMethod = new JComboBox<>(new String[]{"CASH", "BANK", "EASYPAISA", "JAZZCASH"});
        JTextField txtRef = new JTextField();

        Object[] fields = {
                "Expense Category:", cmbCat,
                "Description:", txtDesc,
                "Amount (PKR):", txtAmount,
                "Paid To:", txtPaidTo,
                "Payment Method:", cmbMethod,
                "Invoice / Receipt Ref:", txtRef
        };

        int res = JOptionPane.showConfirmDialog(this, fields, "Record Operating Expense", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amt = new BigDecimal(txtAmount.getText().trim());
                String cat = (String) cmbCat.getSelectedItem();
                Expense exp = expenseService.recordExpense(cat, txtDesc.getText().trim(), amt, LocalDate.now(),
                        (String) cmbMethod.getSelectedItem(), "ACC-01", txtPaidTo.getText().trim(), txtRef.getText().trim(), "admin", "");

                if (cashAccountService != null) {
                    cashAccountService.recordOutflow("ACC-01", amt, AccountTransaction.Type.OPERATING_EXPENSE, exp.getExpenseId(), "Expense: " + cat + " - " + txtDesc.getText().trim(), "admin");
                }
                JOptionPane.showMessageDialog(this, "Operating expense recorded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to record expense: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refreshData() {
        try {
            reportService.refreshData();
            if (financialService != null) financialService.reload();
            if (capitalService != null) capitalService.reload();
            if (expenseService != null) expenseService.reload();
            if (cashAccountService != null) cashAccountService.reload();

            // Refresh Financial KPI Cards
            if (financialService != null) {
                Map<String, Object> rec = financialService.getInvestmentRecoverySummary();
                lblCapital.setText(CurrencyUtil.format((BigDecimal) rec.get("netCapital")));
                lblPurchases.setText(CurrencyUtil.format((BigDecimal) rec.get("totalStockPurchased")));
                lblInventoryValuation.setText(CurrencyUtil.format((BigDecimal) rec.get("inventoryAtCost")));
                lblNetRevenue.setText(CurrencyUtil.format((BigDecimal) rec.get("netSalesRevenue")));
                lblCogs.setText(CurrencyUtil.format((BigDecimal) rec.get("cogs")));
                lblGrossProfit.setText(CurrencyUtil.format((BigDecimal) rec.get("grossProfit")));
                lblExpenses.setText(CurrencyUtil.format((BigDecimal) rec.get("operatingExpenses")));
                lblNetProfit.setText(CurrencyUtil.format((BigDecimal) rec.get("netProfit")));
                lblCustomerCredit.setText(CurrencyUtil.format((BigDecimal) rec.get("customerReceivables")));
                lblCashBalance.setText(CurrencyUtil.format((BigDecimal) rec.get("currentCashBalance")));

                renderInvestmentRecoveryText(rec);
            } else {
                lblNetRevenue.setText(CurrencyUtil.format(reportService.getTotalRevenue()));
                lblNetProfit.setText(CurrencyUtil.format(reportService.getTotalProfit()));
            }

            // Populate Sales Table
            modelSales.setRowCount(0);
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Sale s : reportService.getSalesHistory()) {
                modelSales.addRow(new Object[]{
                        s.getSaleId(),
                        s.getSaleDateTime() != null ? s.getSaleDateTime().format(dtFmt) : "",
                        CurrencyUtil.format(s.getTotalAmount()),
                        s.getPaymentMethod(),
                        s.getPaymentStatus()
                });
            }

            // Populate Inventory Valuation Table
            modelStock.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (ProductBatch b : reportService.getStockInformation()) {
                Product p = reportService.getProductById(b.getProductId());
                String pName = (p != null) ? p.getName() : "Unknown";
                BigDecimal cost = b.getCostPrice();
                BigDecimal totalVal = cost.multiply(BigDecimal.valueOf(b.getQuantity()));

                String status = "OK";
                if (b.isExpired(today)) {
                    status = "EXPIRED";
                } else if (!b.getExpiryDate().isAfter(today.plusDays(7))) {
                    status = "NEAR EXPIRY";
                }
                if (b.isRecalled()) {
                    status = "RECALLED";
                }

                modelStock.addRow(new Object[]{
                        b.getBatchId(),
                        b.getProductId(),
                        pName,
                        b.getQuantity(),
                        CurrencyUtil.format(cost),
                        CurrencyUtil.format(totalVal),
                        b.getExpiryDate().toString(),
                        status
                });
            }

            // Populate Operating Expenses Table
            modelExpenses.setRowCount(0);
            if (expenseService != null) {
                for (Expense e : expenseService.getAllExpenses()) {
                    modelExpenses.addRow(new Object[]{
                            e.getExpenseId(),
                            e.getCategory(),
                            e.getDescription(),
                            CurrencyUtil.format(e.getAmount()),
                            e.getExpenseDate().toString(),
                            e.getPaymentMethod(),
                            e.getPaidTo()
                    });
                }
            }

            // Populate Cash Flow Statement Table
            modelCashFlow.setRowCount(0);
            if (financialService != null) {
                Map<String, BigDecimal> cf = financialService.getCashFlowStatement(null, null);
                modelCashFlow.addRow(new Object[]{"Operating: Cash Collections from Sales & Customers", CurrencyUtil.format(cf.get("cashCollections")), "-", CurrencyUtil.format(cf.get("cashCollections"))});
                modelCashFlow.addRow(new Object[]{"Operating: Payments to Suppliers for Stock", "-", CurrencyUtil.format(cf.get("supplierPayments")), "-" + CurrencyUtil.format(cf.get("supplierPayments"))});
                modelCashFlow.addRow(new Object[]{"Operating: Operating Overheads (Rent/Utilities/Salaries)", "-", CurrencyUtil.format(cf.get("operatingExpenses")), "-" + CurrencyUtil.format(cf.get("operatingExpenses"))});
                modelCashFlow.addRow(new Object[]{"NET OPERATING CASH FLOW", "", "", CurrencyUtil.format(cf.get("netOperatingCash"))});
                modelCashFlow.addRow(new Object[]{"------------------------------------------------------", "", "", "-------------------"});
                modelCashFlow.addRow(new Object[]{"Financing: Owner Capital Introduced", CurrencyUtil.format(cf.get("capitalIntroduced")), "-", CurrencyUtil.format(cf.get("capitalIntroduced"))});
                modelCashFlow.addRow(new Object[]{"Financing: Owner Personal Withdrawals", "-", CurrencyUtil.format(cf.get("ownerWithdrawals")), "-" + CurrencyUtil.format(cf.get("ownerWithdrawals"))});
                modelCashFlow.addRow(new Object[]{"NET FINANCING CASH FLOW", "", "", CurrencyUtil.format(cf.get("netFinancingCash"))});
                modelCashFlow.addRow(new Object[]{"======================================================", "", "", "==================="});
                modelCashFlow.addRow(new Object[]{"NET CASH MOVEMENT (CLOSING BALANCE IMPACT)", "", "", CurrencyUtil.format(cf.get("netCashMovement"))});
            }

            // Populate Capital Ledger Table
            modelCapital.setRowCount(0);
            if (capitalService != null) {
                for (CapitalTransaction t : capitalService.getAllTransactions()) {
                    modelCapital.addRow(new Object[]{
                            t.getTransactionId(),
                            t.getType().getDisplayName(),
                            CurrencyUtil.format(t.getAmount()),
                            t.getDateTime().format(dtFmt),
                            t.getInvestorName(),
                            t.getPaymentMethod(),
                            t.getDestinationAccount(),
                            t.getDescription()
                    });
                }
            }

            // Populate Wastage Table
            modelWastage.setRowCount(0);
            for (WastageRecord w : reportService.getWastageHistory()) {
                modelWastage.addRow(new Object[]{
                        w.getWastageId(),
                        w.getBatchId(),
                        w.getQuantity(),
                        w.getWastageDateTime() != null ? w.getWastageDateTime().format(dtFmt) : "",
                        w.getReason()
                });
            }

            // Populate Returns Table
            modelReturns.setRowCount(0);
            for (SaleReturn r : reportService.getReturnHistory()) {
                modelReturns.addRow(new Object[]{
                        r.getReturnId(),
                        r.getOriginalSaleId(),
                        r.getReturnDate() != null ? r.getReturnDate().format(dtFmt) : "",
                        CurrencyUtil.format(r.getTotalRefund()),
                        r.getProcessedBy(),
                        r.getReason()
                });
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to refresh dashboard: " + ex.getMessage(), "Notice", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderInvestmentRecoveryText(Map<String, Object> rec) {
        if (txtInvestmentRecovery == null || rec == null) return;

        BigDecimal capitalContributed = (BigDecimal) rec.get("capitalContributed");
        BigDecimal ownerWithdrawals   = (BigDecimal) rec.get("ownerWithdrawals");
        BigDecimal netCapital         = (BigDecimal) rec.get("netCapital");
        BigDecimal stockPurchased     = (BigDecimal) rec.get("totalStockPurchased");
        BigDecimal inventoryAtCost    = (BigDecimal) rec.get("inventoryAtCost");
        BigDecimal cogs               = (BigDecimal) rec.get("cogs");
        BigDecimal netRevenue         = (BigDecimal) rec.get("netSalesRevenue");
        BigDecimal grossProfit        = (BigDecimal) rec.get("grossProfit");
        BigDecimal expenses           = (BigDecimal) rec.get("operatingExpenses");
        BigDecimal netProfit          = (BigDecimal) rec.get("netProfit");
        BigDecimal cashCollected     = (BigDecimal) rec.get("cashCollected");
        BigDecimal customerKhata      = (BigDecimal) rec.get("customerReceivables");
        BigDecimal supplierPayable    = (BigDecimal) rec.get("supplierPayables");
        BigDecimal cashBalance        = (BigDecimal) rec.get("currentCashBalance");

        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append("                  SHELFLIFE COMMERCIAL INVESTMENT & CAPITAL RECOVERY STATEMENT          \n");
        sb.append("========================================================================================\n\n");

        sb.append(String.format("1. EQUITY & CAPITAL POSITION:\n"));
        sb.append(String.format("   • Total Capital Contributed by Owner:      %15s\n", CurrencyUtil.format(capitalContributed)));
        sb.append(String.format("   • Owner Withdrawals (Personal Drawings): - %15s\n", CurrencyUtil.format(ownerWithdrawals)));
        sb.append(String.format("   ------------------------------------------------------------\n"));
        sb.append(String.format("   • NET CAPITAL INTRODUCED:                  %15s\n\n", CurrencyUtil.format(netCapital)));

        sb.append(String.format("2. INVENTORY INVESTMENT & RECOVERY:\n"));
        sb.append(String.format("   • Total Stock Purchases (Goods In):        %15s\n", CurrencyUtil.format(stockPurchased)));
        sb.append(String.format("   • Inventory Remaining at Cost (Unsold):    %15s\n", CurrencyUtil.format(inventoryAtCost)));
        sb.append(String.format("   • Investment Consumed Through Sales (COGS):%15s\n\n", CurrencyUtil.format(cogs)));

        sb.append(String.format("3. TRADING PERFORMANCE & PROFITABILITY:\n"));
        sb.append(String.format("   • Net Sales Revenue Earned:                %15s\n", CurrencyUtil.format(netRevenue)));
        sb.append(String.format("   • Cost of Goods Sold (COGS):             - %15s\n", CurrencyUtil.format(cogs)));
        sb.append(String.format("   ------------------------------------------------------------\n"));
        sb.append(String.format("   • GROSS PROFIT:                            %15s\n", CurrencyUtil.format(grossProfit)));
        sb.append(String.format("   • Operating Expenses (Rent/Utilities/etc): - %15s\n", CurrencyUtil.format(expenses)));
        sb.append(String.format("   ------------------------------------------------------------\n"));
        sb.append(String.format("   • NET OPERATING PROFIT:                    %15s\n\n", CurrencyUtil.format(netProfit)));

        sb.append(String.format("4. CASH POSITION & RECEIVABLES/PAYABLES:\n"));
        sb.append(String.format("   • Cash Actually Collected to Date:        %15s\n", CurrencyUtil.format(cashCollected)));
        sb.append(String.format("   • Customer Receivables (Khata / Udhaar):   %15s\n", CurrencyUtil.format(customerKhata)));
        sb.append(String.format("   • Supplier Outstanding Payables:           %15s\n", CurrencyUtil.format(supplierPayable)));
        sb.append(String.format("   • Current Total Cash & Bank Balance:       %15s\n\n", CurrencyUtil.format(cashBalance)));

        sb.append("========================================================================================\n");
        sb.append("COMMERCIAL ACCOUNTING EXPLANATION FOR RETAIL OWNER:\n");
        sb.append(String.format("• %s remains safely invested in physical unsold inventory on shelves.\n", CurrencyUtil.format(inventoryAtCost)));
        sb.append(String.format("• %s was the actual landed cost of the stock that was successfully sold.\n", CurrencyUtil.format(cogs)));
        sb.append(String.format("• %s is earned trading profit, not the simple difference between cash in and out.\n", CurrencyUtil.format(netProfit)));
        sb.append(String.format("• %s was collected in liquid cash; %s is still due from customer Khata.\n", CurrencyUtil.format(cashCollected), CurrencyUtil.format(customerKhata)));
        sb.append("• Inventory purchases are capitalized as assets and NEVER double-counted as expenses.\n");
        sb.append("========================================================================================\n");

        txtInvestmentRecovery.setText(sb.toString());
        txtInvestmentRecovery.setCaretPosition(0);
    }

    private void exportTableToCSV(JTable table, String defaultFilename) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(defaultFilename));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                // Write UTF-8 BOM for Microsoft Excel compatibility
                writer.write('\ufeff');

                for (int i = 0; i < table.getColumnCount(); i++) {
                    writer.print(table.getColumnName(i));
                    if (i < table.getColumnCount() - 1) writer.print(",");
                }
                writer.println();

                for (int row = 0; row < table.getRowCount(); row++) {
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        Object val = table.getValueAt(row, col);
                        String strVal = (val != null) ? val.toString() : "";
                        if (strVal.contains(",")) strVal = "\"" + strVal + "\"";
                        writer.print(strVal);
                        if (col < table.getColumnCount() - 1) writer.print(",");
                    }
                    writer.println();
                }

                JOptionPane.showMessageDialog(this, "Exported successfully to " + file.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
