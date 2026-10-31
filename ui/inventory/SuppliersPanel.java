package shelflife.ui.inventory;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Supplier;
import shelflife.service.InventoryService;
import shelflife.service.SupplierService;
import shelflife.ui.Refreshable;
import shelflife.ui.UITheme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Management panel for Supplier vendor contacts and inventory stock-in receiving.
 */
public class SuppliersPanel extends JPanel implements Refreshable {

    private final InventoryService inventoryService;
    private final SupplierService supplierService;

    // Supplier table
    private DefaultTableModel supplierTableModel;
    private JTable tblSuppliers;
    private JTextField txtSearchSupplier;

    // Supplier form
    private JTextField txtSupplierId;
    private JTextField txtSupplierName;
    private JTextField txtContactPerson;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtAddress;
    private JCheckBox chkActive;

    // Stock receiving form
    private JComboBox<Product> cmbProducts;
    private JComboBox<Supplier> cmbReceivingSuppliers;
    private JTextField txtBatchId;
    private JSpinner spnQuantity;
    private JTextField txtCostPrice;
    private JTextField txtExpiryDate; // YYYY-MM-DD
    private DefaultTableModel batchHistoryModel;
    private JTable tblBatchHistory;

    public SuppliersPanel(InventoryService inventoryService, SupplierService supplierService) {
        this.inventoryService = inventoryService;
        this.supplierService = supplierService;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Tabbed Pane: Tab 1 = Supplier Directory, Tab 2 = Stock-In Receiving
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(UITheme.BG_PAGE);
        tabbedPane.setOpaque(true);

        tabbedPane.addTab("Supplier Directory", createSuppliersTab());
        tabbedPane.addTab("Stock-In & Purchase Intake", createStockIntakeTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createSuppliersTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BG_PAGE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Search Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setOpaque(false);
        JLabel lblSearch = new JLabel("Search Suppliers:");
        lblSearch.setFont(UITheme.FONT_REGULAR);
        topBar.add(lblSearch);
        txtSearchSupplier = new JTextField(20);
        UITheme.styleTextField(txtSearchSupplier);
        txtSearchSupplier.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterSuppliers(txtSearchSupplier.getText().trim());
            }
        });
        topBar.add(txtSearchSupplier);
        panel.add(topBar, BorderLayout.NORTH);

        // Center Split Pane: Table on Left, Details on Right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // Table
        String[] cols = {"Supplier ID", "Company / Name", "Contact Person", "Phone", "Status"};
        supplierTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tblSuppliers = new JTable(supplierTableModel);
        tblSuppliers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(tblSuppliers);
        tblSuppliers.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedSupplierToForm();
            }
        });

        int[] suppWidths = {95, 140, 120, 105, 75};
        for (int i = 0; i < suppWidths.length && i < tblSuppliers.getColumnCount(); i++) {
            tblSuppliers.getColumnModel().getColumn(i).setPreferredWidth(suppWidths[i]);
        }

        JScrollPane scrollTable = new JScrollPane(tblSuppliers);
        scrollTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        splitPane.setLeftComponent(scrollTable);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.CARD_BG);
        form.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER),
                        "Supplier Details",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        UITheme.FONT_SECTION,
                        UITheme.TEXT_MAIN
                ),
                new EmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        // ID
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.32; gbc.gridwidth = 1;
        JLabel lblId = new JLabel("Supplier ID:");
        lblId.setFont(UITheme.FONT_REGULAR);
        form.add(lblId, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        txtSupplierId = new JTextField();
        UITheme.styleTextField(txtSupplierId);
        form.add(txtSupplierId, gbc);

        // Name
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.32;
        JLabel lblName = new JLabel("Company Name:");
        lblName.setFont(UITheme.FONT_REGULAR);
        form.add(lblName, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        txtSupplierName = new JTextField();
        UITheme.styleTextField(txtSupplierName);
        form.add(txtSupplierName, gbc);

        // Contact
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.32;
        JLabel lblContact = new JLabel("Contact Person:");
        lblContact.setFont(UITheme.FONT_REGULAR);
        form.add(lblContact, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        txtContactPerson = new JTextField();
        UITheme.styleTextField(txtContactPerson);
        form.add(txtContactPerson, gbc);

        // Phone
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.32;
        JLabel lblPhone = new JLabel("Phone:");
        lblPhone.setFont(UITheme.FONT_REGULAR);
        form.add(lblPhone, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        txtPhone = new JTextField();
        UITheme.styleTextField(txtPhone);
        form.add(txtPhone, gbc);

        // Email
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.32;
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(UITheme.FONT_REGULAR);
        form.add(lblEmail, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        txtEmail = new JTextField();
        UITheme.styleTextField(txtEmail);
        form.add(txtEmail, gbc);

        // Address
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.32;
        JLabel lblAddr = new JLabel("Address:");
        lblAddr.setFont(UITheme.FONT_REGULAR);
        form.add(lblAddr, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        txtAddress = new JTextField();
        UITheme.styleTextField(txtAddress);
        form.add(txtAddress, gbc);

        // Active
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        chkActive = new JCheckBox("Active Supplier");
        chkActive.setFont(UITheme.FONT_REGULAR);
        chkActive.setOpaque(false);
        chkActive.setSelected(true);
        form.add(chkActive, gbc);

        // Buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 6, 0));
        btnPanel.setOpaque(false);

        JButton btnNew = UITheme.createSecondaryButton("New");
        btnNew.addActionListener(e -> clearSupplierForm());

        JButton btnSave = UITheme.createPrimaryButton("Save");
        btnSave.addActionListener(e -> saveSupplier());

        JButton btnToggle = UITheme.createSecondaryButton("Deactivate");
        btnToggle.addActionListener(e -> toggleSupplierStatus());

        btnPanel.add(btnNew);
        btnPanel.add(btnSave);
        btnPanel.add(btnToggle);
        form.add(btnPanel, gbc);

        // Glue space at bottom
        row++;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        form.add(Box.createVerticalGlue(), gbc);

        splitPane.setRightComponent(shelflife.ui.theme.Styles.createScrollPane(form));
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStockIntakeTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Intake Form on Top
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.CARD_BG);
        form.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER),
                        "Receive New Product Stock Batch",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        UITheme.FONT_SECTION,
                        UITheme.TEXT_MAIN
                ),
                new EmptyBorder(10, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Product & Supplier
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblProd = new JLabel("Product:");
        lblProd.setFont(UITheme.FONT_REGULAR);
        form.add(lblProd, gbc);
        gbc.gridx = 1;
        cmbProducts = new JComboBox<>();
        cmbProducts.setFont(UITheme.FONT_REGULAR);
        form.add(cmbProducts, gbc);

        gbc.gridx = 2;
        JLabel lblSup = new JLabel("Supplier:");
        lblSup.setFont(UITheme.FONT_REGULAR);
        form.add(lblSup, gbc);
        gbc.gridx = 3;
        cmbReceivingSuppliers = new JComboBox<>();
        cmbReceivingSuppliers.setFont(UITheme.FONT_REGULAR);
        form.add(cmbReceivingSuppliers, gbc);

        // Row 1: Batch ID & Quantity
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblBId = new JLabel("Batch ID:");
        lblBId.setFont(UITheme.FONT_REGULAR);
        form.add(lblBId, gbc);
        gbc.gridx = 1;
        txtBatchId = new JTextField("BATCH-" + System.currentTimeMillis() % 100000);
        UITheme.styleTextField(txtBatchId);
        form.add(txtBatchId, gbc);

        gbc.gridx = 2;
        JLabel lblQty = new JLabel("Quantity (Units):");
        lblQty.setFont(UITheme.FONT_REGULAR);
        form.add(lblQty, gbc);
        gbc.gridx = 3;
        spnQuantity = new JSpinner(new SpinnerNumberModel(20, 1, 10000, 1));
        spnQuantity.setFont(UITheme.FONT_REGULAR);
        form.add(spnQuantity, gbc);

        // Row 2: Cost Price & Expiry Date
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblCost = new JLabel("Cost Price (PKR):");
        lblCost.setFont(UITheme.FONT_REGULAR);
        form.add(lblCost, gbc);
        gbc.gridx = 1;
        txtCostPrice = new JTextField("100.00");
        UITheme.styleTextField(txtCostPrice);
        form.add(txtCostPrice, gbc);

        gbc.gridx = 2;
        JLabel lblExp = new JLabel("Expiry Date (YYYY-MM-DD):");
        lblExp.setFont(UITheme.FONT_REGULAR);
        form.add(lblExp, gbc);
        gbc.gridx = 3;
        txtExpiryDate = new JTextField(LocalDate.now().plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE));
        UITheme.styleTextField(txtExpiryDate);
        form.add(txtExpiryDate, gbc);

        // Row 3: Action Button
        gbc.gridx = 3; gbc.gridy = 3;
        JButton btnReceive = UITheme.createPrimaryButton("Receive Stock Into Inventory");
        btnReceive.addActionListener(e -> handleStockIntake());
        form.add(btnReceive, gbc);

        panel.add(form, BorderLayout.NORTH);

        // Batch History Table at Bottom
        String[] bcols = {"Batch ID", "Product", "Qty Remaining", "Cost Price", "Expiry Date", "Supplier", "Received Date"};
        batchHistoryModel = new DefaultTableModel(bcols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tblBatchHistory = new JTable(batchHistoryModel);
        UITheme.styleTable(tblBatchHistory);
        int[] bWidths = {95, 140, 95, 85, 95, 120, 110};
        for (int i = 0; i < bWidths.length && i < tblBatchHistory.getColumnCount(); i++) {
            tblBatchHistory.getColumnModel().getColumn(i).setPreferredWidth(bWidths[i]);
        }

        JScrollPane scrollHistory = new JScrollPane(tblBatchHistory);
        scrollHistory.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                "Current Active Batches & Stock Intake History",
                TitledBorder.LEFT, TitledBorder.TOP,
                UITheme.FONT_SECTION,
                UITheme.TEXT_MAIN
        ));
        panel.add(scrollHistory, BorderLayout.CENTER);

        return panel;
    }

    private void handleStockIntake() {
        Product prod = (Product) cmbProducts.getSelectedItem();
        Supplier sup = (Supplier) cmbReceivingSuppliers.getSelectedItem();
        String batchId = txtBatchId.getText().trim();
        int qty = (int) spnQuantity.getValue();

        if (prod == null) {
            JOptionPane.showMessageDialog(this, "Please select a product.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (batchId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Batch ID cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal cost;
        try {
            cost = new BigDecimal(txtCostPrice.getText().trim());
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid non-negative cost price.", "Invalid Cost Price", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate expiry;
        try {
            expiry = LocalDate.parse(txtExpiryDate.getText().trim());
            if (expiry.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Expiry date cannot be in the past.", "Invalid Expiry Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Please enter expiry date in YYYY-MM-DD format.", "Invalid Date Format", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String supId = (sup != null) ? sup.getSupplierId() : "SUP-01";

        try {
            supplierService.receiveStock(prod.getProductId(), batchId, qty, expiry, cost, supId);
            JOptionPane.showMessageDialog(this,
                    String.format("Successfully received %d units of '%s' into Batch '%s'.", qty, prod.getName(), batchId),
                    "Stock Received", JOptionPane.INFORMATION_MESSAGE);
            txtBatchId.setText("BATCH-" + System.currentTimeMillis() % 100000);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to receive stock: " + ex.getMessage(), "Intake Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSupplier() {
        String id = txtSupplierId.getText().trim();
        String name = txtSupplierName.getText().trim();
        if (id.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier ID and Name are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Supplier existing = supplierService.getSupplierById(id);
        boolean active = chkActive.isSelected();
        Supplier s = new Supplier(id, name, txtContactPerson.getText().trim(), txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim(), active);

        try {
            if (existing == null) {
                supplierService.addSupplier(s);
            } else {
                supplierService.updateSupplier(s);
            }
            JOptionPane.showMessageDialog(this, "Supplier saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving supplier: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleSupplierStatus() {
        int selected = tblSuppliers.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Select a supplier from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = (String) supplierTableModel.getValueAt(selected, 0);
        try {
            Supplier s = supplierService.getSupplierById(id);
            if (s != null) {
                s.setActive(!s.isActive());
                supplierService.updateSupplier(s);
                refreshData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating supplier: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedSupplierToForm() {
        int row = tblSuppliers.getSelectedRow();
        if (row >= 0) {
            String id = (String) supplierTableModel.getValueAt(row, 0);
            Supplier s = supplierService.getSupplierById(id);
            if (s != null) {
                txtSupplierId.setText(s.getSupplierId());
                txtSupplierId.setEditable(false);
                txtSupplierName.setText(s.getName());
                txtContactPerson.setText(s.getContactPerson());
                txtPhone.setText(s.getPhone());
                txtEmail.setText(s.getEmail());
                txtAddress.setText(s.getAddress());
                chkActive.setSelected(s.isActive());
            }
        }
    }

    private void clearSupplierForm() {
        tblSuppliers.clearSelection();
        txtSupplierId.setText("SUP-" + String.format("%02d", supplierService.getAllSuppliers().size() + 1));
        txtSupplierId.setEditable(true);
        txtSupplierName.setText("");
        txtContactPerson.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        chkActive.setSelected(true);
    }

    private void filterSuppliers(String query) {
        supplierTableModel.setRowCount(0);
        String q = query.toLowerCase();
        for (Supplier s : supplierService.getAllSuppliers()) {
            if (s.getSupplierId().toLowerCase().contains(q) ||
                s.getName().toLowerCase().contains(q) ||
                s.getContactPerson().toLowerCase().contains(q) ||
                s.getPhone().contains(q)) {
                supplierTableModel.addRow(new Object[]{
                        s.getSupplierId(),
                        s.getName(),
                        s.getContactPerson(),
                        s.getPhone(),
                        s.isActive() ? "Active" : "Inactive"
                });
            }
        }
    }

    @Override
    public void refreshData() {
        supplierService.reload();
        inventoryService.reload();

        // Refresh suppliers table
        filterSuppliers(txtSearchSupplier != null ? txtSearchSupplier.getText().trim() : "");

        // Refresh products dropdown
        if (cmbProducts != null) {
            cmbProducts.removeAllItems();
            for (Product p : inventoryService.getAllProducts()) {
                if (p.isActive()) {
                    cmbProducts.addItem(p);
                }
            }
        }

        // Refresh receiving suppliers dropdown
        if (cmbReceivingSuppliers != null) {
            cmbReceivingSuppliers.removeAllItems();
            for (Supplier s : supplierService.getActiveSuppliers()) {
                cmbReceivingSuppliers.addItem(s);
            }
        }

        // Refresh batch history
        if (batchHistoryModel != null) {
            batchHistoryModel.setRowCount(0);
            for (ProductBatch b : inventoryService.getAllBatches()) {
                Product p = inventoryService.getProductById(b.getProductId());
                String pName = p != null ? p.getName() : b.getProductId();
                Supplier s = supplierService.getSupplierById(b.getSupplierId());
                String sName = s != null ? s.getName() : b.getSupplierId();

                batchHistoryModel.addRow(new Object[]{
                        b.getBatchId(),
                        pName + " (" + b.getProductId() + ")",
                        b.getQuantity(),
                        CurrencyUtil.formatPKR(b.getCostPrice()),
                        b.getExpiryDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        sName,
                        b.getReceivedDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                });
            }
        }
    }
}
