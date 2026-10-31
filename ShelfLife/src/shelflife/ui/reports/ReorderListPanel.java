package shelflife.ui.reports;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Supplier;
import shelflife.service.InventoryService;
import shelflife.service.ReportService;
import shelflife.service.SupplierService;
import shelflife.ui.Refreshable;
import shelflife.ui.UITheme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reorder List panel displaying depleted inventory below configured thresholds
 * with associated supplier procurement information and CSV export.
 */
public class ReorderListPanel extends JPanel implements Refreshable {

    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final ReportService reportService;

    private DefaultTableModel tableModel;
    private JTable tblReorder;
    private JLabel lblSummary;

    public ReorderListPanel(InventoryService inventoryService, SupplierService supplierService, ReportService reportService) {
        this.inventoryService = inventoryService;
        this.supplierService = supplierService;
        this.reportService = reportService;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Top Banner
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.CARD_BG);
        topPanel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblTitle = new JLabel("Stock Reorder Planning");
        lblTitle.setFont(UITheme.FONT_PAGE_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        lblSummary = new JLabel("Calculating items below threshold...");
        lblSummary.setFont(UITheme.FONT_SUBTITLE);
        lblSummary.setForeground(UITheme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(lblTitle);
        titleBox.add(lblSummary);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnExport = UITheme.createPrimaryButton("Export Reorder Sheet (CSV)");
        btnExport.addActionListener(e -> exportToCsv());

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshData());

        btnBar.add(btnExport);
        btnBar.add(btnRefresh);

        topPanel.add(titleBox, BorderLayout.WEST);
        topPanel.add(btnBar, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Table
        String[] cols = {
                "Product ID", "Product Name", "Category", "Current Stock",
                "Threshold", "Stock Deficit", "Suggested Order", "Primary Supplier", "Supplier Contact"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblReorder = new JTable(tableModel);
        UITheme.styleTable(tblReorder);

        int[] reorderWidths = {85, 140, 95, 100, 80, 100, 125, 125, 115};
        for (int i = 0; i < reorderWidths.length && i < tblReorder.getColumnCount(); i++) {
            tblReorder.getColumnModel().getColumn(i).setPreferredWidth(reorderWidths[i]);
        }

        JScrollPane scrollPane = new JScrollPane(tblReorder);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        tableModel.setRowCount(0);
        int lowCount = 0;

        List<Product> products = inventoryService.getAllProducts();
        for (Product p : products) {
            if (!p.isActive()) continue;

            int stock = inventoryService.getTotalStock(p.getProductId());
            int threshold = p.getLowStockThreshold();

            if (stock <= threshold) {
                lowCount++;
                int deficit = Math.max(0, threshold - stock);
                int suggestedOrder = Math.max(deficit + 20, threshold * 2);

                // Find supplier from most recent batch
                String supplierName = "Not Assigned";
                String supplierPhone = "-";
                List<ProductBatch> batches = inventoryService.getBatchesByProductId(p.getProductId());
                if (!batches.isEmpty()) {
                    ProductBatch lastBatch = batches.get(batches.size() - 1);
                    if (supplierService != null) {
                        Supplier sup = supplierService.getSupplierById(lastBatch.getSupplierId());
                        if (sup != null) {
                            supplierName = sup.getName();
                            supplierPhone = sup.getPhone();
                        }
                    }
                }

                tableModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getName(),
                        p.getCategory(),
                        stock,
                        threshold,
                        deficit,
                        suggestedOrder,
                        supplierName,
                        supplierPhone
                });
            }
        }

        lblSummary.setText(String.format("%d product(s) currently at or below minimum reorder thresholds.", lowCount));
    }

    private void exportToCsv() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No reorder items to export.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Reorder_List_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()) + ".csv"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.UTF_8))) {
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    pw.print("\"" + tableModel.getColumnName(c) + "\"" + (c < tableModel.getColumnCount() - 1 ? "," : "\n"));
                }
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        Object val = tableModel.getValueAt(r, c);
                        pw.print("\"" + (val != null ? val.toString().replace("\"", "\"\"") : "") + "\"" + (c < tableModel.getColumnCount() - 1 ? "," : "\n"));
                    }
                }
                JOptionPane.showMessageDialog(this, "Reorder list successfully exported to:\n" + dest.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
