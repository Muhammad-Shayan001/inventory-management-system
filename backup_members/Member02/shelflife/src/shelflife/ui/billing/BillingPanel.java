package shelflife.ui.billing;

import shelflife.model.*;
import shelflife.service.InventoryService;
import shelflife.service.SaleException;
import shelflife.service.SalesService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
 * Provides cashier point-of-sale functionality: product selection, temporary
 * cart management, real-time monetary calculations using BigDecimal, stock validation,
 * and invocation of SalesService for FEFO batch allocation and transactional checkout.
 */
public class BillingPanel extends JPanel {
    private final InventoryService inventoryService;
    private final SalesService salesService;

    // Temporary cart storage (UI layer only; does not mutate inventory until confirmed checkout)
    private final List<CartItem> cartItems = new ArrayList<>();

    // Swing UI Components
    private JComboBox<Product> cmbProducts;
    private JLabel lblAvailableStock;
    private JLabel lblUnitPrice;
    private JSpinner spnQuantity;
    private JButton btnAddToCart;
    private JButton btnUpdateQuantity;
    private JButton btnRemoveSelected;
    private JButton btnClearCart;

    private JTable tblCart;
    private DefaultTableModel cartTableModel;

    private JLabel lblSubtotalValue;
    private JLabel lblTaxValue;
    private JLabel lblFinalTotalValue;
    private JComboBox<PaymentMethod> cmbPaymentMethod;
    private JButton btnCheckout;
    private JButton btnViewHistory;
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
     * Builds and arranges all Swing UI widgets using professional layout hierarchies.
     */
    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Header
        JPanel pnlHeader = createHeaderPanel();
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Main Content (Left: Product Selector & Actions | Right: Cart Table & Summary)
        JPanel pnlMain = new JPanel(new BorderLayout(12, 12));

        JPanel pnlLeft = createProductSelectionPanel();
        JPanel pnlRight = createCartAndCheckoutPanel();

        pnlMain.add(pnlLeft, BorderLayout.WEST);
        pnlMain.add(pnlRight, BorderLayout.CENTER);
        add(pnlMain, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 222, 232), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblTitle = new JLabel("ShelfLife Point-of-Sale (POS) & Billing");
        lblTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTitle.setForeground(new Color(33, 43, 54));

        JLabel lblSubtitle = new JLabel("Member 3 Module: Multi-Batch FEFO Allocation | Strict Financial Accuracy");
        lblSubtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(99, 115, 129));

        panel.add(lblTitle, BorderLayout.WEST);
        panel.add(lblSubtitle, BorderLayout.EAST);
        return panel;
    }

    private JPanel createProductSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(340, 500));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Product Selection & Cart Staging",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 12)));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Product Dropdown
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

        // Unit Price
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Catalog Price:"), gbc);

        lblUnitPrice = new JLabel("$0.00");
        lblUnitPrice.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        formGrid.add(lblUnitPrice, gbc);

        // Sellable Stock
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Sellable Stock:"), gbc);

        lblAvailableStock = new JLabel("0 units (non-expired)");
        lblAvailableStock.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        lblAvailableStock.setForeground(new Color(46, 125, 50));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.7;
        formGrid.add(lblAvailableStock, gbc);

        // Quantity Input
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        formGrid.add(new JLabel("Quantity:"), gbc);

        spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0.7;
        formGrid.add(spnQuantity, gbc);

        panel.add(formGrid);
        panel.add(Box.createVerticalStrut(12));

        // Action Buttons for Cart
        JPanel btnGrid = new JPanel(new GridLayout(4, 1, 8, 8));
        btnGrid.setBorder(new EmptyBorder(5, 10, 10, 10));

        btnAddToCart = new JButton("Add to Cart");
        btnAddToCart.addActionListener(e -> onAddToCart());

        btnUpdateQuantity = new JButton("Update Selected Quantity");
        btnUpdateQuantity.setEnabled(false);
        btnUpdateQuantity.addActionListener(e -> onUpdateSelectedQuantity());

        btnRemoveSelected = new JButton("Remove Selected Item");
        btnRemoveSelected.setEnabled(false);
        btnRemoveSelected.addActionListener(e -> onRemoveSelectedItem());

        btnClearCart = new JButton("Clear Entire Cart");
        btnClearCart.addActionListener(e -> onClearCart());

        btnGrid.add(btnAddToCart);
        btnGrid.add(btnUpdateQuantity);
        btnGrid.add(btnRemoveSelected);
        btnGrid.add(btnClearCart);

        panel.add(btnGrid);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createCartAndCheckoutPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Cart Table
        String[] columns = {"#", "Product ID", "Product Name", "Unit Price ($)", "Quantity", "Line Total ($)"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCart = new JTable(cartTableModel);
        tblCart.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCart.setRowHeight(24);

        // Align numeric columns to the right
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblCart.getColumnModel().getColumn(0).setPreferredWidth(35);
        tblCart.getColumnModel().getColumn(1).setPreferredWidth(80);
        tblCart.getColumnModel().getColumn(2).setPreferredWidth(200);
        tblCart.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblCart.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tblCart.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblCart.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tblCart.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblCart.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        tblCart.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean hasSelection = tblCart.getSelectedRow() >= 0;
                btnUpdateQuantity.setEnabled(hasSelection);
                btnRemoveSelected.setEnabled(hasSelection);
                if (hasSelection) {
                    int selectedRow = tblCart.getSelectedRow();
                    if (selectedRow < cartItems.size()) {
                        spnQuantity.setValue(cartItems.get(selectedRow).getQuantity());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblCart);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Temporary Cart (Pre-Allocation)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font(Font.SANS_SERIF, Font.BOLD, 12)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom Checkout and Financials Panel
        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Financial Totals Summary
        JPanel pnlTotals = new JPanel(new GridLayout(3, 2, 8, 4));
        pnlTotals.add(new JLabel("Cart Subtotal:"));
        lblSubtotalValue = new JLabel("$0.00", SwingConstants.RIGHT);
        lblSubtotalValue.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        pnlTotals.add(lblSubtotalValue);

        pnlTotals.add(new JLabel("Tax (0.00%):"));
        lblTaxValue = new JLabel("$0.00", SwingConstants.RIGHT);
        lblTaxValue.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        pnlTotals.add(lblTaxValue);

        JLabel lblTotalTitle = new JLabel("Final Total:");
        lblTotalTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        pnlTotals.add(lblTotalTitle);

        lblFinalTotalValue = new JLabel("$0.00", SwingConstants.RIGHT);
        lblFinalTotalValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblFinalTotalValue.setForeground(new Color(21, 101, 192));
        pnlTotals.add(lblFinalTotalValue);

        pnlBottom.add(pnlTotals, BorderLayout.WEST);

        // Checkout & History Controls
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        pnlActions.add(new JLabel("Payment Method:"));
        cmbPaymentMethod = new JComboBox<>(PaymentMethod.values());
        pnlActions.add(cmbPaymentMethod);

        btnCheckout = new JButton("Confirm & Complete Sale");
        btnCheckout.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        btnCheckout.setBackground(new Color(46, 125, 50));
        btnCheckout.setForeground(Color.BLACK);
        btnCheckout.addActionListener(e -> onCheckout());
        pnlActions.add(btnCheckout);

        btnViewHistory = new JButton("View Sales History");
        btnViewHistory.addActionListener(e -> onViewHistory());
        pnlActions.add(btnViewHistory);

        btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshData());
        pnlActions.add(btnRefresh);

        pnlBottom.add(pnlActions, BorderLayout.EAST);
        panel.add(pnlBottom, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Refreshes catalog and stock levels from the shared InventoryService.
     * Preserves temporary cart items while refreshing product labels.
     */
    public void refreshData() {
        inventoryService.reload();
        salesService.reload();

        Product previouslySelected = (Product) cmbProducts.getSelectedItem();
        cmbProducts.removeAllItems();

        List<Product> products = inventoryService.getAllProducts();
        if (products != null) {
            for (Product p : products) {
                cmbProducts.addItem(p);
            }
        }

        if (previouslySelected != null && products != null) {
            for (Product p : products) {
                if (p.getProductId().equals(previouslySelected.getProductId())) {
                    cmbProducts.setSelectedItem(p);
                    break;
                }
            }
        }

        updateProductDetailLabels();
        recalculateCartTotals();
    }

    private void updateProductDetailLabels() {
        Product selected = (Product) cmbProducts.getSelectedItem();
        if (selected == null) {
            lblUnitPrice.setText("$0.00");
            lblAvailableStock.setText("0 units (non-expired)");
            lblAvailableStock.setForeground(Color.GRAY);
            return;
        }

        lblUnitPrice.setText("$" + selected.getUnitPrice().toPlainString());
        int sellableStock = inventoryService.getSellableStock(selected.getProductId(), LocalDate.now());
        lblAvailableStock.setText(sellableStock + " units (non-expired)");
        if (sellableStock > 0) {
            lblAvailableStock.setForeground(new Color(46, 125, 50));
        } else {
            lblAvailableStock.setForeground(new Color(198, 40, 40));
        }
    }

    private void onAddToCart() {
        Product selected = (Product) cmbProducts.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a product from the list.", "No Product Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestedQty = (Integer) spnQuantity.getValue();
        if (requestedQty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int currentSellable = inventoryService.getSellableStock(selected.getProductId(), LocalDate.now());

        // Check if item is already in temporary cart
        CartItem existingCartItem = null;
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(selected.getProductId())) {
                existingCartItem = item;
                break;
            }
        }

        int prospectiveTotalQty = requestedQty;
        if (existingCartItem != null) {
            prospectiveTotalQty += existingCartItem.getQuantity();
        }

        // GUI early feedback warning (Note: SalesService owns final authoritative validation at commit time)
        if (prospectiveTotalQty > currentSellable) {
            int option = JOptionPane.showConfirmDialog(this,
                    "Warning: Requested quantity (" + prospectiveTotalQty + ") exceeds currently known sellable stock (" + currentSellable + ").\n" +
                            "Expired batches are excluded from sellable stock.\n" +
                            "Do you still want to stage this item in your cart?",
                    "Stock Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (existingCartItem != null) {
            existingCartItem.addQuantity(requestedQty);
        } else {
            cartItems.add(new CartItem(selected, requestedQty));
        }

        rebuildCartTable();
        recalculateCartTotals();
    }

    private void onUpdateSelectedQuantity() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= cartItems.size()) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart to update.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int newQty = (Integer) spnQuantity.getValue();
        if (newQty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CartItem item = cartItems.get(selectedRow);
        item.setQuantity(newQty);

        rebuildCartTable();
        recalculateCartTotals();
    }

    private void onRemoveSelectedItem() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= cartItems.size()) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart to remove.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        cartItems.remove(selectedRow);
        rebuildCartTable();
        recalculateCartTotals();
    }

    private void onClearCart() {
        if (cartItems.isEmpty()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all items from the temporary cart?\n(Inventory is not affected by clearing the cart)",
                "Confirm Clear Cart",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            cartItems.clear();
            rebuildCartTable();
            recalculateCartTotals();
        }
    }

    private void rebuildCartTable() {
        cartTableModel.setRowCount(0);
        int index = 1;
        for (CartItem item : cartItems) {
            cartTableModel.addRow(new Object[]{
                    index++,
                    item.getProductId(),
                    item.getProductName(),
                    item.getUnitPrice().toPlainString(),
                    item.getQuantity(),
                    item.getLineTotal().toPlainString()
            });
        }
    }

    private void recalculateCartTotals() {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (CartItem item : cartItems) {
            subtotal = subtotal.add(item.getLineTotal());
        }
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalTotal = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        lblSubtotalValue.setText("$" + subtotal.toPlainString());
        lblTaxValue.setText("$" + tax.toPlainString());
        lblFinalTotalValue.setText("$" + finalTotal.toPlainString());
    }

    /**
     * Executes the checkout procedure:
     * 1. Validates temporary cart is non-empty.
     * 2. Protects against duplicate clicks by disabling checkout button.
     * 3. Confirms user intent.
     * 4. Invokes SalesService.commitSale for FEFO allocation and atomic persistence.
     * 5. Displays formatted receipt or error dialog.
     */
    private void onCheckout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The cart is empty. Please add items before checking out.", "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PaymentMethod paymentMethod = (PaymentMethod) cmbPaymentMethod.getSelectedItem();
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.CASH;
        }

        // Calculate totals for confirmation prompt
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int totalUnits = 0;
        for (CartItem ci : cartItems) {
            total = total.add(ci.getLineTotal());
            totalUnits += ci.getQuantity();
        }

        // Duplicate-click lock
        btnCheckout.setEnabled(false);

        try {
            // Business Rule: Ask for confirmation before committing a sale
            String confirmPrompt = String.format(
                    "Confirm Sales Checkout:\n\n" +
                            "Distinct Products: %d\n" +
                            "Total Units:       %d\n" +
                            "Total Amount:      $%s\n" +
                            "Payment Method:    %s\n\n" +
                            "Proceed with FEFO stock allocation and atomic commit?",
                    cartItems.size(), totalUnits, total.toPlainString(), paymentMethod.getDisplayName());

            int confirm = JOptionPane.showConfirmDialog(this, confirmPrompt, "Confirm Sale Checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                // Cancelled checkout never modifies inventory
                return;
            }

            // Prepare payload for SalesService
            Map<String, Integer> productQuantities = new LinkedHashMap<>();
            for (CartItem item : cartItems) {
                productQuantities.put(item.getProductId(), item.getQuantity());
            }

            // SalesService owns final stock validation, FEFO allocation, and atomic persistence
            Sale committedSale = salesService.commitSale(productQuantities, paymentMethod);

            // On success: clear cart, refresh stock, and display official receipt
            cartItems.clear();
            rebuildCartTable();
            recalculateCartTotals();
            refreshData();

            ReceiptDialog.showReceipt(this, committedSale);

        } catch (SaleException se) {
            // Stock shortage, expired batches, or persistence transaction failure
            JOptionPane.showMessageDialog(this,
                    se.getMessage(),
                    "Sale Commit Rejected",
                    JOptionPane.ERROR_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An unexpected error occurred during checkout: " + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
            refreshData();
        } finally {
            // Always restore checkout button
            btnCheckout.setEnabled(true);
        }
    }

    private void onViewHistory() {
        SalesHistoryDialog dialog = new SalesHistoryDialog(SwingUtilities.getWindowAncestor(this), salesService);
        dialog.setVisible(true);
    }
}
