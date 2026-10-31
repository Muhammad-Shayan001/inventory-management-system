package shelflife.ui.inventory;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.Role;
import shelflife.service.InventoryService;
import shelflife.storage.StorageException;
import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import shelflife.util.BarcodeSheetPrinter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Product and Inventory Management Swing Panel.
 * Primary deliverable for Member 1.
 * Provides complete catalog management, product search, batch tracking,
 * real-time validation, and protected deletion.
 */
public class ProductInventoryPanel extends JPanel implements shelflife.ui.Refreshable {

    private final InventoryService inventoryService;

    // --- Search Components ---
    private JTextField txtSearch;
    private JButton btnSearch;
    private JButton btnClearSearch;
    private JLabel lblSummary;

    // --- Product Table & Model ---
    private JTable tblProducts;
    private DefaultTableModel productTableModel;

    // --- Product Form Components ---
    private JTextField txtProdId;
    private JTextField txtProdName;
    private JComboBox<String> cmbProdCategory;
    private JTextField txtProdPrice;
    private JTextField txtProdThreshold;
    private JButton btnAddProduct;
    private JButton btnUpdateProduct;
    private JButton btnDeleteProduct;
    private JButton btnToggleActive;
    private JButton btnClearProductForm;

    // --- Batch Table & Model ---
    private JTable tblBatches;
    private DefaultTableModel batchTableModel;
    private JLabel lblBatchHeader;
    private JButton btnShowAllBatches;

    // --- Batch Form Components ---
    private JTextField txtBatchId;
    private JTextField txtBatchProdId;
    private JTextField txtBatchQuantity;
    private JTextField txtBatchExpiryDate;
    private JButton btnAddBatch;
    private JButton btnDeleteBatch;
    private JButton btnClearBatchForm;

    // State tracking
    private String selectedProductId = null;

    /**
     * Required constructor taking the shared InventoryService.
     */
    public ProductInventoryPanel(InventoryService inventoryService) {
        if (inventoryService == null) {
            throw new IllegalArgumentException("InventoryService cannot be null.");
        }
        this.inventoryService = inventoryService;

        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(Theme.BG_WINDOW);

        // 1. Top Header & Search Bar
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Workspace (Split into Products & Batches)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createProductSection(),
                createBatchSection());
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerLocation(0.6);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // UI Layout Sections
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        // Title and Subtitle
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Product & Inventory Management");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        lblSummary = new JLabel("Loading inventory metrics...");
        lblSummary.setFont(Theme.FONT_SUBTITLE);
        lblSummary.setForeground(Theme.TEXT_SECONDARY);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSummary);

        // Search Bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);

        JLabel lblSearchPrompt = new JLabel("Search Catalog:");
        lblSearchPrompt.setFont(Theme.FONT_REGULAR);

        txtSearch = new JTextField(16);
        Styles.styleTextField(txtSearch);
        txtSearch.setToolTipText("Enter product ID, name, or category");
        txtSearch.addActionListener(e -> performSearch());

        btnSearch = Styles.createPrimaryButton("Search");
        btnSearch.addActionListener(e -> performSearch());

        btnClearSearch = Styles.createSecondaryButton("Reset");
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            refreshData();
        });

        searchPanel.add(lblSearchPrompt);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClearSearch);

        // Header utility action buttons in FlowLayout(RIGHT)
        JPanel pnlHeaderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlHeaderActions.setOpaque(false);

        JButton btnPrintBarcodes = Styles.createSecondaryButton("Print Barcodes");
        btnPrintBarcodes.addActionListener(e -> BarcodeSheetPrinter.printLabels(this, inventoryService.getAllProducts()));
        pnlHeaderActions.add(btnPrintBarcodes);

        JButton btnExportCsv = Styles.createSecondaryButton("Export CSV");
        btnExportCsv.addActionListener(e -> exportProductsCsv());
        pnlHeaderActions.add(btnExportCsv);

        JButton btnImportCsv = Styles.createSecondaryButton("Import CSV");
        btnImportCsv.addActionListener(e -> importProductsCsv());
        pnlHeaderActions.add(btnImportCsv);

        JPanel pnlRightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlRightHeader.setOpaque(false);
        pnlRightHeader.add(searchPanel);
        pnlRightHeader.add(pnlHeaderActions);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(pnlRightHeader, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(0, 0, 10, 0)
        ));

        return headerPanel;
    }

    private JPanel createProductSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                "Products Catalog",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                Theme.FONT_SECTION,
                Theme.TEXT_PRIMARY
        ));

        // Table definition
        String[] productColumns = {"ID", "Name", "Category", "Price (Rs.)", "Threshold", "Stock", "Status"};
        productTableModel = new DefaultTableModel(productColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblProducts = new JTable(productTableModel);
        tblProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.styleTable(tblProducts);

        int[] prodWidths = {80, 150, 100, 90, 80, 60, 90};
        for (int i = 0; i < prodWidths.length && i < tblProducts.getColumnCount(); i++) {
            tblProducts.getColumnModel().getColumn(i).setPreferredWidth(prodWidths[i]);
        }

        // Custom status cell renderer (applied after Styles.styleTable so it overrides column 6)
        tblProducts.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if ("INACTIVE".equals(value)) {
                    setForeground(new Color(120, 120, 120));
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else if ("LOW STOCK".equals(value)) {
                    setForeground(Theme.COLOR_DANGER_FG);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("OUT OF STOCK".equals(value)) {
                    setForeground(new Color(150, 20, 20));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Theme.COLOR_SUCCESS_FG);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : Theme.TABLE_ROW_ALT);
                }
                return c;
            }
        });

        tblProducts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblProducts.rowAtPoint(e.getPoint());
                if (row < 0) {
                    if (selectedProductId != null) {
                        clearProductFilter();
                    }
                    return;
                }
                String clickedId = (String) tblProducts.getValueAt(row, 0);
                if (clickedId != null && clickedId.equalsIgnoreCase(selectedProductId)) {
                    // Clicked on already-selected row -> Toggle off
                    clearProductFilter();
                } else {
                    onProductSelected();
                }
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblProducts);
        scrollTable.setPreferredSize(new Dimension(450, 220));

        // Product Form
        JPanel formAndButtons = createProductForm();

        panel.add(scrollTable, BorderLayout.CENTER);
        panel.add(formAndButtons, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProductForm() {
        JPanel formContainer = new JPanel(new BorderLayout(5, 5));
        formContainer.setOpaque(false);
        formContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                "Product Details Form",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                Theme.FONT_SECTION,
                Theme.TEXT_PRIMARY
        ));

        JPanel fieldsPanel = new JPanel(new GridLayout(5, 2, 6, 6));
        fieldsPanel.setOpaque(false);

        JLabel lblPId = new JLabel("Product ID:");
        lblPId.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblPId);
        txtProdId = new JTextField();
        Styles.styleTextField(txtProdId);
        fieldsPanel.add(txtProdId);

        JLabel lblPName = new JLabel("Product Name:");
        lblPName.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblPName);
        txtProdName = new JTextField();
        Styles.styleTextField(txtProdName);
        fieldsPanel.add(txtProdName);

        JLabel lblPCat = new JLabel("Category:");
        lblPCat.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblPCat);
        String[] defaultCategories = {"Dairy", "Bakery", "Produce", "Beverages", "Meat & Seafood", "Canned Goods", "Snacks", "Other"};
        cmbProdCategory = new JComboBox<>(defaultCategories);
        cmbProdCategory.setFont(Theme.FONT_REGULAR);
        cmbProdCategory.setEditable(true);
        fieldsPanel.add(cmbProdCategory);

        JLabel lblPPrice = new JLabel("Selling Price (Rs.):");
        lblPPrice.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblPPrice);
        txtProdPrice = new JTextField();
        Styles.styleTextField(txtProdPrice);
        fieldsPanel.add(txtProdPrice);

        JLabel lblPThresh = new JLabel("Low Stock Threshold:");
        lblPThresh.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblPThresh);
        txtProdThreshold = new JTextField();
        Styles.styleTextField(txtProdThreshold);
        fieldsPanel.add(txtProdThreshold);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        btnPanel.setOpaque(false);

        btnAddProduct = Styles.createSuccessButton("Add Product");
        btnAddProduct.addActionListener(e -> handleAddProduct());

        btnUpdateProduct = Styles.createPrimaryButton("Update Product");
        btnUpdateProduct.addActionListener(e -> handleUpdateProduct());

        btnDeleteProduct = Styles.createDangerButton("Delete Product");
        btnDeleteProduct.addActionListener(e -> handleDeleteProduct());

        btnToggleActive = Styles.createSecondaryButton("Toggle Active");
        btnToggleActive.addActionListener(e -> handleToggleActive());

        btnClearProductForm = Styles.createSecondaryButton("Clear Form");
        btnClearProductForm.addActionListener(e -> clearProductForm());

        btnPanel.add(btnAddProduct);
        btnPanel.add(btnUpdateProduct);
        btnPanel.add(btnDeleteProduct);
        btnPanel.add(btnToggleActive);
        btnPanel.add(btnClearProductForm);

        formContainer.add(fieldsPanel, BorderLayout.CENTER);
        formContainer.add(btnPanel, BorderLayout.SOUTH);

        return formContainer;
    }

    private JPanel createBatchSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                "Product Batches",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                Theme.FONT_SECTION,
                Theme.TEXT_PRIMARY
        ));

        String[] batchColumns = {"Batch ID", "Product ID", "Quantity", "Expiry Date", "Expiry Status"};
        batchTableModel = new DefaultTableModel(batchColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblBatches = new JTable(batchTableModel);
        tblBatches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.styleTable(tblBatches);

        int[] batchWidths = {90, 90, 70, 100, 110};
        for (int i = 0; i < batchWidths.length && i < tblBatches.getColumnCount(); i++) {
            tblBatches.getColumnModel().getColumn(i).setPreferredWidth(batchWidths[i]);
        }

        // Expiry Status Renderer (applied after Styles.styleTable)
        tblBatches.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if ("EXPIRED".equals(value)) {
                    setForeground(Theme.COLOR_DANGER_FG);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("EXPIRES TODAY".equals(value)) {
                    setForeground(Theme.COLOR_WARNING_FG);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Theme.COLOR_SUCCESS_FG);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : Theme.TABLE_ROW_ALT);
                }
                return c;
            }
        });

        tblBatches.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onBatchSelected();
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblBatches);
        scrollTable.setPreferredSize(new Dimension(400, 220));

        // Batch Header Bar with Filter Label and "Show All Batches" button
        JPanel batchHeaderBar = new JPanel(new BorderLayout(8, 0));
        batchHeaderBar.setOpaque(false);
        batchHeaderBar.setBorder(new EmptyBorder(2, 4, 6, 4));

        lblBatchHeader = new JLabel("Showing all batches across products");
        lblBatchHeader.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblBatchHeader.setForeground(new Color(110, 120, 130));

        btnShowAllBatches = Styles.createSecondaryButton("Show All Batches");
        btnShowAllBatches.setFont(Theme.FONT_SMALL);
        btnShowAllBatches.setEnabled(false);
        btnShowAllBatches.setToolTipText("Clear product filter and display all batches across all products");
        btnShowAllBatches.addActionListener(e -> clearProductFilter());

        batchHeaderBar.add(lblBatchHeader, BorderLayout.CENTER);
        batchHeaderBar.add(btnShowAllBatches, BorderLayout.EAST);

        JPanel batchForm = createBatchForm();

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(batchHeaderBar, BorderLayout.NORTH);
        centerPanel.add(scrollTable, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(batchForm, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBatchForm() {
        JPanel formContainer = new JPanel(new BorderLayout(5, 5));
        formContainer.setOpaque(false);
        formContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                "Batch Details Form",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                Theme.FONT_SECTION,
                Theme.TEXT_PRIMARY
        ));

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 2, 6, 6));
        fieldsPanel.setOpaque(false);

        JLabel lblBId = new JLabel("Batch ID:");
        lblBId.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblBId);
        txtBatchId = new JTextField();
        Styles.styleTextField(txtBatchId);
        fieldsPanel.add(txtBatchId);

        JLabel lblBPId = new JLabel("Product ID:");
        lblBPId.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblBPId);
        txtBatchProdId = new JTextField();
        Styles.styleTextField(txtBatchProdId);
        fieldsPanel.add(txtBatchProdId);

        JLabel lblBQty = new JLabel("Quantity:");
        lblBQty.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblBQty);
        txtBatchQuantity = new JTextField();
        Styles.styleTextField(txtBatchQuantity);
        fieldsPanel.add(txtBatchQuantity);

        JLabel lblBExp = new JLabel("Expiry Date (YYYY-MM-DD):");
        lblBExp.setFont(Theme.FONT_BOLD);
        fieldsPanel.add(lblBExp);
        txtBatchExpiryDate = new JTextField();
        Styles.styleTextField(txtBatchExpiryDate);
        txtBatchExpiryDate.setToolTipText("e.g. " + LocalDate.now().plusMonths(3));
        fieldsPanel.add(txtBatchExpiryDate);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        btnPanel.setOpaque(false);

        btnAddBatch = Styles.createPrimaryButton("Add Batch");
        btnAddBatch.addActionListener(e -> handleAddBatch());

        btnDeleteBatch = Styles.createDangerButton("Delete Batch");
        btnDeleteBatch.addActionListener(e -> handleDeleteBatch());

        btnClearBatchForm = Styles.createSecondaryButton("Clear Batch Form");
        btnClearBatchForm.addActionListener(e -> clearBatchForm());

        btnPanel.add(btnAddBatch);
        btnPanel.add(btnDeleteBatch);
        btnPanel.add(btnClearBatchForm);

        formContainer.add(fieldsPanel, BorderLayout.CENTER);
        formContainer.add(btnPanel, BorderLayout.SOUTH);

        return formContainer;
    }

    // =========================================================================
    // Core Public API Contract
    // =========================================================================

    /**
     * Refreshes both products and batches from the shared InventoryService.
     * Required by team contract for inter-module synchronization.
     */
    public void refreshData() {
        populateProductTable(inventoryService.getAllProducts());
        refreshBatchTable();
        updateSummaryBanner();
    }

    // =========================================================================
    // Product Actions & Event Handlers
    // =========================================================================

    private void performSearch() {
        String query = txtSearch.getText();
        List<Product> matches = inventoryService.searchProducts(query);
        populateProductTable(matches);
        lblSummary.setText("Search for \"" + query.trim() + "\" returned " + matches.size() + " product(s).");
    }

    private void populateProductTable(List<Product> products) {
        productTableModel.setRowCount(0);

        for (Product p : products) {
            int stock = inventoryService.getTotalStock(p.getProductId());
            String status;
            if (!p.isActive()) {
                status = "INACTIVE";
            } else if (stock == 0) {
                status = "OUT OF STOCK";
            } else if (stock <= p.getLowStockThreshold()) {
                status = "LOW STOCK";
            } else {
                status = "NORMAL";
            }

            productTableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getName(),
                    p.getCategory(),
                    p.getSellingPrice().toPlainString(),
                    p.getLowStockThreshold(),
                    stock,
                    status
            });
        }
    }

    private void onProductSelected() {
        int selectedRow = tblProducts.getSelectedRow();
        if (selectedRow < 0) return;

        String id = (String) tblProducts.getValueAt(selectedRow, 0);
        Product p = inventoryService.getProductById(id);
        if (p != null) {
            selectedProductId = p.getProductId();
            txtProdId.setText(p.getProductId());
            txtProdId.setEditable(false); // Lock ID during update
            txtProdName.setText(p.getName());
            cmbProdCategory.setSelectedItem(p.getCategory());
            txtProdPrice.setText(p.getSellingPrice().toPlainString());
            txtProdThreshold.setText(String.valueOf(p.getLowStockThreshold()));

            // Pre-fill batch form's product ID
            txtBatchProdId.setText(p.getProductId());

            // Filter batch table to show only this product's batches
            refreshBatchTable();
        }
    }

    private void handleAddProduct() {
        try {
            String id = txtProdId.getText() != null ? txtProdId.getText().trim().toUpperCase() : "";
            txtProdId.setText(id); // Reflect uppercase immediately in UI
            String name = txtProdName.getText();
            Object categoryObj = cmbProdCategory.getSelectedItem();
            String category = categoryObj != null ? categoryObj.toString() : "";
            String priceStr = txtProdPrice.getText();
            String thresholdStr = txtProdThreshold.getText();

            // Client-side quick checks
            if (id.isEmpty()) {
                showError("Product ID is required.");
                txtProdId.requestFocus();
                return;
            }
            if (name == null || name.trim().isEmpty()) {
                showError("Product name is required.");
                txtProdName.requestFocus();
                return;
            }
            if (category.trim().isEmpty()) {
                showError("Product category is required.");
                return;
            }

            BigDecimal price;
            try {
                price = new BigDecimal(priceStr.trim());
            } catch (Exception ex) {
                showError("Invalid price format. Please enter a valid positive number.");
                txtProdPrice.requestFocus();
                return;
            }

            int threshold;
            try {
                threshold = Integer.parseInt(thresholdStr.trim());
            } catch (Exception ex) {
                showError("Invalid threshold format. Please enter a non-negative integer.");
                txtProdThreshold.requestFocus();
                return;
            }

            Product product = new Product(id, name, category, price, threshold);
            inventoryService.addProduct(product);

            JOptionPane.showMessageDialog(this,
                    "Product '" + product.getName() + "' (ID: " + product.getProductId() + ") added successfully!",
                    "Product Added", JOptionPane.INFORMATION_MESSAGE);

            clearProductForm();
            refreshData();

        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleUpdateProduct() {
        try {
            String id = txtProdId.getText() != null ? txtProdId.getText().trim().toUpperCase() : "";
            txtProdId.setText(id);
            String name = txtProdName.getText();
            Object categoryObj = cmbProdCategory.getSelectedItem();
            String category = categoryObj != null ? categoryObj.toString() : "";
            String priceStr = txtProdPrice.getText();
            String thresholdStr = txtProdThreshold.getText();

            if (id.isEmpty()) {
                showError("Select a product from the table to update.");
                return;
            }

            BigDecimal price;
            try {
                price = new BigDecimal(priceStr.trim());
            } catch (Exception ex) {
                showError("Invalid price format. Please enter a valid positive number.");
                return;
            }

            int threshold;
            try {
                threshold = Integer.parseInt(thresholdStr.trim());
            } catch (Exception ex) {
                showError("Invalid threshold format. Please enter a non-negative integer.");
                return;
            }

            Product product = new Product(id, name, category, price, threshold);
            inventoryService.updateProduct(product);

            JOptionPane.showMessageDialog(this,
                    "Product '" + product.getProductId() + "' updated successfully!",
                    "Product Updated", JOptionPane.INFORMATION_MESSAGE);

            refreshData();

        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleDeleteProduct() {
        String id = txtProdId.getText() != null ? txtProdId.getText().trim().toUpperCase() : "";
        if (id.isEmpty()) {
            showError("Select a product from the table to delete.");
            return;
        }
        String cleanId = id;

        // 1. Dependency verification check
        if (!inventoryService.canDeleteProduct(cleanId)) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Cannot permanently delete Product '" + cleanId + "' because it has historical transactions or batch records.\n" +
                    "Would you like to soft-delete (deactivate) this product instead?\n" +
                    "Deactivated products cannot be sold at POS, but historical records remain intact.",
                    "Product Has Dependencies - Soft Delete?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                try {
                    inventoryService.setProductActive(cleanId, false);
                    JOptionPane.showMessageDialog(this, "Product '" + cleanId + "' has been deactivated (soft-deleted).", "Soft-Deleted", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } catch (Exception ex) {
                    showError("Error deactivating product: " + ex.getMessage());
                }
            }
            return;
        }

        // 2. Confirmation Dialog
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete Product '" + cleanId + "'?\nThis action cannot be undone.",
                "Confirm Product Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                inventoryService.deleteProduct(cleanId);
                JOptionPane.showMessageDialog(this,
                        "Product '" + cleanId + "' deleted successfully.",
                        "Product Deleted", JOptionPane.INFORMATION_MESSAGE);
                clearProductForm();
                selectedProductId = null;
                refreshData();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void handleToggleActive() {
        String id = txtProdId.getText() != null ? txtProdId.getText().trim().toUpperCase() : "";
        if (id.isEmpty()) {
            showError("Select a product from the table first.");
            return;
        }
        try {
            Product p = inventoryService.getProductById(id);
            if (p != null) {
                boolean newStatus = !p.isActive();
                inventoryService.setProductActive(id, newStatus);
                JOptionPane.showMessageDialog(this,
                        "Product '" + p.getName() + "' is now " + (newStatus ? "ACTIVE (Sellable)" : "INACTIVE (Deactivated / Soft-deleted)") + ".",
                        "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            }
        } catch (Exception ex) {
            showError("Error updating product status: " + ex.getMessage());
        }
    }

    /**
     * Clears any active product filter, deselects the product table,
     * and shows all batches across all products.
     */
    public void clearProductFilter() {
        clearProductForm();
    }

    private void clearProductForm() {
        txtProdId.setText("");
        txtProdId.setEditable(true);
        txtProdName.setText("");
        cmbProdCategory.setSelectedIndex(0);
        txtProdPrice.setText("");
        txtProdThreshold.setText("");
        tblProducts.clearSelection();
        selectedProductId = null;
        if (txtBatchProdId != null) {
            txtBatchProdId.setText("");
        }
        refreshBatchTable();
    }

    // =========================================================================
    // Batch Actions & Event Handlers
    // =========================================================================

    private void refreshBatchTable() {
        batchTableModel.setRowCount(0);
        List<ProductBatch> batches;

        if (selectedProductId != null && !selectedProductId.trim().isEmpty()) {
            batches = inventoryService.getBatchesByProductId(selectedProductId);
            lblBatchHeader.setText("Showing batches for Product: " + selectedProductId +
                    " (" + batches.size() + " batch" + (batches.size() == 1 ? "" : "es") + ")");
            if (btnShowAllBatches != null) {
                btnShowAllBatches.setEnabled(true);
                btnShowAllBatches.setText("Show All Batches");
            }
        } else {
            batches = inventoryService.getAllBatches();
            lblBatchHeader.setText("Showing all batches across all products (" + batches.size() + " total)");
            if (btnShowAllBatches != null) {
                btnShowAllBatches.setEnabled(false);
                btnShowAllBatches.setText("Showing All Batches");
            }
        }

        LocalDate today = LocalDate.now();
        for (ProductBatch b : batches) {
            String expiryStatus;
            if (b.getExpiryDate().isBefore(today)) {
                expiryStatus = "EXPIRED";
            } else if (b.getExpiryDate().isEqual(today)) {
                expiryStatus = "EXPIRES TODAY";
            } else {
                expiryStatus = "ACTIVE";
            }

            batchTableModel.addRow(new Object[]{
                    b.getBatchId(),
                    b.getProductId(),
                    b.getQuantity(),
                    b.getExpiryDate().toString(),
                    expiryStatus
            });
        }
    }

    private void onBatchSelected() {
        int row = tblBatches.getSelectedRow();
        if (row < 0) return;

        String batchId = (String) tblBatches.getValueAt(row, 0);
        ProductBatch b = inventoryService.getBatchById(batchId);
        if (b != null) {
            txtBatchId.setText(b.getBatchId());
            txtBatchId.setEditable(false);
            txtBatchProdId.setText(b.getProductId());
            txtBatchQuantity.setText(String.valueOf(b.getQuantity()));
            txtBatchExpiryDate.setText(b.getExpiryDate().toString());
        }
    }

    private void handleAddBatch() {
        try {
            String batchId = txtBatchId.getText() != null ? txtBatchId.getText().trim().toUpperCase() : "";
            String prodId = txtBatchProdId.getText() != null ? txtBatchProdId.getText().trim().toUpperCase() : "";
            txtBatchId.setText(batchId);
            txtBatchProdId.setText(prodId);
            String qtyStr = txtBatchQuantity.getText();
            String dateStr = txtBatchExpiryDate.getText();

            if (batchId.isEmpty()) {
                showError("Batch ID is required.");
                txtBatchId.requestFocus();
                return;
            }
            if (prodId.isEmpty()) {
                showError("Product ID is required for the batch.");
                txtBatchProdId.requestFocus();
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyStr.trim());
            } catch (Exception ex) {
                showError("Invalid quantity. Please enter a valid positive integer.");
                txtBatchQuantity.requestFocus();
                return;
            }

            LocalDate expiryDate;
            try {
                expiryDate = LocalDate.parse(dateStr.trim());
            } catch (DateTimeParseException ex) {
                showError("Invalid date format. Please use YYYY-MM-DD (e.g. " + LocalDate.now().plusMonths(6) + ")");
                txtBatchExpiryDate.requestFocus();
                return;
            }

            ProductBatch batch = new ProductBatch(batchId, prodId, qty, expiryDate);
            inventoryService.addBatch(batch);

            JOptionPane.showMessageDialog(this,
                    "Batch '" + batch.getBatchId() + "' added successfully for Product '" + batch.getProductId() + "'!",
                    "Batch Added", JOptionPane.INFORMATION_MESSAGE);

            clearBatchForm();
            refreshData();

        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleDeleteBatch() {
        String batchId = txtBatchId.getText() != null ? txtBatchId.getText().trim().toUpperCase() : "";
        if (batchId.isEmpty()) {
            showError("Select a batch from the table to delete.");
            return;
        }
        String cleanId = batchId;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete Batch '" + cleanId + "'?",
                "Confirm Batch Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                inventoryService.deleteBatch(cleanId);
                JOptionPane.showMessageDialog(this,
                        "Batch '" + cleanId + "' deleted successfully.",
                        "Batch Deleted", JOptionPane.INFORMATION_MESSAGE);
                clearBatchForm();
                refreshData();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void clearBatchForm() {
        txtBatchId.setText("");
        txtBatchId.setEditable(true);
        if (selectedProductId != null) {
            txtBatchProdId.setText(selectedProductId);
        } else {
            txtBatchProdId.setText("");
        }
        txtBatchQuantity.setText("");
        txtBatchExpiryDate.setText("");
        tblBatches.clearSelection();
    }

    // =========================================================================
    // Helpers & Status
    // =========================================================================

    private void updateSummaryBanner() {
        List<Product> products = inventoryService.getAllProducts();
        List<ProductBatch> batches = inventoryService.getAllBatches();

        int lowStockCount = 0;
        int outOfStockCount = 0;
        for (Product p : products) {
            int stock = inventoryService.getTotalStock(p.getProductId());
            if (stock == 0) {
                outOfStockCount++;
            } else if (stock <= p.getLowStockThreshold()) {
                lowStockCount++;
            }
        }

        lblSummary.setText(String.format(
                "Catalog: %d products (%d low-stock, %d out-of-stock) | Physical: %d total batches",
                products.size(), lowStockCount, outOfStockCount, batches.size()
        ));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "ShelfLife - Input / Validation Notice",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Enforces role-based permissions on product and batch mutation actions.
     * Cashiers have read-only access for catalog lookup; Admins have full CRUD.
     */
    public void applyRolePermissions(Role role) {
        boolean canManage = (role == Role.ADMIN);
        if (btnAddProduct != null) {
            btnAddProduct.setEnabled(canManage);
            btnAddProduct.setToolTipText(canManage ? "Add new product" : "Requires Administrator role");
        }
        if (btnUpdateProduct != null) {
            btnUpdateProduct.setEnabled(canManage);
            btnUpdateProduct.setToolTipText(canManage ? "Update selected product" : "Requires Administrator role");
        }
        if (btnDeleteProduct != null) {
            btnDeleteProduct.setEnabled(canManage);
            btnDeleteProduct.setToolTipText(canManage ? "Delete selected product" : "Requires Administrator role");
        }
        if (btnToggleActive != null) {
            btnToggleActive.setEnabled(canManage);
            btnToggleActive.setToolTipText(canManage ? "Toggle active/inactive status" : "Requires Administrator role");
        }
        if (btnAddBatch != null) {
            btnAddBatch.setEnabled(canManage);
            btnAddBatch.setToolTipText(canManage ? "Add inventory batch" : "Requires Administrator role");
        }
        if (btnDeleteBatch != null) {
            btnDeleteBatch.setEnabled(canManage);
            btnDeleteBatch.setToolTipText(canManage ? "Delete batch" : "Requires Administrator role");
        }
    }

    private void exportProductsCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("products_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                pw.write('\uFEFF'); // UTF-8 BOM
                pw.println("productId,name,category,sellingPrice,lowStockThreshold,active,barcode,sku");
                for (Product p : inventoryService.getAllProducts()) {
                    pw.println(String.format("\"%s\",\"%s\",\"%s\",%s,%d,%b,\"%s\",\"%s\"",
                            escapeCsv(p.getProductId()),
                            escapeCsv(p.getName()),
                            escapeCsv(p.getCategory()),
                            p.getSellingPrice().toPlainString(),
                            p.getLowStockThreshold(),
                            p.isActive(),
                            escapeCsv(p.getBarcode()),
                            escapeCsv(p.getSku())));
                }
                JOptionPane.showMessageDialog(this, "Products exported successfully to:\n" + f.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importProductsCsv() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            int imported = 0;
            int errors = 0;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line = br.readLine(); // header
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (parts.length >= 5) {
                        try {
                            String pId = cleanCsvCell(parts[0]);
                            String pName = cleanCsvCell(parts[1]);
                            String pCat = cleanCsvCell(parts[2]);
                            BigDecimal price = new BigDecimal(cleanCsvCell(parts[3]));
                            int threshold = Integer.parseInt(cleanCsvCell(parts[4]));
                            boolean active = parts.length > 5 ? Boolean.parseBoolean(cleanCsvCell(parts[5])) : true;
                            String barcode = parts.length > 6 ? cleanCsvCell(parts[6]) : "";
                            String sku = parts.length > 7 ? cleanCsvCell(parts[7]) : "";

                            Product existing = inventoryService.getProductById(pId);
                            if (existing == null) {
                                Product np = new Product(pId, pName, pCat, price, threshold, barcode, sku, active);
                                inventoryService.addProduct(np);
                            } else {
                                existing.setName(pName);
                                existing.setCategory(pCat);
                                existing.setSellingPrice(price);
                                existing.setLowStockThreshold(threshold);
                                existing.setActive(active);
                                existing.setBarcode(barcode);
                                existing.setSku(sku);
                                inventoryService.updateProduct(existing);
                            }
                            imported++;
                        } catch (Exception rowEx) {
                            errors++;
                        }
                    }
                }
                refreshData();
                JOptionPane.showMessageDialog(this, String.format("CSV Import finished: %d products processed, %d errors/skipped.", imported, errors), "Import Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "CSV Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    private String cleanCsvCell(String val) {
        if (val == null) return "";
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1).replace("\"\"", "\"");
        }
        return val.trim();
    }
}
