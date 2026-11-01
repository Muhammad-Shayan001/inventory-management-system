package shelflife.ui.billing;

import shelflife.model.*;
import shelflife.service.ReturnsService;
import shelflife.service.SalesService;
import shelflife.service.AuthService;
import shelflife.ui.Refreshable;
import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer Returns and Refund Management Panel for processing partial or full sale returns.
 */
public class ReturnsPanel extends JPanel implements Refreshable {

    private final ReturnsService returnsService;
    private final SalesService salesService;
    private final AuthService authService;

    // Search bar
    private JTextField txtSearchSaleId;
    private JButton btnFindSale;

    // Sale Info Display
    private JLabel lblSaleInfo;
    private DefaultTableModel returnItemsTableModel;
    private JTable tblReturnItems;

    // Action Form
    private JTextField txtReason;
    private JLabel lblTotalRefundVal;
    private JButton btnProcessReturn;
    private JButton btnPrintLastReturn;

    private Sale currentLoadedSale = null;
    private SaleReturn lastProcessedReturn = null;

    public ReturnsPanel(ReturnsService returnsService, SalesService salesService, AuthService authService) {
        this.returnsService = returnsService;
        this.salesService = salesService;
        this.authService = authService;

        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BG_WINDOW);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initComponents();
    }

    private void initComponents() {
        // Top Lookup Bar
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);
        JLabel lblSalePrompt = new JLabel("Enter Sale ID:");
        lblSalePrompt.setFont(Theme.FONT_BOLD);
        searchBar.add(lblSalePrompt);

        txtSearchSaleId = new JTextField(20);
        Styles.styleTextField(txtSearchSaleId);
        txtSearchSaleId.addActionListener(e -> findSale());
        searchBar.add(txtSearchSaleId);

        btnFindSale = Styles.createPrimaryButton("Lookup Transaction");
        btnFindSale.addActionListener(e -> findSale());
        searchBar.add(btnFindSale);

        lblSaleInfo = new JLabel("Search for a completed sale ID (e.g. SALE-...) to process returns.");
        lblSaleInfo.setFont(Theme.FONT_SUBTITLE);
        lblSaleInfo.setForeground(Theme.TEXT_SECONDARY);

        topPanel.add(searchBar, BorderLayout.WEST);
        topPanel.add(lblSaleInfo, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Center Table: Sale Items Available for Return
        String[] cols = {"Product ID", "Product Name", "Batch ID", "Sold Qty", "Previously Returned", "Return Qty", "Unit Price", "Disposition"};
        returnItemsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5 || col == 7; // Only Return Qty and Disposition editable
            }
        };

        tblReturnItems = new JTable(returnItemsTableModel);
        tblReturnItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.styleTable(tblReturnItems);

        int[] retWidths = {85, 150, 95, 75, 140, 85, 85, 100};
        for (int i = 0; i < retWidths.length && i < tblReturnItems.getColumnCount(); i++) {
            tblReturnItems.getColumnModel().getColumn(i).setPreferredWidth(retWidths[i]);
        }

        // Set combo box editor for disposition column
        JComboBox<String> cmbDisp = new JComboBox<>(new String[]{"RESTOCK", "WASTAGE"});
        cmbDisp.setFont(Theme.FONT_REGULAR);
        tblReturnItems.getColumnModel().getColumn(7).setCellEditor(new DefaultCellEditor(cmbDisp));

        returnItemsTableModel.addTableModelListener(e -> recalculateRefundSummary());

        JScrollPane scrollTable = new JScrollPane(tblReturnItems);
        scrollTable.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                "Items Sold in Selected Transaction (Double click 'Return Qty' to specify items to return)",
                TitledBorder.LEFT, TitledBorder.TOP,
                Theme.FONT_SECTION,
                Theme.TEXT_PRIMARY
        ));
        add(scrollTable, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 10));
        bottomBar.setBackground(Theme.BG_WINDOW);
        bottomBar.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel reasonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        reasonPanel.setOpaque(false);
        JLabel lblReason = new JLabel("Return Reason:");
        lblReason.setFont(Theme.FONT_BOLD);
        reasonPanel.add(lblReason);
        txtReason = new JTextField(25);
        Styles.styleTextField(txtReason);
        txtReason.setText("Customer requested refund / exchange");
        reasonPanel.add(txtReason);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actionPanel.setOpaque(false);

        JLabel lblRefundTitle = new JLabel("Total Refund (PKR):");
        lblRefundTitle.setFont(Theme.FONT_BOLD);
        lblRefundTitle.setForeground(Theme.TEXT_PRIMARY);
        lblTotalRefundVal = new JLabel("Rs. 0.00");
        lblTotalRefundVal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotalRefundVal.setForeground(Theme.COLOR_SUCCESS_FG);

        btnProcessReturn = Styles.createDangerButton("Process Refund & Return");
        btnProcessReturn.addActionListener(e -> handleProcessReturn());

        btnPrintLastReturn = Styles.createSecondaryButton("\uD83D\uDDA8  Print Return Receipt");
        btnPrintLastReturn.setEnabled(false);
        btnPrintLastReturn.addActionListener(e -> {
            if (lastProcessedReturn != null) {
                String user = (authService != null && authService.getCurrentUser() != null) ?
                        authService.getCurrentUser().getUsername() : "ADMIN";
                ReturnReceiptDialog dialog = new ReturnReceiptDialog(
                        SwingUtilities.getWindowAncestor(this),
                        lastProcessedReturn,
                        new AppSettings(),
                        user
                );
                dialog.setVisible(true);
            }
        });

        actionPanel.add(lblRefundTitle);
        actionPanel.add(lblTotalRefundVal);
        actionPanel.add(btnProcessReturn);
        actionPanel.add(btnPrintLastReturn);

        bottomBar.add(reasonPanel, BorderLayout.WEST);
        bottomBar.add(actionPanel, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void findSale() {
        String saleId = txtSearchSaleId.getText().trim();
        if (saleId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Sale ID.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Sale sale = returnsService.getSaleForReturn(saleId);
            this.currentLoadedSale = sale;

            lblSaleInfo.setText(String.format("Loaded: %s | Date: %s | Total: %s | Payment: %s",
                    sale.getSaleId(),
                    sale.getSaleDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    CurrencyUtil.formatPKR(sale.getTotalAmount()),
                    sale.getPaymentMethod().getDisplayName()
            ));

            populateSaleItemsTable(sale);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not locate sale: " + ex.getMessage(), "Sale Not Found", JOptionPane.ERROR_MESSAGE);
            currentLoadedSale = null;
            returnItemsTableModel.setRowCount(0);
            lblSaleInfo.setText("Sale not found.");
        }
    }

    private void populateSaleItemsTable(Sale sale) {
        returnItemsTableModel.setRowCount(0);

        // Fetch previously returned amounts
        List<SaleReturn> previousReturns = returnsService.getReturnsForSale(sale.getSaleId());
        Map<String, Integer> previouslyReturnedMap = new HashMap<>();
        try {
            List<SaleReturnItem> allReturnItems = returnsService.getAllReturns().isEmpty() ? new ArrayList<>() :
                    new ArrayList<>();
            // Compute from returnsService directly
            for (SaleReturn pr : previousReturns) {
                for (SaleReturnItem ri : pr.getItems()) {
                    String key = ri.getProductId() + "#" + ri.getBatchId();
                    previouslyReturnedMap.put(key, previouslyReturnedMap.getOrDefault(key, 0) + ri.getQuantity());
                }
            }
        } catch (Exception ignored) {}

        for (SaleItem it : sale.getItems()) {
            String key = it.getProductId() + "#" + it.getBatchId();
            int previouslyReturned = previouslyReturnedMap.getOrDefault(key, 0);

            returnItemsTableModel.addRow(new Object[]{
                    it.getProductId(),
                    it.getProductName(),
                    it.getBatchId(),
                    it.getQuantity(),
                    previouslyReturned,
                    0, // Default return qty to 0
                    CurrencyUtil.formatPKR(it.getUnitPrice()),
                    "RESTOCK"
            });
        }
        recalculateRefundSummary();
    }

    private void recalculateRefundSummary() {
        BigDecimal totalRefund = BigDecimal.ZERO;
        for (int i = 0; i < returnItemsTableModel.getRowCount(); i++) {
            try {
                Object qtyObj = returnItemsTableModel.getValueAt(i, 5);
                int returnQty = Integer.parseInt(qtyObj.toString().trim());
                if (returnQty > 0) {
                    String priceStr = returnItemsTableModel.getValueAt(i, 6).toString().replace("Rs.", "").replace(",", "").trim();
                    BigDecimal price = new BigDecimal(priceStr);
                    totalRefund = totalRefund.add(price.multiply(BigDecimal.valueOf(returnQty)));
                }
            } catch (Exception ignored) {}
        }
        lblTotalRefundVal.setText(CurrencyUtil.formatPKR(totalRefund));
    }

    private void handleProcessReturn() {
        if (currentLoadedSale == null) {
            JOptionPane.showMessageDialog(this, "Please search for and load a sale first.", "No Sale Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<SaleReturnItem> itemsToReturn = new ArrayList<>();
        for (int i = 0; i < returnItemsTableModel.getRowCount(); i++) {
            try {
                int returnQty = Integer.parseInt(returnItemsTableModel.getValueAt(i, 5).toString().trim());
                if (returnQty > 0) {
                    String prodId = (String) returnItemsTableModel.getValueAt(i, 0);
                    String prodName = (String) returnItemsTableModel.getValueAt(i, 1);
                    String batchId = (String) returnItemsTableModel.getValueAt(i, 2);
                    String priceStr = returnItemsTableModel.getValueAt(i, 6).toString().replace("Rs.", "").replace(",", "").trim();
                    BigDecimal price = new BigDecimal(priceStr);
                    String disp = (String) returnItemsTableModel.getValueAt(i, 7);

                    SaleReturnItem item = new SaleReturnItem(null, null, prodId, prodName, batchId, returnQty, price, null, disp);
                    itemsToReturn.add(item);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid return quantity at row " + (i + 1), "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (itemsToReturn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Return Qty greater than zero for at least one item.", "No Items Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            reason = "Customer refund";
        }

        String user = (authService != null && authService.getCurrentUser() != null) ?
                authService.getCurrentUser().getUsername() : "ADMIN";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to process this refund for " + lblTotalRefundVal.getText() + "?\nThis action will update inventory records.",
                "Confirm Return Transaction",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                SaleReturn result = returnsService.processReturn(currentLoadedSale.getSaleId(), itemsToReturn, reason, user);
                lastProcessedReturn = result;
                if (btnPrintLastReturn != null) {
                    btnPrintLastReturn.setEnabled(true);
                }
                JOptionPane.showMessageDialog(this,
                        "Return " + result.getReturnId() + " successfully processed!\nRefund Amount: " + CurrencyUtil.formatPKR(result.getTotalRefund()),
                        "Return Success", JOptionPane.INFORMATION_MESSAGE);

                ReturnReceiptDialog retDialog = new ReturnReceiptDialog(
                        SwingUtilities.getWindowAncestor(this),
                        result,
                        new AppSettings(),
                        user
                );
                retDialog.setVisible(true);

                findSale(); // reload
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to process return: " + ex.getMessage(), "Return Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void refreshData() {
        if (currentLoadedSale != null) {
            findSale();
        }
    }
}
