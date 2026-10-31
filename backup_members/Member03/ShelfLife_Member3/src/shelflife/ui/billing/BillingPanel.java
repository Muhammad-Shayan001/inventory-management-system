package shelflife.ui.billing;

import shelflife.model.*;
import shelflife.service.InventoryService;
import shelflife.service.SaleException;
import shelflife.service.SalesService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Main Billing and Sales Management Swing panel for ShelfLife (Member 3).
 *
 * Provides complete Point-of-Sale (POS) functionality:
 * 1. Product selection with live non-expired sellable stock display.
 * 2. In-memory temporary cart staging (cart changes never mutate inventory).
 * 3. Exact BigDecimal financial arithmetic for unit price, line totals, and grand totals.
 * 4. Pre-allocation stock validation and confirmation dialog before commit.
 * 5. Transactional handoff to SalesService for FEFO batch allocation and atomic persistence.
 * 6. Receipt generation and sales history browsing.
 */
public class BillingPanel extends JPanel {

    private final InventoryService inventoryService;
    private final SalesService salesService;

    // Temporary Cart Staging List (UI layer only; inventory unaffected until checkout commit)
    private final List<CartItem> cartItems = new ArrayList<>();

    // Swing Components — Selection Panel
    private JComboBox<Product> cmbProducts;
    private JLabel lblCategoryVal;
    private JLabel lblUnitPriceVal;
    private JLabel lblAvailableStockVal;
    private JLabel lblStockAlertNote;
    private JSpinner spnQuantity;
    private JButton btnAddToCart;
    private JButton btnUpdateQuantity;
    private JButton btnRemoveSelected;
    private JButton btnClearCart;

    // Swing Components — Cart Table & Summary
    private JTable tblCart;
    private DefaultTableModel cartTableModel;
    private JLabel lblSubtotalVal;
    private JLabel lblTaxVal;
    private JLabel lblFinalTotalVal;
    private JComboBox<PaymentMethod> cmbPaymentMethod;
    private JButton btnCheckout;
    private JButton btnSalesHistory;
    private JButton btnRefresh;

    /**
     * Required constructor adhering to ShelfLife team architectural specifications.
     *
     * @param inventoryService Shared inventory service contract
     * @param salesService     Shared sales service contract
     */
    public BillingPanel(InventoryService inventoryService, SalesService salesService) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
        this.salesService = Objects.requireNonNull(salesService, "salesService cannot be null");

        initComponents();
        refreshData();
    }

    /**
     * Initializes and arranges all Swing UI widgets.
     */
    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Header Panel
        JPanel pnlHeader = createHeaderPanel();
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Central Area (Left: Selection & Actions | Right: Cart Table & Summary)
        JPanel pnlCenter = new JPanel(new BorderLayout(12, 12));
        JPanel pnlLeft = createProductSelectionPanel();
        JPanel pnlRight = createCartAndCheckoutPanel();

        pnlCenter.add(pnlLeft, BorderLayout.WEST);
        pnlCenter.add(pnlRight, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(243, 246, 250));
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 218, 228), 1),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JLabel lblTitle = new JLabel("ShelfLife Point-of-Sale (POS) & Billing");
        lblTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTitle.setForeground(new Color(26, 36, 48));

        JLabel lblSubtitle = new JLabel("Member 3 Module: Multi-Batch FEFO Allocation | Strict Financial Accuracy");
        lblSubtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(100, 115, 130));

        panel.add(lblTitle, BorderLayout.WEST);
        panel.add(lblSubtitle, BorderLayout.EAST);
        return panel;
    }

    private JPanel createProductSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(360, 520));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Product Selection & Staging",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 13)));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Product Dropdown
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Product:"), gbc);

        cmbProducts = new JComboBox<>();
        cmbProducts.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product) {
                    Product p = (Product) value;
                    setText(p.getName() + " (" + p.getProductId() + ")");
                } else if (value == null) {
                    setText("-- Select Product --");
                }
                return this;
            }
        });
        cmbProducts.addActionListener(e -> updateProductDetailLabels());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        formGrid.add(cmbProducts, gbc);

        // Row 1: Category
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Category:"), gbc);

        lblCategoryVal = new JLabel("-");
        lblCategoryVal.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        formGrid.add(lblCategoryVal, gbc);

        // Row 2: Unit Price
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Unit Price:"), gbc);

        lblUnitPriceVal = new JLabel("Rs. 0.00");
        lblUnitPriceVal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        lblUnitPriceVal.setForeground(new Color(24, 112, 61));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.7;
        formGrid.add(lblUnitPriceVal, gbc);

        // Row 3: Available Sellable Stock
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Sellable Stock:"), gbc);

        lblAvailableStockVal = new JLabel("0 units");
        lblAvailableStockVal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.7;
        formGrid.add(lblAvailableStockVal, gbc);

        // Row 4: Quantity Spinner
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Quantity:"), gbc);

        spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        spnQuantity.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 0.7;
        formGrid.add(spnQuantity, gbc);

        // Row 5: Stock Alert Note
        lblStockAlertNote = new JLabel(" ");
        lblStockAlertNote.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        lblStockAlertNote.setForeground(new Color(180, 80, 20));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        formGrid.add(lblStockAlertNote, gbc);

        panel.add(formGrid);
        panel.add(Box.createVerticalStrut(10));

        // Action Buttons Panel
        JPanel pnlButtons = new JPanel(new GridLayout(4, 1, 6, 6));
        pnlButtons.setBorder(new EmptyBorder(6, 12, 12, 12));

        btnAddToCart = new JButton("Add to Cart");
        btnAddToCart.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        btnAddToCart.setBackground(new Color(46, 117, 182));
        btnAddToCart.setForeground(Color.WHITE);
        btnAddToCart.setFocusPainted(false);
        btnAddToCart.addActionListener(e -> handleAddToCart());

        btnUpdateQuantity = new JButton("Update Selected Item Quantity");
        btnUpdateQuantity.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnUpdateQuantity.setFocusPainted(false);
        btnUpdateQuantity.addActionListener(e -> handleUpdateQuantity());

        btnRemoveSelected = new JButton("Remove Selected Item");
        btnRemoveSelected.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnRemoveSelected.setFocusPainted(false);
        btnRemoveSelected.addActionListener(e -> handleRemoveSelected());

        btnClearCart = new JButton("Clear Cart");
        btnClearCart.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnClearCart.setForeground(new Color(180, 40, 40));
        btnClearCart.setFocusPainted(false);
        btnClearCart.addActionListener(e -> handleClearCart());

        pnlButtons.add(btnAddToCart);
        pnlButtons.add(btnUpdateQuantity);
        pnlButtons.add(btnRemoveSelected);
        pnlButtons.add(btnClearCart);

        panel.add(pnlButtons);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createCartAndCheckoutPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // Cart Table
        String[] columns = {"Product ID", "Product Name", "Quantity", "Unit Price (Rs.)", "Line Total (Rs.)"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblCart = new JTable(cartTableModel);
        tblCart.setRowHeight(24);
        tblCart.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCart.getTableHeader().setReorderingAllowed(false);
        tblCart.getSelectionModel().addListSelectionListener(e -> syncSelectionWithForm());

        // Alignment renderers
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblCart.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tblCart.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblCart.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblCart.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tblCart);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Active Cart Items",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 13)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom Summary & Checkout Panel
        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Transaction Summary & Payment",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 13)));

        // Financial figures grid
        JPanel pnlTotals = new JPanel(new GridLayout(3, 2, 8, 4));
        pnlTotals.setBorder(new EmptyBorder(8, 12, 8, 12));

        pnlTotals.add(new JLabel("Subtotal:"));
        lblSubtotalVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblSubtotalVal.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        pnlTotals.add(lblSubtotalVal);

        pnlTotals.add(new JLabel("Tax / Surcharge (0%):"));
        lblTaxVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblTaxVal.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        pnlTotals.add(lblTaxVal);

        JLabel lblFinalTotalTitle = new JLabel("FINAL TOTAL:");
        lblFinalTotalTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        pnlTotals.add(lblFinalTotalTitle);

        lblFinalTotalVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblFinalTotalVal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        lblFinalTotalVal.setForeground(new Color(24, 112, 61));
        pnlTotals.add(lblFinalTotalVal);

        // Payment Method & Actions
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        pnlActions.add(new JLabel("Payment Method:"));
        cmbPaymentMethod = new JComboBox<>(PaymentMethod.values());
        cmbPaymentMethod.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        pnlActions.add(cmbPaymentMethod);

        btnCheckout = new JButton("Checkout & Commit Sale");
        btnCheckout.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnCheckout.setBackground(new Color(39, 147, 89));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFocusPainted(false);
        btnCheckout.addActionListener(e -> handleCheckout());
        pnlActions.add(btnCheckout);

        btnSalesHistory = new JButton("Sales History");
        btnSalesHistory.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnSalesHistory.setFocusPainted(false);
        btnSalesHistory.addActionListener(e -> openSalesHistory());
        pnlActions.add(btnSalesHistory);

        btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> refreshData());
        pnlActions.add(btnRefresh);

        pnlBottom.add(pnlTotals, BorderLayout.NORTH);
        pnlBottom.add(pnlActions, BorderLayout.SOUTH);

        panel.add(pnlBottom, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Core Business Operations & UI Logic
    // =========================================================================

    /**
     * Re-populates product catalog from InventoryService and refreshes stock metrics.
     * Required contract method for Member 5's main application window.
     */
    public void refreshData() {
        Product currentlySelected = (Product) cmbProducts.getSelectedItem();
        String selectedId = currentlySelected != null ? currentlySelected.getProductId() : null;

        cmbProducts.removeAllItems();
        List<Product> products = inventoryService.getAllProducts();
        if (products != null) {
            for (Product p : products) {
                cmbProducts.addItem(p);
            }
        }

        // Restore selection if still present
        if (selectedId != null) {
            for (int i = 0; i < cmbProducts.getItemCount(); i++) {
                Product p = cmbProducts.getItemAt(i);
                if (p.getProductId().equalsIgnoreCase(selectedId)) {
                    cmbProducts.setSelectedIndex(i);
                    break;
                }
            }
        }

        updateProductDetailLabels();
        updateSummaryTotals();
    }

    /**
     * Calculates the currently available sellable stock for a given product.
     * BUSINESS RULE: Exclude expired batches (a batch expiring today is sellable).
     */
    private int getAvailableSellableStock(String productId) {
        if (productId == null) return 0;
        LocalDate today = LocalDate.now();
        List<ProductBatch> batches = inventoryService.getBatchesByProductId(productId);
        if (batches == null || batches.isEmpty()) {
            batches = new ArrayList<>();
            for (ProductBatch b : inventoryService.getAllBatches()) {
                if (b.getProductId().equalsIgnoreCase(productId)) {
                    batches.add(b);
                }
            }
        }

        int sellable = 0;
        for (ProductBatch b : batches) {
            if (!b.isExpired(today) && b.getQuantity() > 0) {
                sellable += b.getQuantity();
            }
        }
        return sellable;
    }

    /**
     * Updates label details when the selected product changes.
     */
    private void updateProductDetailLabels() {
        Product selected = (Product) cmbProducts.getSelectedItem();
        if (selected == null) {
            lblCategoryVal.setText("-");
            lblUnitPriceVal.setText("Rs. 0.00");
            lblAvailableStockVal.setText("0 units");
            lblStockAlertNote.setText(" ");
            btnAddToCart.setEnabled(false);
            return;
        }

        lblCategoryVal.setText(selected.getCategory());
        lblUnitPriceVal.setText("Rs. " + selected.getSellingPrice().setScale(2, RoundingMode.HALF_UP));

        int sellableStock = getAvailableSellableStock(selected.getProductId());
        lblAvailableStockVal.setText(sellableStock + " units");

        if (sellableStock <= 0) {
            lblAvailableStockVal.setForeground(new Color(190, 40, 40));
            lblStockAlertNote.setText("Out of sellable stock (expired or 0 qty)!");
            btnAddToCart.setEnabled(false);
        } else if (sellableStock <= selected.getLowStockThreshold()) {
            lblAvailableStockVal.setForeground(new Color(200, 100, 20));
            lblStockAlertNote.setText("Warning: Low sellable stock (Threshold: " + selected.getLowStockThreshold() + ")");
            btnAddToCart.setEnabled(true);
        } else {
            lblAvailableStockVal.setForeground(new Color(24, 112, 61));
            lblStockAlertNote.setText("Stock healthy");
            btnAddToCart.setEnabled(true);
        }
    }

    /**
     * Adds the selected product and quantity to the staging cart.
     * BUSINESS RULE: Prevent sales that exceed available sellable stock.
     */
    private void handleAddToCart() {
        Product selected = (Product) cmbProducts.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a product first.", "Selection Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestedQty = (Integer) spnQuantity.getValue();
        if (requestedQty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int availableStock = getAvailableSellableStock(selected.getProductId());
        if (availableStock <= 0) {
            JOptionPane.showMessageDialog(this, "Cannot add product: No sellable non-expired stock available.", "Stock Unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if item already exists in temporary cart
        CartItem existingItem = null;
        for (CartItem item : cartItems) {
            if (item.getProductId().equalsIgnoreCase(selected.getProductId())) {
                existingItem = item;
                break;
            }
        }

        int totalRequestedInCart = (existingItem != null ? existingItem.getQuantity() : 0) + requestedQty;
        if (totalRequestedInCart > availableStock) {
            JOptionPane.showMessageDialog(this,
                    String.format("Requested total (%d) exceeds available sellable stock (%d) for '%s'.\n" +
                                  "Expired stock is strictly excluded from ordinary sales.",
                            totalRequestedInCart, availableStock, selected.getName()),
                    "Insufficient Sellable Stock", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (existingItem != null) {
            existingItem.addQuantity(requestedQty);
        } else {
            CartItem newItem = new CartItem(
                    selected.getProductId(),
                    selected.getName(),
                    requestedQty,
                    selected.getSellingPrice()
            );
            cartItems.add(newItem);
        }

        renderCartTable();
        updateSummaryTotals();
        spnQuantity.setValue(1);
    }

    /**
     * Updates quantity of the currently selected cart row.
     */
    private void handleUpdateQuantity() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= cartItems.size()) {
            JOptionPane.showMessageDialog(this, "Please select an item from the cart table to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CartItem item = cartItems.get(selectedRow);
        int newQty = (Integer) spnQuantity.getValue();
        if (newQty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int availableStock = getAvailableSellableStock(item.getProductId());
        if (newQty > availableStock) {
            JOptionPane.showMessageDialog(this,
                    String.format("New quantity (%d) exceeds available sellable stock (%d) for '%s'.",
                            newQty, availableStock, item.getProductName()),
                    "Insufficient Stock", JOptionPane.ERROR_MESSAGE);
            return;
        }

        item.setQuantity(newQty);
        renderCartTable();
        updateSummaryTotals();
    }

    /**
     * Removes the currently selected item from the cart.
     */
    private void handleRemoveSelected() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= cartItems.size()) {
            JOptionPane.showMessageDialog(this, "Please select an item from the cart table to remove.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cartItems.remove(selectedRow);
        renderCartTable();
        updateSummaryTotals();
    }

    /**
     * Clears all items from the temporary cart.
     * BUSINESS RULE: A cancelled cart never changes inventory.
     */
    private void handleClearCart() {
        if (cartItems.isEmpty()) return;

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all items from the cart?\n" +
                "Zero inventory changes will occur.",
                "Confirm Clear Cart", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            cartItems.clear();
            renderCartTable();
            updateSummaryTotals();
        }
    }

    /**
     * Commits the sale transaction:
     * 1. Confirms with the user.
     * 2. Delegates validation & FEFO allocation to SalesService.
     * 3. Displays receipt dialog upon success.
     * 4. Recovers smoothly without data loss on failure.
     */
    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot checkout: The cart is empty.", "Cart Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PaymentMethod selectedMethod = (PaymentMethod) cmbPaymentMethod.getSelectedItem();
        if (selectedMethod == null) {
            selectedMethod = PaymentMethod.CASH;
        }

        BigDecimal finalTotal = calculateSubtotal();

        // Confirmation dialog before committing
        String confirmMsg = String.format(
                "Confirm Checkout & Commit Sale?\n\n" +
                "Total Items in Cart : %d\n" +
                "Payment Method      : %s\n" +
                "Grand Total         : Rs. %.2f\n\n" +
                "This will allocate eligible non-expired batches using FEFO and update stock.",
                cartItems.size(), selectedMethod.getDisplayName(), finalTotal.doubleValue());

        int response = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm Sale Commitment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response != JOptionPane.YES_OPTION) {
            // Cancelled: zero inventory side-effects!
            return;
        }

        // Prepare quantity map for SalesService
        Map<String, Integer> productQuantities = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            productQuantities.put(item.getProductId(), item.getQuantity());
        }

        // Disable button during checkout to prevent accidental double-commit
        btnCheckout.setEnabled(false);

        try {
            Sale committedSale = salesService.commitSale(productQuantities, selectedMethod);

            // Sale succeeded: clear staging cart
            cartItems.clear();
            renderCartTable();
            updateSummaryTotals();
            refreshData();

            // Display itemized receipt
            ReceiptDialog dialog = new ReceiptDialog(SwingUtilities.getWindowAncestor(this), committedSale, committedSale.getItems());
            dialog.setVisible(true);

        } catch (SaleException ex) {
            // Business rule or persistence failure: cart remains intact, show clear explanation
            JOptionPane.showMessageDialog(this,
                    "Sale could not be committed:\n" + ex.getMessage(),
                    "Sale Failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnCheckout.setEnabled(true);
        }
    }

    private void renderCartTable() {
        cartTableModel.setRowCount(0);
        for (CartItem item : cartItems) {
            cartTableModel.addRow(new Object[]{
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    String.format("%.2f", item.getUnitPrice().doubleValue()),
                    String.format("%.2f", item.getLineTotal().doubleValue())
            });
        }
    }

    private BigDecimal calculateSubtotal() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (CartItem item : cartItems) {
            sum = sum.add(item.getLineTotal());
        }
        return sum;
    }

    private void updateSummaryTotals() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP); // 0% per university specification
        BigDecimal grandTotal = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        lblSubtotalVal.setText("Rs. " + subtotal);
        lblTaxVal.setText("Rs. " + tax);
        lblFinalTotalVal.setText("Rs. " + grandTotal);
    }

    private void syncSelectionWithForm() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < cartItems.size()) {
            CartItem item = cartItems.get(selectedRow);
            for (int i = 0; i < cmbProducts.getItemCount(); i++) {
                Product p = cmbProducts.getItemAt(i);
                if (p.getProductId().equalsIgnoreCase(item.getProductId())) {
                    cmbProducts.setSelectedIndex(i);
                    break;
                }
            }
            spnQuantity.setValue(item.getQuantity());
        }
    }

    private void openSalesHistory() {
        SalesHistoryDialog dialog = new SalesHistoryDialog(SwingUtilities.getWindowAncestor(this), salesService);
        dialog.setVisible(true);
    }
}
