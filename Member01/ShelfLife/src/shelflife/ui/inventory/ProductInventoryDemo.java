package shelflife.ui.inventory;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.service.InventoryService;
import shelflife.service.InventoryServiceImpl;
import shelflife.storage.InMemoryDataStore;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Standalone demo launcher for Member 1: Product & Inventory Management module.
 * Can be run directly from Apache NetBeans or terminal to test and demonstrate the UI.
 */
public class ProductInventoryDemo {

    public static void main(String[] args) {
        // Set modern system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            // 1. Initialize storage and service with sample data
            InMemoryDataStore dataStore = new InMemoryDataStore();
            seedSampleData(dataStore);

            InventoryService inventoryService = new InventoryServiceImpl(dataStore);

            // 2. Create the Member 1 ProductInventoryPanel
            ProductInventoryPanel panel = new ProductInventoryPanel(inventoryService);

            // 3. Open in a top-level demo window
            JFrame frame = new JFrame("ShelfLife — Member 1: Product & Inventory Management");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel);
            frame.setSize(1080, 680);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void seedSampleData(InMemoryDataStore store) {
        // Sample products
        Product p1 = new Product("P001", "Full Cream Milk 1L", "Dairy", new BigDecimal("220.00"), 10);
        Product p2 = new Product("P002", "Whole Wheat Sandwich Bread", "Bakery", new BigDecimal("120.00"), 15);
        Product p3 = new Product("P003", "Farm Fresh Eggs 12pk", "Dairy", new BigDecimal("340.00"), 8);
        Product p4 = new Product("P004", "Plain Greek Yogurt 500g", "Dairy", new BigDecimal("290.00"), 5);
        Product p5 = new Product("P005", "Organic Orange Juice 1L", "Beverages", new BigDecimal("380.00"), 12);
        Product p6 = new Product("P006", "Artisan Sourdough Loaf", "Bakery", new BigDecimal("250.00"), 5);

        store.addProduct(p1);
        store.addProduct(p2);
        store.addProduct(p3);
        store.addProduct(p4);
        store.addProduct(p5);
        store.addProduct(p6);

        // Sample batches with various expiry dates
        LocalDate today = LocalDate.now();
        store.addBatch(new ProductBatch("B001", "P001", 12, today.plusDays(4)));
        store.addBatch(new ProductBatch("B002", "P001", 8, today.plusDays(10)));
        store.addBatch(new ProductBatch("B003", "P002", 5, today.plusDays(2))); // low stock condition
        store.addBatch(new ProductBatch("B004", "P003", 20, today.plusDays(14)));
        store.addBatch(new ProductBatch("B005", "P004", 4, today.minusDays(2))); // expired batch
        store.addBatch(new ProductBatch("B006", "P005", 15, today.plusMonths(2)));
    }
}
