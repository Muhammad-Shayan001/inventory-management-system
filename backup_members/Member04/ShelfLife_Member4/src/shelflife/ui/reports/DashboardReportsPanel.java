package shelflife.ui.reports;

import shelflife.model.*;
import shelflife.service.ReportService;
import shelflife.storage.StorageException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardReportsPanel extends JPanel {

    private final ReportService reportService;

    private JLabel lblProductCount;
    private JLabel lblPhysicalStock;
    private JLabel lblExpiredBatches;
    private JLabel lblNearExpiry;
    private JLabel lblLowStock;
    private JLabel lblTodaySales;

    private JTable tblSales;
    private DefaultTableModel modelSales;
    private JTable tblStock;
    private DefaultTableModel modelStock;
    private JTable tblWastage;
    private DefaultTableModel modelWastage;

    public DashboardReportsPanel(ReportService reportService) {
        this.reportService = reportService;
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 247, 250));

        initUI();
        refreshData();
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Dashboard & Operational Reports");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(new Color(40, 53, 147));

        JButton btnRefresh = new JButton("Refresh Data");
        btnRefresh.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(63, 81, 181));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> refreshData());

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(btnRefresh, BorderLayout.EAST);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 6, 10, 0));
        cardsPanel.setOpaque(false);

        lblProductCount = createKpiCard(cardsPanel, "Total Products", "0", new Color(33, 150, 243));
        lblPhysicalStock = createKpiCard(cardsPanel, "Total Stock", "0", new Color(76, 175, 80));
        lblExpiredBatches = createKpiCard(cardsPanel, "Expired Batches", "0", new Color(244, 67, 54));
        lblNearExpiry = createKpiCard(cardsPanel, "Near Expiry (7d)", "0", new Color(255, 152, 0));
        lblLowStock = createKpiCard(cardsPanel, "Low Stock Items", "0", new Color(156, 39, 176));
        lblTodaySales = createKpiCard(cardsPanel, "Today's Sales", "Rs 0.00", new Color(0, 150, 136));

        JPanel topContainer = new JPanel(new BorderLayout(0, 15));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(cardsPanel, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel pnlSales = new JPanel(new BorderLayout(10, 10));
        pnlSales.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelSales = new DefaultTableModel(new String[]{"Sale ID", "Date & Time", "Total Amount", "Payment Method", "Status"}, 0);
        tblSales = createStyledTable(modelSales);
        pnlSales.add(new JScrollPane(tblSales), BorderLayout.CENTER);
        pnlSales.add(createExportButton(tblSales, "sales_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Sales History", pnlSales);

        JPanel pnlStock = new JPanel(new BorderLayout(10, 10));
        pnlStock.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelStock = new DefaultTableModel(new String[]{"Batch ID", "Product ID", "Product Name", "Quantity", "Expiry Date", "Status"}, 0);
        tblStock = createStyledTable(modelStock);
        pnlStock.add(new JScrollPane(tblStock), BorderLayout.CENTER);
        pnlStock.add(createExportButton(tblStock, "stock_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Current Inventory", pnlStock);

        JPanel pnlWastage = new JPanel(new BorderLayout(10, 10));
        pnlWastage.setBorder(new EmptyBorder(10, 10, 10, 10));
        modelWastage = new DefaultTableModel(new String[]{"Wastage ID", "Batch ID", "Quantity Discarded", "Date & Time", "Reason"}, 0);
        tblWastage = createStyledTable(modelWastage);
        pnlWastage.add(new JScrollPane(tblWastage), BorderLayout.CENTER);
        pnlWastage.add(createExportButton(tblWastage, "wastage_report.csv"), BorderLayout.SOUTH);
        tabbedPane.addTab("Wastage Log", pnlWastage);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JLabel createKpiCard(JPanel parent, String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTitle.setForeground(Color.GRAY);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblVal.setForeground(new Color(33, 33, 33));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);

        parent.add(card);
        return lblVal;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(230, 235, 245));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setGridColor(new Color(220, 220, 220));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        return table;
    }

    private JButton createExportButton(JTable table, String defaultFilename) {
        JButton btnExport = new JButton("Export Table to CSV");
        btnExport.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnExport.addActionListener(e -> exportTableToCSV(table, defaultFilename));
        return btnExport;
    }

    public void refreshData() {
        try {
            reportService.refreshData();

            lblProductCount.setText(String.valueOf(reportService.getTotalProductCount()));
            lblPhysicalStock.setText(String.valueOf(reportService.getTotalPhysicalStock()));
            lblExpiredBatches.setText(String.valueOf(reportService.getExpiredBatchesCount()));
            lblNearExpiry.setText(String.valueOf(reportService.getNearExpiryBatchesCount(7)));
            lblLowStock.setText(String.valueOf(reportService.getLowStockProductCount()));

            BigDecimal todayTotal = reportService.getSalesTotalForDate(LocalDate.now());
            lblTodaySales.setText("Rs " + todayTotal.toPlainString());

            modelSales.setRowCount(0);
            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Sale s : reportService.getSalesHistory()) {
                modelSales.addRow(new Object[]{
                        s.getSaleId(),
                        s.getSaleDateTime().format(dtFmt),
                        "Rs " + s.getTotalAmount().toPlainString(),
                        s.getPaymentMethod(),
                        s.getPaymentStatus()
                });
            }

            modelStock.setRowCount(0);
            LocalDate today = LocalDate.now();
            for (ProductBatch b : reportService.getStockInformation()) {
                Product p = reportService.getProductById(b.getProductId());
                String pName = (p != null) ? p.getName() : "Unknown";

                String status = "OK";
                if (b.getExpiryDate().isBefore(today)) {
                    status = "EXPIRED";
                } else if (!b.getExpiryDate().isAfter(today.plusDays(7))) {
                    status = "NEAR EXPIRY";
                }

                modelStock.addRow(new Object[]{
                        b.getBatchId(),
                        b.getProductId(),
                        pName,
                        b.getQuantity(),
                        b.getExpiryDate().toString(),
                        status
                });
            }

            modelWastage.setRowCount(0);
            for (WastageRecord w : reportService.getWastageHistory()) {
                modelWastage.addRow(new Object[]{
                        w.getWastageId(),
                        w.getBatchId(),
                        w.getQuantity(),
                        w.getWastageDateTime().format(dtFmt),
                        w.getReason()
                });
            }

        } catch (StorageException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to refresh dashboard: " + ex.getMessage(),
                    "Storage Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportTableToCSV(JTable table, String defaultFilename) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(defaultFilename));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
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
