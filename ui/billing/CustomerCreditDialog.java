package shelflife.ui.billing;

import shelflife.model.Customer;
import shelflife.model.CustomerLedgerEntry;
import shelflife.model.PaymentMethod;
import shelflife.service.CustomerCreditService;
import shelflife.ui.ToastNotification;
import shelflife.ui.theme.Theme;
import shelflife.util.CsvExporter;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Customer Credit & Udhaar (Khata) Management Dialog.
 * Enables searching customer accounts, reviewing ledger audit trails,
 * recording incoming payments, and setting credit limits.
 */
public class CustomerCreditDialog extends JDialog {

    private final CustomerCreditService creditService;
    private final String currentCashier;

    private final DefaultTableModel custModel;
    private final JTable tblCustomers;
    private final DefaultTableModel ledgerModel;
    private final JTable tblLedger;
    private final JTextField txtSearch;
    private final JLabel lblSelectedInfo;

    private Customer selectedCustomer = null;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    public CustomerCreditDialog(Frame parent, CustomerCreditService creditService, String currentCashier) {
        super(parent, "Customer Credit Ledger (Khata / Udhaar)", true);
        this.creditService = creditService;
        this.currentCashier = currentCashier != null ? currentCashier : "cashier";

        setSize(960, 620);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Initialize Table Models early
        String[] custCols = {"ID", "Name", "Phone", "Credit Limit", "Balance Due", "Status"};
        custModel = new DefaultTableModel(custCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCustomers = new JTable(custModel);
        tblCustomers.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tblCustomers.setFont(Theme.FONT_TABLE_CELL);
        tblCustomers.getTableHeader().setFont(Theme.FONT_TABLE_HEADER);

        String[] ledgerCols = {"Date/Time", "Type", "Ref", "Debit (+)", "Credit (-)", "Running Bal", "Notes"};
        ledgerModel = new DefaultTableModel(ledgerCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLedger = new JTable(ledgerModel);
        tblLedger.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tblLedger.setFont(Theme.FONT_TABLE_CELL);
        tblLedger.getTableHeader().setFont(Theme.FONT_TABLE_HEADER);

        // Top Toolbar
        JPanel topPanel = new JPanel(new BorderLayout(10, 6));
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(14, 18, 10, 18));

        JLabel lblTitle = new JLabel("Customer Credit Accounts (Khata)");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);
        JLabel lblSearch = new JLabel("Search Customer:");
        lblSearch.setFont(Theme.FONT_BOLD);
        txtSearch = new JTextField(20);
        txtSearch.setFont(Theme.FONT_REGULAR);
        txtSearch.addCaretListener(e -> refreshCustomerTable());

        JButton btnAddCust = new JButton("+ New Customer");
        btnAddCust.setFont(Theme.FONT_BOLD);
        btnAddCust.setBackground(Theme.ACCENT_PRIMARY);
        btnAddCust.setForeground(Color.WHITE);
        btnAddCust.addActionListener(e -> showAddCustomerDialog());

        JButton btnExport = new JButton("Export Ledger to Excel");
        btnExport.setFont(Theme.FONT_REGULAR);
        btnExport.addActionListener(e -> CsvExporter.exportWithFileChooser(this, tblLedger, "Customer_Ledger_Export.csv"));

        searchBar.add(lblSearch);
        searchBar.add(txtSearch);
        searchBar.add(btnAddCust);
        searchBar.add(btnExport);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(searchBar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Center Split: Left = Customer Table, Right = Selected Customer Ledger
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.48);

        tblCustomers.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onCustomerSelected();
            }
        });

        JScrollPane scrollCust = new JScrollPane(tblCustomers);
        scrollCust.setBorder(BorderFactory.createTitledBorder("Customer Accounts"));
        splitPane.setLeftComponent(scrollCust);

        // Right: Customer Details & Ledger Table
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);
        lblSelectedInfo = new JLabel("Select a customer from the left to view ledger");
        lblSelectedInfo.setFont(Theme.FONT_SECTION);
        lblSelectedInfo.setForeground(Theme.TEXT_PRIMARY);

        JButton btnPay = new JButton("Receive Payment");
        btnPay.setFont(Theme.FONT_BOLD);
        btnPay.setBackground(new Color(0x166534)); // Dark green
        btnPay.setForeground(Color.WHITE);
        btnPay.addActionListener(e -> showPaymentDialog());

        detailHeader.add(lblSelectedInfo, BorderLayout.CENTER);
        detailHeader.add(btnPay, BorderLayout.EAST);
        rightPanel.add(detailHeader, BorderLayout.NORTH);

        JScrollPane scrollLedger = new JScrollPane(tblLedger);
        scrollLedger.setBorder(BorderFactory.createTitledBorder("Transaction Audit Trail"));
        rightPanel.add(scrollLedger, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnClose = new JButton("Close");
        btnClose.setFont(Theme.FONT_REGULAR);
        btnClose.addActionListener(e -> dispose());
        bottomBar.add(btnClose);
        add(bottomBar, BorderLayout.SOUTH);

        refreshCustomerTable();
    }

    private void refreshCustomerTable() {
        custModel.setRowCount(0);
        String q = txtSearch.getText().trim();
        List<Customer> list = creditService.searchCustomers(q);
        for (Customer c : list) {
            custModel.addRow(new Object[]{
                    c.getCustomerId(),
                    c.getFullName(),
                    c.getPhoneNumber(),
                    CurrencyUtil.format(c.getCreditLimit()),
                    CurrencyUtil.format(c.getCurrentBalance()),
                    c.isActive() ? "Active" : "Inactive"
            });
        }
    }

    private void onCustomerSelected() {
        int row = tblCustomers.getSelectedRow();
        if (row < 0) {
            selectedCustomer = null;
            lblSelectedInfo.setText("Select a customer from the left to view ledger");
            ledgerModel.setRowCount(0);
            return;
        }

        String custId = (String) custModel.getValueAt(row, 0);
        selectedCustomer = creditService.getCustomer(custId);
        if (selectedCustomer != null) {
            lblSelectedInfo.setText("<html><b>" + selectedCustomer.getFullName() + "</b> (Bal: " + 
                    CurrencyUtil.format(selectedCustomer.getCurrentBalance()) + " / Avail: " + 
                    CurrencyUtil.format(selectedCustomer.getAvailableCredit()) + ")</html>");

            ledgerModel.setRowCount(0);
            List<CustomerLedgerEntry> entries = creditService.getCustomerLedger(selectedCustomer.getCustomerId());
            for (CustomerLedgerEntry le : entries) {
                ledgerModel.addRow(new Object[]{
                        le.getTimestamp().format(DT_FMT),
                        le.getTransactionType(),
                        le.getReferenceId(),
                        le.getDebit().compareTo(BigDecimal.ZERO) > 0 ? CurrencyUtil.format(le.getDebit()) : "-",
                        le.getCredit().compareTo(BigDecimal.ZERO) > 0 ? CurrencyUtil.format(le.getCredit()) : "-",
                        CurrencyUtil.format(le.getRunningBalance()),
                        le.getNotes()
                });
            }
        }
    }

    private void showAddCustomerDialog() {
        JTextField txtId = new JTextField("CUST-" + (int)(Math.random() * 9000 + 1000));
        JTextField txtName = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtAddr = new JTextField();
        JTextField txtLimit = new JTextField("10000.00");

        Object[] msg = {
                "Customer ID:", txtId,
                "Full Name:", txtName,
                "Phone Number:", txtPhone,
                "Address:", txtAddr,
                "Credit Limit (PKR):", txtLimit
        };

        int res = JOptionPane.showConfirmDialog(this, msg, "Register New Khata Customer", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                BigDecimal limit = new BigDecimal(txtLimit.getText().trim());
                Customer c = new Customer(txtId.getText().trim(), txtName.getText().trim(),
                        txtPhone.getText().trim(), txtAddr.getText().trim(), limit, BigDecimal.ZERO, true, java.time.LocalDate.now());
                creditService.addCustomer(c);
                ToastNotification.show(this, "Customer '" + c.getFullName() + "' added successfully.", ToastNotification.Type.SUCCESS);
                refreshCustomerTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to create customer: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showPaymentDialog() {
        if (selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "Please select a customer first.", "No Customer Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtAmount = new JTextField();
        JComboBox<PaymentMethod> cmbMethod = new JComboBox<>(new PaymentMethod[]{
                PaymentMethod.CASH, PaymentMethod.EASYPAISA, PaymentMethod.JAZZCASH, PaymentMethod.DEBIT_CARD, PaymentMethod.CREDIT_CARD
        });
        JTextField txtRef = new JTextField();
        JTextField txtNotes = new JTextField();

        Object[] msg = {
                "Customer: " + selectedCustomer.getFullName(),
                "Current Balance Due: " + CurrencyUtil.format(selectedCustomer.getCurrentBalance()),
                "Payment Amount (PKR):", txtAmount,
                "Payment Method:", cmbMethod,
                "Transaction Reference #:", txtRef,
                "Notes / Memo:", txtNotes
        };

        int res = JOptionPane.showConfirmDialog(this, msg, "Receive Khata Payment", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                BigDecimal amt = new BigDecimal(txtAmount.getText().trim());
                PaymentMethod pm = (PaymentMethod) cmbMethod.getSelectedItem();
                CustomerLedgerEntry entry = creditService.recordPayment(selectedCustomer.getCustomerId(),
                        amt, pm, txtRef.getText().trim(), txtNotes.getText().trim(), currentCashier);

                ToastNotification.show(this, "Payment of " + CurrencyUtil.format(amt) + " recorded successfully.", ToastNotification.Type.SUCCESS);
                refreshCustomerTable();
                onCustomerSelected();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Payment error: " + ex.getMessage(), "Payment Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
