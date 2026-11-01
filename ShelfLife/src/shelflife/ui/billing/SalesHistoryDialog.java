package shelflife.ui.billing;

import shelflife.model.AppSettings;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.service.SalesService;
import shelflife.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.math.BigDecimal;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal dialog for inspecting past sales records and re-printing itemized receipts.
 */
public class SalesHistoryDialog extends JDialog {

    private final SalesService salesService;
    private JTable tblSales;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtSearch;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SalesHistoryDialog(Window owner, SalesService salesService) {
        super(owner, "Sales Transaction History", ModalityType.APPLICATION_MODAL);
        this.salesService = salesService;

        initComponents();
        loadSalesData();
        setSize(800, 500);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UITheme.BG_PAGE);

        // Top Filter Bar
        JPanel pnlTop = new JPanel(new BorderLayout(8, 8));
        pnlTop.setOpaque(false);
        pnlTop.setBorder(new EmptyBorder(12, 12, 6, 12));

        JLabel lblSearch = new JLabel("Filter History (ID/Payment):");
        lblSearch.setFont(UITheme.FONT_BOLD);
        pnlTop.add(lblSearch, BorderLayout.WEST);

        txtSearch = new JTextField();
        UITheme.styleTextField(txtSearch);
        txtSearch.setToolTipText("Type sale ID or payment method to filter...");
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyFilter();
            }
        });
        pnlTop.add(txtSearch, BorderLayout.CENTER);

        JButton btnReload = UITheme.createSecondaryButton("Reload");
        btnReload.addActionListener(e -> {
            salesService.reload();
            loadSalesData();
        });
        pnlTop.add(btnReload, BorderLayout.EAST);

        add(pnlTop, BorderLayout.NORTH);

        // Sales Table
        String[] columnNames = {"Sale ID", "Date & Time", "Total Amount (Rs)", "Payment Method", "Status", "Items Count"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblSales = new JTable(tableModel);
        tblSales.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(tblSales);

        rowSorter = new TableRowSorter<>(tableModel);
        tblSales.setRowSorter(rowSorter);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblSales.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblSales.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblSales.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblSales.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tblSales.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tblSales.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tblSales);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        pnlBottom.setOpaque(false);
        pnlBottom.setBorder(new EmptyBorder(6, 12, 10, 12));

        JButton btnViewReceipt = UITheme.createPrimaryButton("\uD83D\uDDA8  View / Reprint Receipt");
        btnViewReceipt.addActionListener(e -> viewSelectedReceipt());

        JButton btnClose = UITheme.createSecondaryButton("Close");
        btnClose.addActionListener(e -> dispose());

        pnlBottom.add(btnViewReceipt);
        pnlBottom.add(btnClose);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    private void loadSalesData() {
        tableModel.setRowCount(0);
        List<Sale> sales = salesService.getAllSales();
        if (sales != null) {
            for (Sale s : sales) {
                int count = s.getItems() != null ? s.getItems().size() : 0;
                tableModel.addRow(new Object[]{
                        s.getSaleId(),
                        s.getSaleDateTime() != null ? s.getSaleDateTime().format(DATE_TIME_FMT) : "N/A",
                        s.getTotalAmount() != null ? s.getTotalAmount().toPlainString() : "0.00",
                        s.getPaymentMethod(),
                        s.getPaymentStatus(),
                        count
                });
            }
        }
    }

    private void applyFilter() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
        }
    }

    private void viewSelectedReceipt() {
        int viewRow = tblSales.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a sales record from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tblSales.convertRowIndexToModel(viewRow);
        String saleId = (String) tableModel.getValueAt(modelRow, 0);

        Sale sale = salesService.getSaleById(saleId);
        if (sale == null) {
            JOptionPane.showMessageDialog(this, "Could not find sale record for ID: " + saleId, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<SaleItem> items = salesService.getSaleItems(saleId);
        if ((sale.getItems() == null || sale.getItems().isEmpty()) && items != null && !items.isEmpty()) {
            sale.setItems(items);
        }

        // Sale loaded from history should be treated as a reprint (isDuplicateReceipt = true)
        if (sale.getReprintCount() == 0) {
            sale.incrementReprintCount();
        }
        try {
            salesService.markDuplicateReceipt(saleId);
        } catch (Exception ignored) {}

        ReceiptDialog receiptDialog = new ReceiptDialog(
                this,
                sale,
                items,
                new AppSettings(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                sale.getCashierUsername() != null ? sale.getCashierUsername() : "cashier",
                sale.getCustomerId() != null && !sale.getCustomerId().isEmpty() ? sale.getCustomerId() : "Walk-in Customer"
        );
        receiptDialog.setVisible(true);
    }
}
