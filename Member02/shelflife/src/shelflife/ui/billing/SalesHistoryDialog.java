package shelflife.ui.billing;

import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.service.SalesService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Historical sales viewer dialog enabling cashiers to inspect
 * completed sales transactions, batch allocations, and re-generate receipts.
 */
public class SalesHistoryDialog extends JDialog {
    private final SalesService salesService;
    private JTable tblSales;
    private DefaultTableModel salesTableModel;
    private JTable tblItems;
    private DefaultTableModel itemsTableModel;
    private List<Sale> salesList;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SalesHistoryDialog(Window owner, SalesService salesService) {
        super(owner, "ShelfLife - Sales Transaction History", ModalityType.APPLICATION_MODAL);
        this.salesService = salesService;
        initUI();
        loadHistoryData();
    }

    private void initUI() {
        setSize(850, 580);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        // Top Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBorder(new EmptyBorder(10, 15, 5, 15));
        JLabel lblTitle = new JLabel("Completed Sales History & Batch Audit");
        lblTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        add(pnlHeader, BorderLayout.NORTH);

        // Center Split: Sales Table (Top) and Items Table (Bottom)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // Sales Table
        String[] salesCols = {"Sale ID", "Date / Time", "Items Count", "Subtotal ($)", "Total ($)", "Payment Method", "Status"};
        salesTableModel = new DefaultTableModel(salesCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblSales = new JTable(salesTableModel);
        tblSales.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSales.setRowHeight(22);
        tblSales.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSaleSelected();
            }
        });

        // Cell Alignment
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblSales.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        tblSales.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tblSales.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane spSales = new JScrollPane(tblSales);
        spSales.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Committed Sales (Select a row to inspect batch breakdown)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 12)));
        splitPane.setTopComponent(spSales);

        // Line Items Table
        String[] itemsCols = {"Item ID", "Product ID", "Product Name", "Allocated Batch ID", "Qty", "Unit Price ($)", "Line Total ($)"};
        itemsTableModel = new DefaultTableModel(itemsCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblItems = new JTable(itemsTableModel);
        tblItems.setRowHeight(22);
        tblItems.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tblItems.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tblItems.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        JScrollPane spItems = new JScrollPane(tblItems);
        spItems.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Allocated Sale Items & Batch Tracing",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 12)));
        splitPane.setBottomComponent(spItems);

        add(splitPane, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlBottom.setBorder(new EmptyBorder(5, 10, 10, 10));

        JButton btnViewReceipt = new JButton("View / Reprint Receipt");
        btnViewReceipt.addActionListener(e -> viewSelectedReceipt());

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> {
            salesService.reload();
            loadHistoryData();
        });

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        pnlBottom.add(btnViewReceipt);
        pnlBottom.add(btnRefresh);
        pnlBottom.add(btnClose);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private void loadHistoryData() {
        salesTableModel.setRowCount(0);
        itemsTableModel.setRowCount(0);
        salesList = salesService.getAllSales();

        if (salesList != null) {
            for (Sale s : salesList) {
                salesTableModel.addRow(new Object[]{
                        s.getSaleId(),
                        s.getSaleDateTime().format(DATE_FMT),
                        s.getItems().size(),
                        s.getSubtotal().toPlainString(),
                        s.getFinalTotal().toPlainString(),
                        s.getPaymentMethod() != null ? s.getPaymentMethod().getDisplayName() : "N/A",
                        s.getPaymentStatus() != null ? s.getPaymentStatus().getDisplayName() : "N/A"
                });
            }
        }

        if (salesTableModel.getRowCount() > 0) {
            tblSales.setRowSelectionInterval(0, 0);
        }
    }

    private void onSaleSelected() {
        itemsTableModel.setRowCount(0);
        int selectedRow = tblSales.getSelectedRow();
        if (selectedRow < 0 || salesList == null || selectedRow >= salesList.size()) {
            return;
        }

        Sale sale = salesList.get(selectedRow);
        for (SaleItem item : sale.getItems()) {
            itemsTableModel.addRow(new Object[]{
                    item.getSaleItemId(),
                    item.getProductId(),
                    item.getProductName(),
                    item.getBatchId(),
                    item.getQuantity(),
                    item.getUnitPrice().toPlainString(),
                    item.getLineTotal().toPlainString()
            });
        }
    }

    private void viewSelectedReceipt() {
        int selectedRow = tblSales.getSelectedRow();
        if (selectedRow < 0 || salesList == null || selectedRow >= salesList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a sale transaction from the table.", "No Sale Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Sale sale = salesList.get(selectedRow);
        ReceiptDialog.showReceipt(this, sale);
    }
}
