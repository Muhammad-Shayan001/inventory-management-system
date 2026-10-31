package shelflife.ui.billing;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.service.InventoryService;
import shelflife.service.SalesService;
import shelflife.service.SalesServiceImpl;
import shelflife.storage.CsvDataStore;
import shelflife.storage.DataStore;
import shelflife.test.MockInventoryService;

import javax.swing.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone demo application launcher for ShelfLife Member 3 (Billing & Sales Management).
 * Seeds realistic catalog and multi-batch inventory for interactive testing.
 */
public class BillingAppDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            File dataDir = new File("data");
            DataStore dataStore = new CsvDataStore(dataDir);
            seedInitialDataIfEmpty(dataStore);

            InventoryService inventoryService = new MockInventoryService(dataStore);
            SalesService salesService = new SalesServiceImpl(inventoryService, dataStore);

            JFrame frame = new JFrame("ShelfLife Billing & Sales Management - Member 3 POS");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 680);
            frame.setLocationRelativeTo(null);

            BillingPanel billingPanel = new BillingPanel(inventoryService, salesService);
            frame.setContentPane(billingPanel);

            frame.setVisible(true);
        });
    }

    private static void seedInitialDataIfEmpty(DataStore dataStore) {
        try {
            List<Product> products = dataStore.loadProducts();
            if (products.isEmpty()) {
                List<Product> seedProducts = new ArrayList<>();
                seedProducts.add(new Product("P101", "Organic Whole Milk 1L", "Dairy", new BigDecimal("3.50")));
                seedProducts.add(new Product("P102", "Artisan Sourdough Loaf", "Bakery", new BigDecimal("4.75")));
                seedProducts.add(new Product("P103", "Farm Fresh Large Eggs (12pk)", "Dairy", new BigDecimal("5.20")));
                seedProducts.add(new Product("P104", "Greek Vanilla Yogurt 500g", "Dairy", new BigDecimal("2.99")));
                seedProducts.add(new Product("P105", "Extra Virgin Olive Oil 750ml", "Pantry", new BigDecimal("12.50")));
                dataStore.saveProducts(seedProducts);

                List<ProductBatch> seedBatches = new ArrayList<>();
                LocalDate today = LocalDate.now();

                // Multi-batch for P101 (Milk) to demonstrate FEFO
                seedBatches.add(new ProductBatch("B101-A", "P101", 5, today.plusDays(3), today.minusDays(2)));  // Expires earliest
                seedBatches.add(new ProductBatch("B101-B", "P101", 10, today.plusDays(10), today.minusDays(1))); // Expires later
                seedBatches.add(new ProductBatch("B101-EXP", "P101", 8, today.minusDays(2), today.minusDays(12))); // Already EXPIRED!

                // P102 (Bread)
                seedBatches.add(new ProductBatch("B102-A", "P102", 7, today.plusDays(2), today));

                // P103 (Eggs)
                seedBatches.add(new ProductBatch("B103-A", "P103", 15, today.plusDays(21), today));

                // P104 (Yogurt)
                seedBatches.add(new ProductBatch("B104-A", "P104", 4, today.plusDays(5), today));
                seedBatches.add(new ProductBatch("B104-B", "P104", 6, today.plusDays(15), today));

                // P105 (Olive Oil)
                seedBatches.add(new ProductBatch("B105-A", "P105", 20, today.plusYears(1), today));

                dataStore.saveBatches(seedBatches);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
