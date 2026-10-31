package shelflife.ui.inventory;

import shelflife.model.Product;
import shelflife.service.InventoryService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * Panel for managing the product inventory catalog.
 * Allows adding, updating, and removing products.
 * Displays product table with stock status.
 * 
 * @author ShelfLife Project
 * @version 1.0 (Shared Architecture Contract)
 */
public class ProductInventoryPanel extends javax.swing.JPanel {
    private final InventoryService inventoryService;
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField productIdField;
    private JTextField nameField;
    private JTextField categoryField;
    private JTextField priceField;
    private JTextField thresholdField;

    public ProductInventoryPanel(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        initComponents();
        refreshData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Controls panel
        JPanel controls = new JPanel(new GridLayout(5, 2, 5, 5));
        controls.add(new JLabel("Product ID:"));
        productIdField = new JTextField();
        controls.add(productIdField);
        controls.add(new JLabel("Name:"));
        nameField = new JTextField();
        controls.add(nameField);
        controls.add(new JLabel("Category:"));
        categoryField = new JTextField();
        controls.add(categoryField);
        controls.add(new JLabel("Selling Price:"));
        priceField = new JTextField();
        controls.add(priceField);
        controls.add(new JLabel("Low Stock Threshold:"));
        thresholdField = new JTextField();
        controls.add(thresholdField);

        JButton addBtn = new JButton("Add Product");
        addBtn.addActionListener(e -> addProduct());
        controls.add(addBtn);

        JButton updateBtn = new JButton("Update Product");
        updateBtn.addActionListener(e -> updateProduct());
        controls.add(updateBtn);

        JButton deleteBtn = new JButton("Delete Product");
        deleteBtn.addActionListener(e -> deleteProduct());
        controls.add(deleteBtn);

        add(controls, BorderLayout.NORTH);

        // Table panel
        tableModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Product ID", "Name", "Category", "Price", "Low Stock Threshold", "Stock"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(productTable);
        add(tableScroll, BorderLayout.CENTER);

        // Load products into table
        loadProductsIntoTable();
    }

    private void loadProductsIntoTable() {
        tableModel.setRowCount(0);
        List<Product> products = inventoryService.getAllProducts();
        for (Product p : products) {
            int stock = inventoryService.getSellableStock(p.getProductId(), java.time.LocalDate.now());
            tableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getName(),
                    p.getCategory(),
                    p.getSellingPrice().toPlainString(),
                    p.getLowStockThreshold(),
                    stock
            });
        }
    }

    private void addProduct() {
        try {
            String productId = productIdField.getText().trim();
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            int threshold = Integer.parseInt(thresholdField.getText().trim());

            Product product = new Product(productId, name, category, price, threshold);
            inventoryService.addProduct(product);
            refreshData();
            JOptionPane.showMessageDialog(this, "Product added successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        try {
            String productId = productIdField.getText().trim();
            if (productId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a product ID first.");
                return;
            }
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            int threshold = Integer.parseInt(thresholdField.getText().trim());

            Product product = new Product(productId, name, category, price, threshold);
            inventoryService.updateProduct(product);
            refreshData();
            JOptionPane.showMessageDialog(this, "Product updated successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        try {
            String productId = productIdField.getText().trim();
            if (productId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a product ID to delete.");
                return;
            }
            inventoryService.deleteProduct(productId);
            refreshData();
            JOptionPane.showMessageDialog(this, "Product deleted successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshData() {
        loadProductsIntoTable();
        // Clear form fields
        productIdField.setText("");
        nameField.setText("");
        categoryField.setText("");
        priceField.setText("");
        thresholdField.setText("");
    }
}