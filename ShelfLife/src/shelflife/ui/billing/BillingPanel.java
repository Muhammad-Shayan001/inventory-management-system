package shelflife.ui.billing;

import shelflife.model.*;
import shelflife.service.*;
import shelflife.ui.ToastNotification;
import shelflife.ui.UITheme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Enhanced Main Billing & Commercial POS Panel for ShelfLife.
 *
 * Comprehensive Features:
 * 1. Product selection with live non-expired sellable stock display.
 * 2. Quick-Sale (Top Sellers) fast 1-click addition grid.
 * 3. In-memory temporary cart staging (cart changes never mutate inventory).
 * 4. Undo Remove Stack: allows rolling back an accidental cart item deletion.
 * 5. Hold Bill & Resume Bills: suspend transactions and restore later.
 * 6. Cash Received & Live Change Due Calculator with color-coded feedback.
 * 7. Multi-Tender Split Payment integration (Cash, Card, Easypaisa, JazzCash, Khata).
 * 8. Customer Credit (Khata / Udhaar) management access.
 * 9. Cashier Shift Management (Open / Close shift directly from POS header).
 * 10. Below-Cost sale warning protection: alerts cashier before selling at a loss.
 * 11. Transactional handoff to SalesService for FEFO batch allocation and atomic persistence.
 */
public class BillingPanel extends JPanel implements shelflife.ui.Refreshable {

    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final HeldBillService heldBillService;
    private final CustomerCreditService customerCreditService;
    private final CashierShiftService cashierShiftService;
    private final SettingsService settingsService;
    private final AuthService authService;

    private String currentCashier = "cashier";

    // Temporary Cart Staging List
    private final List<CartItem> cartItems = new ArrayList<>();
    // Undo Stack for removed cart items
    private final Deque<CartItem> undoStack = new ArrayDeque<>();

    // Quick-Sale Top Sellers panel
    private JPanel pnlQuickSale;

    // Swing Components — Selection Panel
    private JTextField txtBarcodeScan;
    private JComboBox<Product> cmbProducts;
    private JLabel lblCategoryVal;
    private JLabel lblUnitPriceVal;
    private JLabel lblAvailableStockVal;
    private JLabel lblStockAlertNote;
    private JSpinner spnQuantity;
    private JButton btnAddToCart;
    private JButton btnUpdateQuantity;
    private JButton btnRemoveSelected;
    private JButton btnUndoRemove;
    private JButton btnClearCart;

    // Shift Header Controls
    private JLabel lblShiftStatus;
    private JButton btnOpenShift;
    private JButton btnCloseShift;

    // Cart Table & Summary
    private JTable tblCart;
    private DefaultTableModel cartTableModel;
    private JLabel lblSubtotalVal;
    private JLabel lblTaxVal;
    private JLabel lblFinalTotalVal;

    // Cash Tendered & Change Due
    private JTextField txtCashReceived;
    private JLabel lblChangeDueVal;

    // Split Payment storage
    private List<PaymentLine> pendingSplitPayments = null;
    private String selectedCustomerId = null;
    private BigDecimal pendingDiscount = BigDecimal.ZERO;

    private JComboBox<PaymentMethod> cmbPaymentMethod;
    private JButton btnSplitPayment;
    private JButton btnHoldBill;
    private JButton btnResumeBills;
    private JButton btnKhata;
    private JButton btnCheckout;
    private JButton btnSalesHistory;
    private JButton btnRefresh;

    public BillingPanel(InventoryService inventoryService, SalesService salesService) {
        this(inventoryService, salesService, null, null, null, null, null);
    }

    public BillingPanel(InventoryService inventoryService, SalesService salesService,
                        HeldBillService heldBillService, CustomerCreditService customerCreditService,
                        CashierShiftService cashierShiftService, SettingsService settingsService,
                        AuthService authService) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService cannot be null");
        this.salesService = Objects.requireNonNull(salesService, "salesService cannot be null");
        this.heldBillService = heldBillService;
        this.customerCreditService = customerCreditService;
        this.cashierShiftService = cashierShiftService;
        this.settingsService = settingsService;
        this.authService = authService;

        initComponents();
        refreshData();
    }

    public void setCurrentCashier(String username) {
        if (username != null && !username.trim().isEmpty()) {
            this.currentCashier = username.trim();
            updateShiftStatusDisplay();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.BG_PAGE);

        // 1. Top Header with Shift Management
        JPanel pnlHeader = createHeaderPanel();
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Center Area (Left: Selection + Quick Sale | Right: Cart Table & Summary)
        JPanel pnlCenter = new JPanel(new BorderLayout(12, 12));
        pnlCenter.setOpaque(false);

        JPanel pnlLeft = createProductSelectionPanel();
        JPanel pnlRight = createCartAndCheckoutPanel();

        pnlCenter.add(shelflife.ui.theme.Styles.createScrollPane(pnlLeft), BorderLayout.WEST);
        pnlCenter.add(pnlRight, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);

        installKeyboardShortcuts();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(UITheme.CARD_BG);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Point-of-Sale & Retail Checkout");
        title.setFont(UITheme.FONT_PAGE_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel subtitle = new JLabel("FEFO Multi-Batch Allocation • Multi-Tender Split Payment • Live Shift Audit");
        subtitle.setFont(UITheme.FONT_SUBTITLE);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        titleBox.add(title);
        titleBox.add(subtitle);

        // Shift Controls
        JPanel shiftBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        shiftBox.setOpaque(false);

        lblShiftStatus = new JLabel("Shift: Checking...");
        lblShiftStatus.setFont(UITheme.FONT_BOLD);
        lblShiftStatus.setForeground(UITheme.TEXT_MAIN);
        shiftBox.add(lblShiftStatus);

        btnOpenShift = UITheme.createSecondaryButton("Open Shift");
        btnOpenShift.addActionListener(e -> handleOpenShift());
        shiftBox.add(btnOpenShift);

        btnCloseShift = UITheme.createSecondaryButton("Close Shift");
        btnCloseShift.addActionListener(e -> handleCloseShift());
        shiftBox.add(btnCloseShift);

        header.add(titleBox, BorderLayout.WEST);
        header.add(shiftBox, BorderLayout.EAST);
        return header;
    }

    private void updateShiftStatusDisplay() {
        if (lblShiftStatus == null) return;
        if (cashierShiftService == null) {
            lblShiftStatus.setText("Cashier: " + currentCashier);
            if (btnOpenShift != null) btnOpenShift.setVisible(false);
            if (btnCloseShift != null) btnCloseShift.setVisible(false);
            return;
        }

        CashierShift active = cashierShiftService.getActiveShiftForCashier(currentCashier);
        if (active != null) {
            lblShiftStatus.setText(String.format("Shift Active: %s (Float: %s)",
                    currentCashier, CurrencyUtil.format(active.getOpeningFloat())));
            lblShiftStatus.setForeground(UITheme.SUCCESS);
            if (btnOpenShift != null) btnOpenShift.setEnabled(false);
            if (btnCloseShift != null) btnCloseShift.setEnabled(true);
        } else {
            lblShiftStatus.setText(String.format("No Active Shift for %s", currentCashier));
            lblShiftStatus.setForeground(UITheme.DANGER);
            if (btnOpenShift != null) btnOpenShift.setEnabled(true);
            if (btnCloseShift != null) btnCloseShift.setEnabled(false);
        }
    }

    private void handleOpenShift() {
        if (cashierShiftService == null) return;
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        ShiftOpenDialog dlg = new ShiftOpenDialog(parent, cashierShiftService, currentCashier);
        dlg.setVisible(true);
        updateShiftStatusDisplay();
    }

    private void handleCloseShift() {
        if (cashierShiftService == null) return;
        CashierShift active = cashierShiftService.getActiveShiftForCashier(currentCashier);
        if (active == null) {
            JOptionPane.showMessageDialog(this, "No active shift to close.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        ShiftCloseDialog dlg = new ShiftCloseDialog(parent, cashierShiftService, active);
        dlg.setVisible(true);
        updateShiftStatusDisplay();
    }

    private JPanel createProductSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.CARD_BG);
        panel.setPreferredSize(new Dimension(420, 640));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                "Product Selection & Staging",
                TitledBorder.LEFT, TitledBorder.TOP, UITheme.FONT_SECTION, UITheme.TEXT_MAIN));

        // Quick-Sale Panel (Top 12 Sellers)
        pnlQuickSale = new JPanel(new GridLayout(3, 4, 6, 6));
        pnlQuickSale.setOpaque(false);
        pnlQuickSale.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                "Quick Add (Top Sellers)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11), UITheme.TEXT_MUTED));
        pnlQuickSale.setMaximumSize(new Dimension(400, 110));
        pnlQuickSale.setPreferredSize(new Dimension(400, 110));

        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Barcode scan
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.38;
        JLabel lblBarcode = new JLabel("Barcode / SKU:");
        lblBarcode.setFont(UITheme.FONT_BOLD);
        formGrid.add(lblBarcode, gbc);

        txtBarcodeScan = new JTextField();
        UITheme.styleTextField(txtBarcodeScan);
        txtBarcodeScan.setToolTipText("Scan barcode or enter SKU/ID and hit Enter");
        txtBarcodeScan.addActionListener(e -> handleBarcodeScan());
        gbc.gridx = 1; gbc.weightx = 0.62;
        formGrid.add(txtBarcodeScan, gbc);

        // Row 1: Product dropdown
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.38;
        JLabel lblProduct = new JLabel("Product Name:");
        lblProduct.setFont(UITheme.FONT_BOLD);
        formGrid.add(lblProduct, gbc);

        cmbProducts = new JComboBox<>();
        cmbProducts.setFont(UITheme.FONT_REGULAR);
        cmbProducts.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product) {
                    Product p = (Product) value;
                    setText(p.getName() + " (" + p.getProductId() + ")");
                }
                return this;
            }
        });
        cmbProducts.addActionListener(e -> updateProductDetailLabels());
        gbc.gridx = 1; gbc.weightx = 0.62;
        formGrid.add(cmbProducts, gbc);

        // Row 2: Category
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.38;
        formGrid.add(new JLabel("Category:"), gbc);
        lblCategoryVal = new JLabel("—");
        lblCategoryVal.setFont(UITheme.FONT_BOLD);
        gbc.gridx = 1; gbc.weightx = 0.62;
        formGrid.add(lblCategoryVal, gbc);

        // Row 3: Unit Price
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.38;
        formGrid.add(new JLabel("Unit Price:"), gbc);
        lblUnitPriceVal = new JLabel("Rs. 0.00");
        lblUnitPriceVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUnitPriceVal.setForeground(UITheme.PRIMARY);
        gbc.gridx = 1; gbc.weightx = 0.62;
        formGrid.add(lblUnitPriceVal, gbc);

        // Row 4: Available Stock
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.38;
        formGrid.add(new JLabel("Sellable Stock:"), gbc);
        lblAvailableStockVal = new JLabel("0 units");
        lblAvailableStockVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblAvailableStockVal.setForeground(UITheme.SUCCESS);
        gbc.gridx = 1; gbc.weightx = 0.62;
        formGrid.add(lblAvailableStockVal, gbc);

        // Row 5: Stock Alert Note
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        lblStockAlertNote = new JLabel("");
        lblStockAlertNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStockAlertNote.setForeground(UITheme.TEXT_MUTED);
        formGrid.add(lblStockAlertNote, gbc);
        gbc.gridwidth = 1;

        // Row 6: Quantity Spinner
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.38;
        JLabel lblQty = new JLabel("Quantity:");
        lblQty.setFont(UITheme.FONT_BOLD);
        formGrid.add(lblQty, gbc);

        spnQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        spnQuantity.setFont(new Font("Segoe UI", Font.BOLD, 13));
        gbc.gridx = 1; gbc.weightx = 0.62;
        formGrid.add(spnQuantity, gbc);

        // Cart Action Buttons
        JPanel pnlCartActions = new JPanel(new GridLayout(3, 2, 8, 8));
        pnlCartActions.setOpaque(false);
        pnlCartActions.setBorder(new EmptyBorder(10, 8, 10, 8));

        btnAddToCart = UITheme.createPrimaryButton("+ Add to Cart");
        btnAddToCart.addActionListener(e -> handleAddToCart());
        pnlCartActions.add(btnAddToCart);

        btnUpdateQuantity = UITheme.createSecondaryButton("Update Qty");
        btnUpdateQuantity.addActionListener(e -> handleUpdateQuantity());
        pnlCartActions.add(btnUpdateQuantity);

        btnRemoveSelected = UITheme.createSecondaryButton("Remove Item");
        btnRemoveSelected.addActionListener(e -> handleRemoveSelected());
        pnlCartActions.add(btnRemoveSelected);

        btnUndoRemove = UITheme.createSecondaryButton("Undo Remove");
        btnUndoRemove.setEnabled(false);
        btnUndoRemove.addActionListener(e -> handleUndoRemove());
        pnlCartActions.add(btnUndoRemove);

        btnClearCart = UITheme.createSecondaryButton("Clear Cart");
        btnClearCart.addActionListener(e -> handleClearCart());
        pnlCartActions.add(btnClearCart);

        panel.add(pnlQuickSale);
        panel.add(Box.createVerticalStrut(8));
        panel.add(formGrid);
        panel.add(Box.createVerticalStrut(8));
        panel.add(pnlCartActions);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createCartAndCheckoutPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                "Active POS Cart (In-Memory Staging)",
                TitledBorder.LEFT, TitledBorder.TOP, UITheme.FONT_SECTION, UITheme.TEXT_MAIN));

        // Cart Table
        String[] columns = {"Product ID", "Product Name", "Quantity", "Unit Price", "Line Total"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblCart = new JTable(cartTableModel);
        UITheme.styleTable(tblCart);
        tblCart.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblCart.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblCart.getColumnModel().getColumn(2).setPreferredWidth(60);
        tblCart.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblCart.getColumnModel().getColumn(4).setPreferredWidth(100);

        tblCart.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) syncSelectionWithForm();
        });

        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        tblCart.getColumnModel().getColumn(3).setCellRenderer(rightAlign);
        tblCart.getColumnModel().getColumn(4).setCellRenderer(rightAlign);

        JScrollPane scrollCart = new JScrollPane(tblCart);
        scrollCart.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        panel.add(scrollCart, BorderLayout.CENTER);

        // Bottom Summary & Actions
        JPanel pnlBottom = new JPanel();
        pnlBottom.setLayout(new BoxLayout(pnlBottom, BoxLayout.Y_AXIS));
        pnlBottom.setOpaque(false);
        pnlBottom.setBorder(new EmptyBorder(8, 10, 8, 10));

        // Financial Summary Matrix (Row 1: Subtotal, Row 2: Tax / GST, Row 3: Grand Total)
        JPanel pnlTotals = new JPanel(new GridLayout(3, 2, 12, 8));
        pnlTotals.setOpaque(false);

        pnlTotals.add(new JLabel("Subtotal:"));
        lblSubtotalVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblSubtotalVal.setFont(UITheme.FONT_BOLD);
        pnlTotals.add(lblSubtotalVal);

        pnlTotals.add(new JLabel("Tax / GST:"));
        lblTaxVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblTaxVal.setFont(UITheme.FONT_BOLD);
        lblTaxVal.setForeground(UITheme.TEXT_MUTED);
        pnlTotals.add(lblTaxVal);

        pnlTotals.add(new JLabel("GRAND TOTAL:"));
        lblFinalTotalVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblFinalTotalVal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblFinalTotalVal.setForeground(UITheme.SUCCESS);
        pnlTotals.add(lblFinalTotalVal);

        // Cash Tendered & Change Due row
        JPanel pnlCashRow = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlCashRow.setOpaque(false);

        JLabel lblCashTendered = new JLabel("Cash Tendered:");
        lblCashTendered.setFont(UITheme.FONT_BOLD);
        pnlCashRow.add(lblCashTendered);

        txtCashReceived = new JTextField();
        txtCashReceived.setToolTipText("Enter cash received [F4]");
        txtCashReceived.setHorizontalAlignment(JTextField.RIGHT);
        txtCashReceived.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtCashReceived.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateChangeDue(); }
            @Override public void removeUpdate(DocumentEvent e) { updateChangeDue(); }
            @Override public void changedUpdate(DocumentEvent e) { updateChangeDue(); }
        });
        pnlCashRow.add(txtCashReceived);

        JLabel lblChangeLabel = new JLabel("Change Due:");
        lblChangeLabel.setFont(UITheme.FONT_BOLD);
        pnlCashRow.add(lblChangeLabel);

        lblChangeDueVal = new JLabel("Rs. 0.00", SwingConstants.RIGHT);
        lblChangeDueVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblChangeDueVal.setForeground(new Color(0x059669));
        pnlCashRow.add(lblChangeDueVal);

        // Actions Toolbar (BoxLayout Y_AXIS: Top row secondary, Bottom row primary)
        JPanel pnlActions = new JPanel();
        pnlActions.setLayout(new BoxLayout(pnlActions, BoxLayout.Y_AXIS));
        pnlActions.setOpaque(false);

        JPanel pnlSecondaryRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pnlSecondaryRow.setOpaque(false);

        btnHoldBill = UITheme.createSecondaryButton("Hold (F6)");
        btnHoldBill.addActionListener(e -> handleHoldBill());
        pnlSecondaryRow.add(btnHoldBill);

        btnResumeBills = UITheme.createSecondaryButton("Resume (F7)");
        btnResumeBills.addActionListener(e -> handleResumeBills());
        pnlSecondaryRow.add(btnResumeBills);

        btnSplitPayment = UITheme.createSecondaryButton("Split Payment");
        btnSplitPayment.addActionListener(e -> handleSplitPayment());
        pnlSecondaryRow.add(btnSplitPayment);

        btnKhata = UITheme.createSecondaryButton("Khata (F3)");
        btnKhata.addActionListener(e -> handleOpenKhata());
        pnlSecondaryRow.add(btnKhata);

        btnSalesHistory = UITheme.createSecondaryButton("Sales History");
        btnSalesHistory.addActionListener(e -> openSalesHistory());
        pnlSecondaryRow.add(btnSalesHistory);

        btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshData());
        pnlSecondaryRow.add(btnRefresh);

        JPanel pnlPrimaryRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        pnlPrimaryRow.setOpaque(false);

        JLabel lblPayLabel = new JLabel("Payment:");
        lblPayLabel.setFont(UITheme.FONT_BOLD);
        pnlPrimaryRow.add(lblPayLabel);

        cmbPaymentMethod = new JComboBox<>(PaymentMethod.values());
        cmbPaymentMethod.setFont(UITheme.FONT_REGULAR);
        pnlPrimaryRow.add(cmbPaymentMethod);

        btnCheckout = UITheme.createPrimaryButton("Checkout & Commit (F8)");
        btnCheckout.setPreferredSize(new Dimension(210, 40));
        btnCheckout.addActionListener(e -> handleCheckout());
        pnlPrimaryRow.add(btnCheckout);

        pnlActions.add(pnlSecondaryRow);
        pnlActions.add(pnlPrimaryRow);

        pnlBottom.add(pnlTotals);
        pnlBottom.add(Box.createVerticalStrut(8));
        pnlBottom.add(pnlCashRow);
        pnlBottom.add(Box.createVerticalStrut(8));
        pnlBottom.add(pnlActions);

        panel.add(pnlBottom, BorderLayout.SOUTH);
        return panel;
    }

    private BigDecimal calculateGrandTotal() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal taxRate = BigDecimal.ZERO;
        if (settingsService != null) {
            try { taxRate = settingsService.getSettings().getTaxRate(); } catch (Exception ignored) {}
        }
        BigDecimal tax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
    }

    private void updateChangeDue() {
        if (txtCashReceived == null || lblChangeDueVal == null) return;
        try {
            String text = txtCashReceived.getText().trim();
            BigDecimal received = !text.isEmpty() ? new BigDecimal(text) : BigDecimal.ZERO;
            BigDecimal grandTotal = calculateGrandTotal();
            BigDecimal change = received.subtract(grandTotal);

            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                lblChangeDueVal.setText(CurrencyUtil.format(change));
                lblChangeDueVal.setForeground(new Color(0x059669)); // Green
            } else {
                lblChangeDueVal.setText("-" + CurrencyUtil.format(change.abs()));
                lblChangeDueVal.setForeground(Color.RED);
            }
        } catch (Exception ignored) {
            lblChangeDueVal.setText("Rs. 0.00");
        }
    }

    private void refreshQuickSaleButtons() {
        if (pnlQuickSale == null) return;
        pnlQuickSale.removeAll();

        List<String> topIds = salesService.getTopSellingProductIds(12, 30);
        int added = 0;
        if (topIds != null) {
            for (String pid : topIds) {
                if (added >= 12) break;
                Product p = inventoryService.getProductById(pid);
                if (p != null && p.isActive()) {
                    JButton btn = new JButton(p.getName());
                    btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    btn.setMargin(new Insets(2, 2, 2, 2));
                    btn.setToolTipText("Add 1 unit of " + p.getName() + " (" + CurrencyUtil.format(p.getSellingPrice()) + ")");
                    btn.addActionListener(e -> quickAddProductToCart(p));
                    pnlQuickSale.add(btn);
                    added++;
                }
            }
        }

        // Fill remaining slots up to 12 from general active products if store is new
        if (added < 12) {
            for (Product p : inventoryService.getAllProducts()) {
                if (added >= 12) break;
                if (p.isActive()) {
                    JButton btn = new JButton(p.getName());
                    btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    btn.setMargin(new Insets(2, 2, 2, 2));
                    btn.addActionListener(e -> quickAddProductToCart(p));
                    pnlQuickSale.add(btn);
                    added++;
                }
            }
        }

        pnlQuickSale.revalidate();
        pnlQuickSale.repaint();
    }

    private void quickAddProductToCart(Product p) {
        int avail = getAvailableSellableStock(p.getProductId());
        if (avail <= 0) {
            ToastNotification.show(this, "No sellable non-expired stock for: " + p.getName(), ToastNotification.Type.WARNING);
            return;
        }

        CartItem existing = findCartItemByProductId(p.getProductId());
        if (existing != null) {
            if (existing.getQuantity() + 1 > avail) {
                ToastNotification.show(this, "Cannot exceed available stock (" + avail + ") for: " + p.getName(), ToastNotification.Type.WARNING);
                return;
            }
            existing.addQuantity(1);
        } else {
            cartItems.add(new CartItem(p.getProductId(), p.getName(), 1, p.getSellingPrice()));
        }

        renderCartTable();
        updateSummaryTotals();
        ToastNotification.show(this, "Added " + p.getName() + " to cart", ToastNotification.Type.INFO);
    }

    private void handleRemoveSelected() {
        int selectedRow = tblCart.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= cartItems.size()) {
            JOptionPane.showMessageDialog(this, "Please select an item from the cart table to remove.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CartItem removed = cartItems.remove(selectedRow);
        undoStack.push(removed);
        if (btnUndoRemove != null) btnUndoRemove.setEnabled(true);

        renderCartTable();
        updateSummaryTotals();
    }

    private void handleUndoRemove() {
        if (undoStack.isEmpty()) return;
        CartItem restored = undoStack.pop();

        CartItem existing = findCartItemByProductId(restored.getProductId());
        if (existing != null) {
            existing.addQuantity(restored.getQuantity());
        } else {
            cartItems.add(restored);
        }

        if (undoStack.isEmpty() && btnUndoRemove != null) {
            btnUndoRemove.setEnabled(false);
        }

        renderCartTable();
        updateSummaryTotals();
        ToastNotification.show(this, "Restored " + restored.getProductName() + " to cart", ToastNotification.Type.SUCCESS);
    }

    private void handleHoldBill() {
        if (heldBillService == null) {
            JOptionPane.showMessageDialog(this, "HeldBillService is not configured.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot hold an empty cart.", "Cart Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtCust = new JTextField();
        JTextField txtNote = new JTextField();
        Object[] fields = {
                "Customer Name (Optional):", txtCust,
                "Hold Note (e.g., Customer fetching cash):", txtNote
        };

        int res = JOptionPane.showConfirmDialog(this, fields, "Hold Active POS Cart Session", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                HeldBill bill = heldBillService.holdBill(currentCashier, txtCust.getText().trim(), txtNote.getText().trim(), cartItems);
                cartItems.clear();
                renderCartTable();
                updateSummaryTotals();
                ToastNotification.show(this, "Bill suspended successfully (Hold ID: " + bill.getHeldBillId() + ")", ToastNotification.Type.SUCCESS);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to hold bill: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleResumeBills() {
        if (heldBillService == null) {
            JOptionPane.showMessageDialog(this, "HeldBillService is not configured.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        ResumeBillDialog dlg = new ResumeBillDialog(parent, heldBillService);
        dlg.setVisible(true);

        List<HeldBillItem> items = dlg.getResumedItems();
        if (items != null && !items.isEmpty()) {
            for (HeldBillItem it : items) {
                CartItem existing = findCartItemByProductId(it.getProductId());
                if (existing != null) {
                    existing.addQuantity(it.getQuantity());
                } else {
                    cartItems.add(new CartItem(it.getProductId(), it.getProductName(), it.getQuantity(), it.getUnitPrice()));
                }
            }
            renderCartTable();
            updateSummaryTotals();
            ToastNotification.show(this, "Restored " + items.size() + " line items into cart.", ToastNotification.Type.SUCCESS);
        }
    }

    private void handleSplitPayment() {
        BigDecimal total = calculateSubtotal();
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Add items to cart before allocating payment.", "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        SplitPaymentDialog dlg = new SplitPaymentDialog(parent, total, customerCreditService);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            this.pendingSplitPayments = dlg.getAllocatedPayments();
            this.selectedCustomerId = dlg.getSelectedCustomerId();
            if (txtCashReceived != null && dlg.getConfirmedCashTendered().compareTo(BigDecimal.ZERO) > 0) {
                txtCashReceived.setText(dlg.getConfirmedCashTendered().toPlainString());
            }
            ToastNotification.show(this, "Split payment tender allocations saved.", ToastNotification.Type.SUCCESS);
        }
    }

    private void handleOpenKhata() {
        if (customerCreditService == null) {
            JOptionPane.showMessageDialog(this, "Customer Credit (Khata) service is not configured.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        CustomerCreditDialog dlg = new CustomerCreditDialog(parent, customerCreditService, currentCashier);
        dlg.setVisible(true);
    }

    @Override
    public void refreshData() {
        Product currentlySelected = (Product) cmbProducts.getSelectedItem();
        String selectedId = currentlySelected != null ? currentlySelected.getProductId() : null;

        cmbProducts.removeAllItems();
        List<Product> products = inventoryService.getAllProducts();
        if (products != null) {
            for (Product p : products) {
                if (p.isActive()) {
                    cmbProducts.addItem(p);
                }
            }
        }

        if (selectedId != null) {
            for (int i = 0; i < cmbProducts.getItemCount(); i++) {
                if (cmbProducts.getItemAt(i).getProductId().equalsIgnoreCase(selectedId)) {
                    cmbProducts.setSelectedIndex(i);
                    break;
                }
            }
        }

        updateProductDetailLabels();
        updateShiftStatusDisplay();
        refreshQuickSaleButtons();
    }

    private void handleBarcodeScan() {
        String query = txtBarcodeScan.getText().trim();
        if (query.isEmpty()) return;

        Product matched = null;
        for (Product p : inventoryService.getAllProducts()) {
            if (p.isActive()) {
                if (query.equalsIgnoreCase(p.getBarcode()) || query.equalsIgnoreCase(p.getSku()) || query.equalsIgnoreCase(p.getProductId())) {
                    matched = p;
                    break;
                }
            }
        }

        if (matched != null) {
            quickAddProductToCart(matched);
            txtBarcodeScan.setText("");
        } else {
            ToastNotification.show(this, "No active product matched barcode/SKU: " + query, ToastNotification.Type.WARNING);
        }
    }

    private int getAvailableSellableStock(String productId) {
        int total = 0;
        LocalDate today = LocalDate.now();
        for (ProductBatch b : inventoryService.getBatchesByProductId(productId)) {
            if (!b.isExpired(today) && b.getQuantity() > 0 && !b.isRecalled()) {
                total += b.getQuantity();
            }
        }
        return total;
    }

    private void updateProductDetailLabels() {
        Product selected = (Product) cmbProducts.getSelectedItem();
        if (selected == null) {
            lblCategoryVal.setText("—");
            lblUnitPriceVal.setText("Rs. 0.00");
            lblAvailableStockVal.setText("0 units");
            lblStockAlertNote.setText("");
            return;
        }

        lblCategoryVal.setText(selected.getCategory());
        lblUnitPriceVal.setText(CurrencyUtil.format(selected.getSellingPrice()));

        int sellableStock = getAvailableSellableStock(selected.getProductId());
        lblAvailableStockVal.setText(sellableStock + " units");

        if (sellableStock <= 0) {
            lblAvailableStockVal.setForeground(UITheme.DANGER);
            lblStockAlertNote.setText("Out of stock or all batches expired/recalled");
            lblStockAlertNote.setForeground(UITheme.DANGER);
        } else if (sellableStock <= selected.getLowStockThreshold()) {
            lblAvailableStockVal.setForeground(UITheme.WARNING);
            lblStockAlertNote.setText("Low stock warning (threshold: " + selected.getLowStockThreshold() + ")");
            lblStockAlertNote.setForeground(UITheme.WARNING);
        } else {
            lblAvailableStockVal.setForeground(UITheme.SUCCESS);
            lblStockAlertNote.setText("Sufficient stock available");
            lblStockAlertNote.setForeground(UITheme.SUCCESS);
        }
    }

    private void handleAddToCart() {
        Product selected = (Product) cmbProducts.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a product from the list.", "No Product Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestedQty = (Integer) spnQuantity.getValue();
        if (requestedQty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int availableStock = getAvailableSellableStock(selected.getProductId());
        CartItem existingItem = findCartItemByProductId(selected.getProductId());
        int currentInCart = existingItem != null ? existingItem.getQuantity() : 0;
        int totalProjected = currentInCart + requestedQty;

        if (totalProjected > availableStock) {
            JOptionPane.showMessageDialog(this,
                    String.format("Requested quantity (%d) plus cart items (%d) exceeds available sellable stock (%d) for '%s'.",
                            requestedQty, currentInCart, availableStock, selected.getName()),
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

    private void handleClearCart() {
        if (cartItems.isEmpty()) return;

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all items from the cart?\nZero inventory changes will occur.",
                "Confirm Clear Cart", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            cartItems.clear();
            pendingSplitPayments = null;
            selectedCustomerId = null;
            renderCartTable();
            updateSummaryTotals();
        }
    }

    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot checkout: The cart is empty.", "Cart Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Below-cost audit warning
        for (CartItem item : cartItems) {
            if (salesService.isBelowCost(item.getProductId(), item.getUnitPrice(), BigDecimal.ZERO)) {
                int warnRes = JOptionPane.showConfirmDialog(this,
                        "Warning: Selling price for '" + item.getProductName() + "' is BELOW batch cost!\n" +
                        "Do you wish to proceed with this sale at a trading loss?",
                        "Below-Cost Loss Warning",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (warnRes != JOptionPane.YES_OPTION) {
                    return;
                }
                break;
            }
        }

        PaymentMethod selectedMethod = (PaymentMethod) cmbPaymentMethod.getSelectedItem();
        if (selectedMethod == null) selectedMethod = PaymentMethod.CASH;

        BigDecimal finalTotal = calculateGrandTotal();

        String confirmMsg = String.format(
                "Confirm Checkout & Commit Sale?\n\n" +
                "Total Items in Cart : %d\n" +
                "Payment Method      : %s\n" +
                "Grand Total         : Rs. %.2f\n\n" +
                "This will allocate eligible non-expired batches using FEFO and update stock.",
                cartItems.size(), selectedMethod.getDisplayName(), finalTotal.doubleValue());

        int response = JOptionPane.showConfirmDialog(this, confirmMsg, "Confirm Sale Commitment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response != JOptionPane.YES_OPTION) return;

        Map<String, Integer> productQuantities = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            productQuantities.put(item.getProductId(), item.getQuantity());
        }

        btnCheckout.setEnabled(false);

        try {
            Sale committedSale;
            if (pendingSplitPayments != null && !pendingSplitPayments.isEmpty()) {
                committedSale = salesService.commitSaleWithPayments(
                        productQuantities, pendingSplitPayments, selectedCustomerId,
                        pendingDiscount, "Split POS checkout", currentCashier
                );
            } else {
                committedSale = salesService.commitSale(productQuantities, selectedMethod);
            }

            // Record cash sales on active cashier shift
            if (cashierShiftService != null && committedSale != null) {
                try {
                    CashierShift active = cashierShiftService.getActiveShiftForCashier(currentCashier);
                    if (active != null) {
                        cashierShiftService.recordCashSale(active.getShiftId(), committedSale.getTotalAmount());
                    }
                } catch (Exception shiftEx) {
                    System.err.println("Warning: Failed to record shift cash sale: " + shiftEx.getMessage());
                }
            }

            cartItems.clear();
            pendingSplitPayments = null;
            selectedCustomerId = null;
            renderCartTable();
            updateSummaryTotals();
            refreshData();

            BigDecimal cashRcvd = BigDecimal.ZERO;
            try {
                String txt = txtCashReceived != null ? txtCashReceived.getText().trim() : "";
                if (!txt.isEmpty()) cashRcvd = new BigDecimal(txt);
            } catch (Exception ignored) {}

            BigDecimal change = cashRcvd.subtract(committedSale.getTotalAmount());
            if (change.compareTo(BigDecimal.ZERO) < 0) change = BigDecimal.ZERO;

            AppSettings settings = new AppSettings();
            if (settingsService != null) {
                try { settings = settingsService.getSettings(); } catch (Exception ignored) {}
            }

            ReceiptDialog dialog = new ReceiptDialog(
                    SwingUtilities.getWindowAncestor(this),
                    committedSale,
                    committedSale.getItems(),
                    settings,
                    cashRcvd,
                    change,
                    currentCashier,
                    "Walk-in Customer"
            );
            dialog.setVisible(true);

        } catch (SaleException ex) {
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
                    CurrencyUtil.format(item.getUnitPrice()),
                    CurrencyUtil.format(item.getLineTotal())
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
        BigDecimal taxRate = BigDecimal.ZERO;
        if (settingsService != null) {
            try { taxRate = settingsService.getSettings().getTaxRate(); } catch (Exception ignored) {}
        }
        BigDecimal tax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        lblSubtotalVal.setText(CurrencyUtil.format(subtotal));
        if (lblTaxVal != null) lblTaxVal.setText(CurrencyUtil.format(tax));
        lblFinalTotalVal.setText(CurrencyUtil.format(grandTotal));
        updateChangeDue();
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

    private CartItem findCartItemByProductId(String productId) {
        for (CartItem item : cartItems) {
            if (item.getProductId().equalsIgnoreCase(productId)) {
                return item;
            }
        }
        return null;
    }

    private void openSalesHistory() {
        SalesHistoryDialog dialog = new SalesHistoryDialog(SwingUtilities.getWindowAncestor(this), salesService);
        dialog.setVisible(true);
    }

    private void installKeyboardShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        // F2: Focus Barcode / Search
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusBarcode");
        am.put("focusBarcode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (txtBarcodeScan != null) {
                    txtBarcodeScan.requestFocusInWindow();
                    txtBarcodeScan.selectAll();
                }
            }
        });

        // F3: Customer Khata
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "openKhata");
        am.put("openKhata", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOpenKhata();
            }
        });

        // F4: Focus Cash Received
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "focusCash");
        am.put("focusCash", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (txtCashReceived != null) {
                    txtCashReceived.requestFocusInWindow();
                    txtCashReceived.selectAll();
                }
            }
        });

        // F6: Hold Bill
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "holdBill");
        am.put("holdBill", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleHoldBill();
            }
        });

        // F7: Resume Bills
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "resumeBills");
        am.put("resumeBills", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleResumeBills();
            }
        });

        // F8: Checkout
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "checkoutSale");
        am.put("checkoutSale", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCheckout();
            }
        });

        // Delete: Remove item
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeCartItem");
        am.put("removeCartItem", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (!(focusOwner instanceof javax.swing.text.JTextComponent)) {
                    handleRemoveSelected();
                }
            }
        });
    }
}
