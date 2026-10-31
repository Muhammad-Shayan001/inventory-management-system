package shelflife.example;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.service.InventoryService;
import shelflife.service.InventoryServiceImpl;
import shelflife.service.SalesService;
import shelflife.service.SalesServiceImpl;
import shelflife.storage.DataStore;
import shelflife.storage.FileManager;
import shelflife.ui.billing.BillingPanel;

import javax.swing.*;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * Standalone executable demo launcher for ShelfLife Member 3 (Billing & Sales Management).
 * Can be run directly in NetBeans, Eclipse, IntelliJ, or from command line:
 * `java -cp bin shelflife.example.BillingDemo`
 */
public class BillingDemo {

    public static void main(String[] args) {
        // Set modern system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize local CSV data store
                Path dataDir = Paths.get("data");
                File dir = dataDir.toFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                DataStore dataStore = new FileManager(dataDir);
                InventoryService inventoryService = new InventoryServiceImpl(dataStore);
                SalesService salesService = new SalesServiceImpl(inventoryService, dataStore);

                // Populate sample data if product catalog is empty
                seedSampleDataIfEmpty(dataStore, inventoryService);

                // Create and display main application window
                JFrame frame = new JFrame("ShelfLife — Point-of-Sale & Billing (Member 3 Demo)");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1000, 680);
                frame.setLocationRelativeTo(null);

                BillingPanel billingPanel = new BillingPanel(inventoryService, salesService);
                frame.setContentPane(billingPanel);

                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to launch Billing Demo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void seedSampleDataIfEmpty(DataStore dataStore, InventoryService inventoryService) {
        if (!inventoryService.getAllProducts().isEmpty()) {
            return; // Data already exists
        }

        LocalDate today = LocalDate.now();

        // 1. Fresh Milk (P101) - Demonstrates Multi-Batch FEFO Allocation & Expired Exclusion
        Product milk = new Product("P101", "Fresh Whole Milk 1L", "Dairy", new BigDecimal("65.00"), 10);
        inventoryService.addProduct(milk);

        // Batch 1: Expiring soon (In 2 days) -> Should be allocated 1st!
        inventoryService.addBatch(new ProductBatch("B-MILK-01", "P101", 10, today.plusDays(2)));
        // Batch 2: Expiring later (In 7 days) -> Should be allocated 2nd!
        inventoryService.addBatch(new ProductBatch("B-MILK-02", "P101", 15, today.plusDays(7)));
        // Batch 3: Already Expired (3 days ago) -> MUST BE STRICTLY EXCLUDED!
        inventoryService.addBatch(new ProductBatch("B-MILK-EXP", "P101", 8, today.minusDays(3)));

        // 2. Whole Wheat Bread (P102)
        Product bread = new Product("P102", "Whole Wheat Farm Bread", "Bakery", new BigDecimal("45.00"), 5);
        inventoryService.addProduct(bread);
        inventoryService.addBatch(new ProductBatch("B-BREAD-01", "P102", 12, today.plusDays(3)));
        inventoryService.addBatch(new ProductBatch("B-BREAD-02", "P102", 10, today.plusDays(5)));

        // 3. Greek Yogurt (P103)
        Product yogurt = new Product("P103", "Greek Style Yogurt 500g", "Dairy", new BigDecimal("120.00"), 5);
        inventoryService.addProduct(yogurt);
        inventoryService.addBatch(new ProductBatch("B-YOG-01", "P103", 6, today.plusDays(4)));

        // 4. Organic Eggs (P104)
        Product eggs = new Product("P104", "Organic Brown Eggs 12pk", "Poultry", new BigDecimal("185.00"), 8);
        inventoryService.addProduct(eggs);
        inventoryService.addBatch(new ProductBatch("B-EGG-01", "P104", 15, today.plusDays(10)));

        // 5. Canned Tuna (P105) - Long Shelf Life
        Product tuna = new Product("P105", "Skipjack Tuna Flakes 180g", "Pantry", new BigDecimal("210.00"), 15);
        inventoryService.addProduct(tuna);
        inventoryService.addBatch(new ProductBatch("B-TUNA-01", "P105", 30, today.plusDays(180)));
    }
}
