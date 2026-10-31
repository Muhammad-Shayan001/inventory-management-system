package shelflife.ui.billing;

import shelflife.model.PaymentMethod;
import shelflife.model.Product;
import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.service.InventoryService;
import shelflife.service.SalesService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.Vector;

/**
 * Panel for billing and sales management.
 * Allows the user to select products and quantities, review the cart,
 and commit the sale. Uses FEFO allocation for batch selection.
 * Sale and stock changes are persisted through the approved transaction mechanism.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class BillingPanel extends javax.swing.JPanel {
    private final InventoryService inventoryService;
    private final SalesService salesService;
    
    private JTable productsTable;
    private JTable cartTable;
    private DefaultTableModel productsTableModel;
    private DefaultTableModel cartTableModel;
    
    private JTextField quantityField;
    private JTextField productIdField;
    private JLabel subtotalLabel;
    private JLabel totalLabel;
    private JComboBox<PaymentMethod> paymentMethodCombo;
    
    private Map<String, Integer> cartQuantities = new HashMap<>();
    private List<SaleItem> cartItems = new ArrayList<>();

    public BillingPanel(InventoryService inventoryService, SalesService salesService) {
        this.inventoryService = inventoryService;
        this.salesService = salesService;
        initComponents();
        loadProducts();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Left panel: product selection
        JPanel selectionPanel = new JPanel(new BorderLayout(5, 5));
        
        // Product table
        String[] prodCols = {"Product ID", "Name", "Category", "Sellable Stock", "Price"};
        productsTableModel = new DefaultTableModel(new Object[][]{}, prodCols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(productsTableModel);
        JScrollPane prodScroll = new JScrollPane(productsTable);
        selectionPanel.add(new JLabel("Product Catalog"), BorderLayout.NORTH);
        selectionPanel.add(prodScroll, BorderLayout.CENTER);
        
        // Right panel: cart
        JPanel cartPanel = new JPanel(new BorderLayout(5, 5));
        
        String[] cartCols = {"Sale Item", "Product", "Batch", "Qty", "Unit Price", "Line Total"};
        cartTableModel = new DefaultTableModel(new Object[][]{}, cartCols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartTableModel);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartPanel.add(new JLabel("Shopping Cart"), BorderLayout.NORTH);
        cartPanel.add(cartScroll, BorderLayout.CENTER);
        
        // Quantity and add button
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField("1");
        addPanel.add(quantityField);
        
        JButton addToCartBtn = new JButton("Add to Cart");
        addToCartBtn.addActionListener(e -> addToCart());
        addPanel.add(addToCartBtn);
        
        selectionPanel.add(addPanel, BorderLayout.SOUTH);
        
        // Bottom: totals and payment
        JPanel bottomPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        subtotalLabel = new JLabel("Subtotal: Rs. 0.00");
        bottomPanel.add(subtotalLabel);
        totalLabel = new JLabel("Total: Rs. 0.00");
        bottomPanel.add(totalLabel);
        
        bottomPanel.add(new JLabel("Payment Method:"));
        paymentMethodCombo = new JComboBox<>(PaymentMethod.values());
        bottomPanel.add(paymentMethodCombo);
        
        JButton commitBtn = new JButton("Commit Sale");
        commitBtn.addActionListener(e -> commitSale());
        bottomPanel.add(commitBtn);
        
        JButton cancelBtn = new JButton("Clear Cart");
        cancelBtn.addActionListener(e -> clearCart());
        bottomPanel.add(cancelBtn);
        
        // Add all panels
        add(selectionPanel, BorderLayout.CENTER);
        add(cartPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        
        loadProducts();
    }
    
    private void loadProducts() {
        productsTableModel.setRowCount(0);
        List<Product> products = inventoryService.getAllProducts();
        for (Product p : products) {
            int stock = inventoryService.getSellableStock(p.getProductId(), java.time.LocalDate.now());
            productsTableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getName(),
                    p.getCategory(),
                    stock,
                    p.getSellingPrice().toPlainString()
            });
        }
    }
    
    private void addToCart() {
        try {
            String productId = productIdField.getText().trim();
            if (productId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a product first.");
                return;
            }
            int requestedQty = Integer.parseInt(quantityField.getText().trim());
            if (requestedQty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive.");
                return;
            }
            
            // Check sellable stock
            int available = inventoryService.getSellableStock(productId, java.time.LocalDate.now());
            if (requestedQty > available) {
                JOptionPane.showMessageDialog(this, 
                        "Insufficient sellable stock. Available: " + available + ", Requested: " + requestedQty);
                return;
            }
            
            // Add to cart
            Product product = null;
            for (int i = 0; i < productsTableModel.getRowCount(); i++) {
                if (productsTableModel.getValueAt(i, 0).toString().equals(productId)) {
                    product = new Product(
                            productId,
                            productsTableModel.getValueAt(i, 1).toString(),
                            productsTableModel.getValueAt(i, 2).toString(),
                            new BigDecimal(productsTableModel.getValueAt(i, 4).toString()),
                            0
                    );
                    break;
                }
            }
            
            if (product != null) {
                cartQuantities.put(productId, cartQuantities.getOrDefault(productId, 0) + requestedQty);
                
                // For FEFO allocation, we need batches - the service will handle this at commit time
                // For now, add a cart entry with the product info
                cartItems.add(new SaleItem(
                        "SI-temp-" + System.currentTimeMillis(),
                        "", // saleId will be set at commit
                        product.getProductId(),
                        product.getName(),
                        "", // batchId - will be determined at commit
                        requestedQty,
                        product.getSellingPrice()
                ));
                
                refreshCartDisplay();
                JOptionPane.showMessageDialog(this, "Added " + requestedQty + " x " + product.getName() + " to cart.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshCartDisplay() {
        cartTableModel.setRowCount(0);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleItem item : cartItems) {
            cartTableModel.addRow(new Object[]{
                    item.getSaleItemId(),
                    item.getProductName(),
                    item.getBatchId() != null ? item.getBatchId() : "FEFO allocation at checkout",
                    item.getQuantity(),
                    item.getUnitPrice().toPlainString(),
                    item.getLineTotal().toPlainString()
            });
            subtotal = subtotal.add(item.getLineTotal());
        }
        subtotalLabel.setText("Subtotal: Rs. " + subtotal.toPlainString());
        totalLabel.setText("Total: Rs. " + subtotal.toPlainString()); // No tax in this system
    }
    
    private void clearCart() {
        cartQuantities.clear();
        cartItems.clear();
        refreshCartDisplay();
    }
    
    private void commitSale() {
        try {
            if (cartItems.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty. Add items before committing.");
                return;
            }
            
            // Build productQuantities map from cart
            Map<String, Integer> productQuantities = new HashMap<>();
            for (SaleItem item : cartItems) {
                String pid = item.getProductId();
                int totalQty = cartQuantities.getOrDefault(pid, 0) + item.getQuantity();
                // Actually, cartQuantities tracks the added quantity, and cartItems has the item qty
                // Let me simplify: use the cartItems quantities
                productQuantities.put(item.getProductId(), item.getQuantity());
            }
            
            PaymentMethod paymentMethod = paymentMethodCombo.getSelectedItem();
            Sale sale = salesService.commitSale(productQuantities, paymentMethod);
            
            JOptionPane.showMessageDialog(this, 
                    "Sale committed successfully!\n" +
                    "Sale ID: " + sale.getSaleId() + "\n" +
                    "Total: Rs. " + sale.getFinalTotal().toPlainString());
            
            clearCart();
            refreshData();
        } catch (shelflife.service.SaleException ex) {
            JOptionPane.showMessageDialog(this, "Sale failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void refreshData() {
        loadProducts();
        refreshCartDisplay();
    }
}