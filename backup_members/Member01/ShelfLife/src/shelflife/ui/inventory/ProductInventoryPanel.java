package shelflife.ui.inventory;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.service.InventoryService;
import shelflife.storage.StorageException;

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
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Product and Inventory Management Swing Panel.
 * Primary deliverable for Member 1.
 * Provides complete catalog management, product search, batch tracking,
 * real-time validation, and protected deletion.
 */
public class ProductInventoryPanel extends JPanel {

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
        setBackground(new Color(245, 247, 250));

        // 1. Top Header & Search Bar
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Workspace (Split into Products & Batches)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createProductSection(),
                createBatchSection());
        splitPane.setResizeWeight(0.55);
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
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(33, 43, 54));

        lblSummary = new JLabel("Loading inventory metrics...");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSummary.setForeground(new Color(99, 115, 129));

        titlePanel.add(lblTitle);
        titlePanel.add(lblSummary);

        // Search Bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);

        JLabel lblSearchPrompt = new JLabel("Search Catalog:");
        lblSearchPrompt.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtSearch = new JTextField(16);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setToolTipText("Enter product ID, name, or category");
        txtSearch.addActionListener(e -> performSearch());

        btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(new Color(24, 144, 255));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> performSearch());

        btnClearSearch = new JButton("Reset");
        btnClearSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            refreshData();
        });

        searchPanel.add(lblSearchPrompt);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClearSearch);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 224, 230)),
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
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(33, 43, 54)
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
        tblProducts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblProducts.setRowHeight(24);
        tblProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProducts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblProducts.getTableHeader().setReorderingAllowed(false);

        // Custom status cell renderer
        tblProducts.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if ("LOW STOCK".equals(value)) {
                    setForeground(new Color(207, 19, 34));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("OUT OF STOCK".equals(value)) {
                    setForeground(new Color(150, 20, 20));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(56, 158, 13));
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(Color.WHITE);
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
        formContainer.setBorder(BorderFactory.createTitledBorder("Product Details Form"));

        JPanel fieldsPanel = new JPanel(new GridLayout(5, 2, 6, 6));
        fieldsPanel.setOpaque(false);

        fieldsPanel.add(new JLabel("Product ID:"));
        txtProdId = new JTextField();
        fieldsPanel.add(txtProdId);

        fieldsPanel.add(new JLabel("Product Name:"));
        txtProdName = new JTextField();
        fieldsPanel.add(txtProdName);

        fieldsPanel.add(new JLabel("Category:"));
        String[] defaultCategories = {"Dairy", "Bakery", "Produce", "Beverages", "Meat & Seafood", "Canned Goods", "Snacks", "Other"};
        cmbProdCategory = new JComboBox<>(defaultCategories);
        cmbProdCategory.setEditable(true);
        fieldsPanel.add(cmbProdCategory);

        fieldsPanel.add(new JLabel("Selling Price (Rs.):"));
        txtProdPrice = new JTextField();
        fieldsPanel.add(txtProdPrice);

        fieldsPanel.add(new JLabel("Low Stock Threshold:"));
        txtProdThreshold = new JTextField();
        fieldsPanel.add(txtProdThreshold);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        btnPanel.setOpaque(false);

        btnAddProduct = new JButton("Add Product");
        btnAddProduct.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddProduct.setBackground(new Color(56, 158, 13));
        btnAddProduct.setForeground(Color.WHITE);
        btnAddProduct.setFocusPainted(false);
        btnAddProduct.addActionListener(e -> handleAddProduct());

        btnUpdateProduct = new JButton("Update Product");
        btnUpdateProduct.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnUpdateProduct.setFocusPainted(false);
        btnUpdateProduct.addActionListener(e -> handleUpdateProduct());

        btnDeleteProduct = new JButton("Delete Product");
        btnDeleteProduct.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteProduct.setBackground(new Color(255, 77, 79));
        btnDeleteProduct.setForeground(Color.WHITE);
        btnDeleteProduct.setFocusPainted(false);
        btnDeleteProduct.addActionListener(e -> handleDeleteProduct());

        btnClearProductForm = new JButton("Clear Form");
        btnClearProductForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearProductForm.setFocusPainted(false);
        btnClearProductForm.addActionListener(e -> clearProductForm());

        btnPanel.add(btnAddProduct);
        btnPanel.add(btnUpdateProduct);
        btnPanel.add(btnDeleteProduct);
        btnPanel.add(btnClearProductForm);

        formContainer.add(fieldsPanel, BorderLayout.CENTER);
        formContainer.add(btnPanel, BorderLayout.SOUTH);

        return formContainer;
    }

    private JPanel createBatchSection() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                "Product Batches",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(33, 43, 54)
        ));

        String[] batchColumns = {"Batch ID", "Product ID", "Quantity", "Expiry Date", "Expiry Status"};
        batchTableModel = new DefaultTableModel(batchColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblBatches = new JTable(batchTableModel);
        tblBatches.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblBatches.setRowHeight(24);
        tblBatches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblBatches.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblBatches.getTableHeader().setReorderingAllowed(false);

        // Expiry Status Renderer
        tblBatches.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                if ("EXPIRED".equals(value)) {
                    setForeground(new Color(207, 19, 34));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("EXPIRES TODAY".equals(value)) {
                    setForeground(new Color(212, 107, 8));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(new Color(56, 158, 13));
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(Color.WHITE);
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

        btnShowAllBatches = new JButton("Show All Batches");
        btnShowAllBatches.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnShowAllBatches.setFocusPainted(false);
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
        formContainer.setBorder(BorderFactory.createTitledBorder("Batch Details Form"));

        JPanel fieldsPanel = new JPanel(new GridLayout(4, 2, 6, 6));
        fieldsPanel.setOpaque(false);

        fieldsPanel.add(new JLabel("Batch ID:"));
        txtBatchId = new JTextField();
        fieldsPanel.add(txtBatchId);

        fieldsPanel.add(new JLabel("Product ID:"));
        txtBatchProdId = new JTextField();
        fieldsPanel.add(txtBatchProdId);

        fieldsPanel.add(new JLabel("Quantity:"));
        txtBatchQuantity = new JTextField();
        fieldsPanel.add(txtBatchQuantity);

        fieldsPanel.add(new JLabel("Expiry Date (YYYY-MM-DD):"));
        txtBatchExpiryDate = new JTextField();
        txtBatchExpiryDate.setToolTipText("e.g. " + LocalDate.now().plusMonths(3));
        fieldsPanel.add(txtBatchExpiryDate);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        btnPanel.setOpaque(false);

        btnAddBatch = new JButton("Add Batch");
        btnAddBatch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddBatch.setBackground(new Color(24, 144, 255));
        btnAddBatch.setForeground(Color.WHITE);
        btnAddBatch.setFocusPainted(false);
        btnAddBatch.addActionListener(e -> handleAddBatch());

        btnDeleteBatch = new JButton("Delete Batch");
        btnDeleteBatch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDeleteBatch.setBackground(new Color(255, 77, 79));
        btnDeleteBatch.setForeground(Color.WHITE);
        btnDeleteBatch.setFocusPainted(false);
        btnDeleteBatch.addActionListener(e -> handleDeleteBatch());

        btnClearBatchForm = new JButton("Clear Batch Form");
        btnClearBatchForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearBatchForm.setFocusPainted(false);
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
            if (stock == 0) {
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

        } catch (IllegalArgumentException | StorageException ex) {
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

        } catch (IllegalArgumentException | StorageException ex) {
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
            showError("Cannot delete Product '" + cleanId + "':\n" +
                    "It has active batch records or historical transaction data associated with it.\n" +
                    "Per project integrity rules, products with dependencies cannot be deleted.");
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

        } catch (IllegalArgumentException | StorageException ex) {
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
}
