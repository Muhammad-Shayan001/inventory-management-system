package shelflife.ui.expiry;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.WastageRecord;
import shelflife.service.ExpiryService;
import shelflife.service.InventoryService;
import shelflife.ui.Refreshable;
import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Expiry Management and Stock Wastage Swing Panel.
 * Identifies expired and near-expiry inventory batches, displays live risk metrics,
 * and enables cashiers/managers to record unsaleable stock wastage.
 */
public class ExpiryAlertsPanel extends JPanel implements Refreshable {

    private final InventoryService inventoryService;
    private final ExpiryService expiryService;

    // Filter controls
    private JComboBox<String> cmbFilterType;
    private JSpinner spnWarningDays;
    private JButton btnRefresh;
    private JLabel lblMetricsSummary;

    // Alerts Table
    private JTable tblAlerts;
    private DefaultTableModel modelAlerts;

    // Wastage Form
    private JLabel lblSelectedBatch;
    private JSpinner spnWastageQuantity;
    private JComboBox<String> cmbWastageReason;
    private JButton btnRecordWastage;
    private JButton btnViewWastageHistory;

    private ProductBatch selectedBatch = null;

    public ExpiryAlertsPanel(InventoryService inventoryService, ExpiryService expiryService) {
        if (inventoryService == null || expiryService == null) {
            throw new IllegalArgumentException("InventoryService and ExpiryService cannot be null.");
        }
        this.inventoryService = inventoryService;
        this.expiryService = expiryService;

        initUI();
        refreshData();
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BG_WINDOW);

        // 1. Header & Filters
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center: Alerts Table
        add(createTableSection(), BorderLayout.CENTER);

        // 3. Bottom: Wastage Action Panel
        add(createWastagePanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        // Title and description
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Expiry Tracking & Wastage Management");
        title.setFont(Theme.FONT_PAGE_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        lblMetricsSummary = new JLabel("Calculating batch expiry status...");
        lblMetricsSummary.setFont(Theme.FONT_SUBTITLE);
        lblMetricsSummary.setForeground(Theme.TEXT_SECONDARY);

        titlePanel.add(title);
        titlePanel.add(lblMetricsSummary);

        // Controls bar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);

        JLabel lblView = new JLabel("View:");
        lblView.setFont(Theme.FONT_REGULAR);
        filterPanel.add(lblView);
        cmbFilterType = new JComboBox<>(new String[]{"All Attention Batches", "Expired Only", "Near-Expiry Only", "All Inventory Batches"});
        cmbFilterType.setFont(Theme.FONT_REGULAR);
        cmbFilterType.addActionListener(e -> refreshData());
        filterPanel.add(cmbFilterType);

        JLabel lblWindow = new JLabel("Warning Window (Days):");
        lblWindow.setFont(Theme.FONT_REGULAR);
        filterPanel.add(lblWindow);
        spnWarningDays = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
        spnWarningDays.setFont(Theme.FONT_REGULAR);
        spnWarningDays.addChangeListener(e -> refreshData());
        filterPanel.add(spnWarningDays);

        btnRefresh = Styles.createPrimaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshData());
        filterPanel.add(btnRefresh);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(0, 0, 10, 0)
        ));

        return headerPanel;
    }

    private JPanel createTableSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                "Monitored Stock Batches",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                Theme.FONT_SECTION,
                Theme.TEXT_PRIMARY
        ));

        String[] cols = {"Batch ID", "Product ID", "Product Name", "Available Qty", "Expiry Date", "Days Remaining", "Status"};
        modelAlerts = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblAlerts = new JTable(modelAlerts);
        tblAlerts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.styleTable(tblAlerts);

        int[] alertWidths = {95, 85, 170, 95, 100, 105, 120};
        for (int i = 0; i < alertWidths.length && i < tblAlerts.getColumnCount(); i++) {
            tblAlerts.getColumnModel().getColumn(i).setPreferredWidth(alertWidths[i]);
        }

        // Status Renderer
        tblAlerts.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                String val = value != null ? value.toString() : "";
                if (val.contains("EXPIRED")) {
                    setForeground(Theme.COLOR_DANGER_FG);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (val.contains("NEAR EXPIRY")) {
                    setForeground(Theme.COLOR_WARNING_FG);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Theme.COLOR_SUCCESS_FG);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : Theme.BG_TABLE_ROW_ALT);
                }
                return c;
            }
        });

        tblAlerts.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onBatchSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(tblAlerts);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createWastagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                        "Record Stock Wastage for Selected Batch",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        Theme.FONT_SECTION,
                        Theme.TEXT_PRIMARY
                ),
                new EmptyBorder(8, 12, 8, 12)
        ));

        lblSelectedBatch = new JLabel("Please select a batch from the table above to record wastage.");
        lblSelectedBatch.setFont(Theme.FONT_BOLD);
        lblSelectedBatch.setForeground(Theme.TEXT_PRIMARY);

        JPanel formControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        formControls.setOpaque(false);

        JLabel lblWQty = new JLabel("Wastage Quantity:");
        lblWQty.setFont(Theme.FONT_REGULAR);
        formControls.add(lblWQty);
        spnWastageQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
        spnWastageQuantity.setFont(Theme.FONT_REGULAR);
        spnWastageQuantity.setPreferredSize(new Dimension(80, 28));
        formControls.add(spnWastageQuantity);

        JLabel lblWReason = new JLabel("Reason:");
        lblWReason.setFont(Theme.FONT_REGULAR);
        formControls.add(lblWReason);
        cmbWastageReason = new JComboBox<>(new String[]{
                "Expired stock",
                "Damaged packaging",
                "Spoiled / Temperature issue",
                "Customer return / contaminated",
                "Other write-off"
        });
        cmbWastageReason.setFont(Theme.FONT_REGULAR);
        cmbWastageReason.setEditable(true);
        cmbWastageReason.setPreferredSize(new Dimension(220, 28));
        formControls.add(cmbWastageReason);

        btnRecordWastage = Styles.createDangerButton("Confirm Wastage");
        btnRecordWastage.setEnabled(false);
        btnRecordWastage.addActionListener(e -> handleRecordWastage());
        formControls.add(btnRecordWastage);

        btnViewWastageHistory = Styles.createSecondaryButton("View Wastage History");
        btnViewWastageHistory.addActionListener(e -> showWastageHistoryDialog());
        formControls.add(btnViewWastageHistory);

        panel.add(lblSelectedBatch, BorderLayout.NORTH);
        panel.add(formControls, BorderLayout.CENTER);

        return panel;
    }

    private void onBatchSelected() {
        int row = tblAlerts.getSelectedRow();
        if (row >= 0) {
            String batchId = (String) tblAlerts.getValueAt(row, 0);
            selectedBatch = inventoryService.getBatchById(batchId);
            if (selectedBatch != null) {
                Product p = inventoryService.getProductById(selectedBatch.getProductId());
                String pName = p != null ? p.getName() : selectedBatch.getProductId();
                lblSelectedBatch.setText("Selected Batch: " + selectedBatch.getBatchId() + " (" + pName + 
                        ") | Available: " + selectedBatch.getQuantity() + " units | Expiry: " + selectedBatch.getExpiryDate());

                if (selectedBatch.getQuantity() > 0) {
                    spnWastageQuantity.setModel(new SpinnerNumberModel(1, 1, selectedBatch.getQuantity(), 1));
                    btnRecordWastage.setEnabled(true);
                } else {
                    lblSelectedBatch.setText(lblSelectedBatch.getText() + " [OUT OF STOCK]");
                    btnRecordWastage.setEnabled(false);
                }
                return;
            }
        }
        selectedBatch = null;
        lblSelectedBatch.setText("Please select a batch from the table above to record wastage.");
        btnRecordWastage.setEnabled(false);
    }

    private void handleRecordWastage() {
        if (selectedBatch == null) {
            JOptionPane.showMessageDialog(this, "Please select an inventory batch first.", "No Batch Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qty = (Integer) spnWastageQuantity.getValue();
        if (qty <= 0 || qty > selectedBatch.getQuantity()) {
            JOptionPane.showMessageDialog(this, 
                    "Wastage quantity must be between 1 and " + selectedBatch.getQuantity(), 
                    "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String reason = cmbWastageReason.getSelectedItem() != null ? cmbWastageReason.getSelectedItem().toString().trim() : "";
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a wastage reason.", "Reason Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm write-off of " + qty + " units from Batch '" + selectedBatch.getBatchId() + "'?\nReason: " + reason,
                "Confirm Wastage Write-Off",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String wastageId = "WST-" + System.currentTimeMillis();
                WastageRecord record = new WastageRecord(wastageId, selectedBatch.getBatchId(), qty, LocalDateTime.now(), reason);
                expiryService.recordWastage(record);

                JOptionPane.showMessageDialog(this,
                        "Successfully recorded wastage (" + qty + " units) for Batch '" + selectedBatch.getBatchId() + "'.",
                        "Wastage Recorded",
                        JOptionPane.INFORMATION_MESSAGE);

                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to record wastage: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showWastageHistoryDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Historical Stock Wastage Records", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(750, 400);
        dialog.setLocationRelativeTo(this);

        String[] cols = {"Wastage ID", "Batch ID", "Product", "Quantity", "Date & Time", "Reason"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (WastageRecord w : expiryService.getAllWastageRecords()) {
            ProductBatch b = inventoryService.getBatchById(w.getBatchId());
            String prodName = "Unknown";
            if (b != null) {
                Product p = inventoryService.getProductById(b.getProductId());
                if (p != null) prodName = p.getName();
            }
            model.addRow(new Object[]{
                    w.getWastageId(),
                    w.getBatchId(),
                    prodName,
                    w.getQuantity(),
                    w.getWastageDateTime() != null ? w.getWastageDateTime().format(dtf) : "",
                    w.getReason()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        closePanel.add(btnClose);
        dialog.add(closePanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    @Override
    public void refreshData() {
        modelAlerts.setRowCount(0);
        LocalDate today = LocalDate.now();
        int warningDays = (Integer) spnWarningDays.getValue();
        int filterIdx = cmbFilterType != null ? cmbFilterType.getSelectedIndex() : 0;

        List<ProductBatch> allBatches = inventoryService.getAllBatches();
        int expiredCount = 0;
        int nearExpiryCount = 0;
        int safeCount = 0;

        for (ProductBatch b : allBatches) {
            boolean isExp = b.isExpired(today);
            boolean isNear = b.isNearExpiry(warningDays);

            if (isExp) expiredCount++;
            else if (isNear) nearExpiryCount++;
            else safeCount++;

            boolean include = false;
            switch (filterIdx) {
                case 1: // Expired Only
                    include = isExp;
                    break;
                case 2: // Near-Expiry Only
                    include = isNear;
                    break;
                case 3: // All Batches
                    include = true;
                    break;
                default: // All Attention Batches (Expired or Near-Expiry)
                    include = isExp || isNear;
                    break;
            }

            if (include) {
                Product p = inventoryService.getProductById(b.getProductId());
                String pName = p != null ? p.getName() : "Unknown";

                long daysDiff = ChronoUnit.DAYS.between(today, b.getExpiryDate());
                String daysStr;
                String statusStr;

                if (isExp) {
                    daysStr = Math.abs(daysDiff) + " days ago";
                    statusStr = "EXPIRED";
                } else if (daysDiff == 0) {
                    daysStr = "Expires Today";
                    statusStr = "NEAR EXPIRY (CRITICAL)";
                } else if (isNear) {
                    daysStr = daysDiff + " days left";
                    statusStr = "NEAR EXPIRY (" + daysDiff + "d)";
                } else {
                    daysStr = daysDiff + " days left";
                    statusStr = "SAFE";
                }

                modelAlerts.addRow(new Object[]{
                        b.getBatchId(),
                        b.getProductId(),
                        pName,
                        b.getQuantity(),
                        b.getExpiryDate().toString(),
                        daysStr,
                        statusStr
                });
            }
        }

        lblMetricsSummary.setText(String.format(
                "Risk Status: %d Expired | %d Near-Expiry (<= %d days) | %d Safe Batches",
                expiredCount, nearExpiryCount, warningDays, safeCount
        ));

        onBatchSelected();
    }
}
