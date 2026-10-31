package shelflife.ui.reports;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Sale;
import shelflife.model.WastageRecord;
import shelflife.service.ReportService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Vector;

/**
 * Panel for dashboard and reports display.
 * Shows inventory summaries, sales totals, wastage history, and stock information.
 * Uses JTable for all tabular data display.
 * Refreshes data after successful operations.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class DashboardReportsPanel extends javax.swing.JPanel {
    private final ReportService reportService;
    private JTable reportsTable;
    private DefaultTableModel tableModel;
    private JTextField dateField;
    
    public DashboardReportsPanel(ReportService reportService) {
        this.reportService = reportService;
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Select Date (yyyy-MM-dd):"));
        dateField = new JTextField("2026-10-25");
        filterPanel.add(dateField);
        
        JButton showDayReportBtn = new JButton("Show Day's Sales");
        showDayReportBtn.addActionListener(e -> showDaySalesReport());
        filterPanel.add(showDayReportBtn);
        
        add(filterPanel, BorderLayout.NORTH);
        
        // Reports table
        tableModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Metric", "Value"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportsTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(reportsTable);
        add(tableScroll, BorderLayout.CENTER);
        
        // Control buttons panel at bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.addActionListener(e -> refreshData());
        controlPanel.add(refreshBtn);
        
        JButton exportBtn = new JButton("Export Summary");
        exportBtn.addActionListener(e -> exportSummary());
        controlPanel.add(exportBtn);
        
        add(controlPanel, BorderLayout.SOUTH);
        
        loadSummary();
    }
    
    private void loadSummary() {
        tableModel.setRowCount(0);
        
        // Product count
        tableModel.addRow(new Object[]{"Total Products", reportService.getProductCount()});
        
        // Total stock
        tableModel.addRow(new Object[]{"Total Physical Stock", reportService.getTotalStock()});
        
        // Sellable stock
        tableModel.addRow(new Object[]{"Sellable Stock (non-expired)", reportService.getSellableStockTotal()});
        
        // Low stock count
        tableModel.addRow(new Object[]{"Low Stock Products", reportService.getLowStockCount()});
        
        // Expired batches
        tableModel.addRow(new Object[]{"Expired Batches", reportService.getExpiredBatchCount()});
        
        // Near-expiry count
        tableModel.addRow(new Object[]{"Near-Expiry Batches (30 days)", reportService.getNearExpiryCount(30)});
        
        // Sales total for selected date
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            BigDecimal daySales = reportService.getSalesTotalForDay(date);
            tableModel.addRow(new Object[]{"Sales Total for " + date, daySales});
        } catch (Exception e) {
            tableModel.addRow(new Object[]{"Sales Total for " + dateField.getText(), BigDecimal.ZERO});
        }
    }
    
    private void showDaySalesReport() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            List<Sale> sales = reportService.getSalesHistory();
            
            // Filter sales for the selected date
            List<Sale> daySales = new ArrayList<>();
            for (Sale s : sales) {
                if (s.getSaleDateTime() != null && s.getSaleDateTime().toLocalDate().equals(date)) {
                    daySales.add(s);
                }
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Sales Report for ").append(date).append("\n\n");
            sb.append(String.format("%-10s %-20s %-15s %-10s%n", "Sale ID", "Date Time", "Total", "Status"));
            for (Sale s : daySales) {
                sb.append(String.format("%-10s %-20s %-15s %-10s%n",
                        s.getSaleId(),
                        s.getSaleDateTime(),
                        s.getFinalTotal().toPlainString(),
                        s.getPaymentStatus()));
            }
            sb.append("\nTotal sales: ").append(daySales.size()).append("\n");
            sb.append("Total revenue: Rs. ").append(reportService.getSalesTotalForDay(date).toPlainString());
            
            JOptionPane.showMessageDialog(this, sb.toString(), "Daily Sales Report", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportSummary() {
        // In a full implementation, this would export to a file
        JOptionPane.showMessageDialog(this, "Summary export functionality would save a PDF/CSV report.", 
                "Export", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshData() {
        loadSummary();
    }
}