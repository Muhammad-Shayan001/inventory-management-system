package shelflife.test;

import shelflife.model.*;
import shelflife.service.*;
import shelflife.storage.CsvDataStore;
import shelflife.ui.LoginDialog;
import shelflife.ui.ShelfLifeApp;
import shelflife.ui.SignUpDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * High-fidelity screenshot generation tool for ShelfLife.
 * Initializes isolated test fixtures in a temporary directory and renders
 * every application screen and modal at standard desktop resolution (1366x768)
 * for before/after visual QA audits.
 */
public class ScreenshotTool {

    public static void main(String[] args) {
        String targetSubdir = (args.length > 0) ? args[0] : "before";
        File outputDir = new File("docs/screenshots/" + targetSubdir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        System.out.println(">>> Starting Screenshot Capture -> " + outputDir.getAbsolutePath());

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("shelflife_screenshot_fixture_").toFile();
            CsvDataStore ds = new CsvDataStore(tempDir);
            seedFixtureData(ds);

            // Capture standalone dialogs
            captureLoginDialog(ds, new File(outputDir, "01_login.png"));
            captureSignUpDialog(ds, new File(outputDir, "02_signup.png"));

            // Capture main window screens
            captureMainWindowScreens(ds, outputDir);

            System.out.println(">>> All screenshots successfully captured in " + outputDir.getPath());
        } catch (Exception ex) {
            System.err.println("Fatal error during screenshot capture: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (tempDir != null && tempDir.exists()) {
                deleteRecursively(tempDir);
            }
        }
    }

    private static void seedFixtureData(CsvDataStore ds) throws Exception {
        // Products
        List<Product> products = new ArrayList<>();
        products.add(new Product("PROD-01", "Farm Fresh Whole Milk 1L", "Dairy & Eggs", new BigDecimal("260.00"), 15, "8964001112233", "SKU-MILK-01", true));
        products.add(new Product("PROD-02", "Artisan Whole Wheat Sourdough", "Bakery", new BigDecimal("180.00"), 10, "8964002223344", "SKU-BREAD-02", true));
        products.add(new Product("PROD-03", "Greek Style Plain Yogurt 500g", "Dairy & Eggs", new BigDecimal("320.00"), 12, "8964003334455", "SKU-YOG-03", true));
        products.add(new Product("PROD-04", "Pure Ceylon Premium Tea 400g", "Beverages", new BigDecimal("750.00"), 8, "8964004445566", "SKU-TEA-04", true));
        products.add(new Product("PROD-05", "Crispy Organic Corn Flakes 375g", "Breakfast Cereals", new BigDecimal("550.00"), 10, "8964005556677", "SKU-CRN-05", true));
        products.add(new Product("PROD-06", "Himalayan Pink Rock Salt 800g", "Pantry Essentials", new BigDecimal("120.00"), 20, "8964006667788", "SKU-SLT-06", true));
        products.add(new Product("PROD-07", "Cold-Pressed Canola Oil 1L", "Pantry Essentials", new BigDecimal("650.00"), 10, "8964007778899", "SKU-OIL-07", true));
        ds.saveProducts(products);

        // Batches with varying expiry dates
        List<ProductBatch> batches = new ArrayList<>();
        LocalDate today = LocalDate.now();
        batches.add(new ProductBatch("B-01-EXP", "PROD-01", 8, today.minusDays(3), new BigDecimal("190.00"), "SUP-01", today.minusDays(20)));
        batches.add(new ProductBatch("B-01-WARN", "PROD-01", 14, today.plusDays(4), new BigDecimal("195.00"), "SUP-01", today.minusDays(10)));
        batches.add(new ProductBatch("B-01-SAFE", "PROD-01", 25, today.plusDays(25), new BigDecimal("200.00"), "SUP-01", today.minusDays(2)));
        batches.add(new ProductBatch("B-02-EXP", "PROD-02", 5, today.minusDays(1), new BigDecimal("130.00"), "SUP-02", today.minusDays(5)));
        batches.add(new ProductBatch("B-02-SAFE", "PROD-02", 18, today.plusDays(5), new BigDecimal("135.00"), "SUP-02", today));
        batches.add(new ProductBatch("B-03-WARN", "PROD-03", 10, today.plusDays(7), new BigDecimal("240.00"), "SUP-01", today.minusDays(8)));
        batches.add(new ProductBatch("B-04-SAFE", "PROD-04", 30, today.plusMonths(8), new BigDecimal("580.00"), "SUP-02", today.minusDays(30)));
        batches.add(new ProductBatch("B-05-SAFE", "PROD-05", 22, today.plusMonths(6), new BigDecimal("420.00"), "SUP-01", today.minusDays(15)));
        batches.add(new ProductBatch("B-06-SAFE", "PROD-06", 50, today.plusYears(2), new BigDecimal("85.00"), "SUP-02", today.minusDays(60)));
        ds.saveBatches(batches);

        // Suppliers
        List<Supplier> suppliers = new ArrayList<>();
        suppliers.add(new Supplier("SUP-01", "Premier Wholesale Traders", "Muhammad Ali", "0300-1234567", "orders@premierwholesale.pk", "Industrial Area, Lahore", true));
        suppliers.add(new Supplier("SUP-02", "Al-Madina FMCG Distributors", "Tariq Mehmood", "0321-9876543", "sales@almadina.pk", "Wholesale Market, Karachi", true));
        suppliers.add(new Supplier("SUP-03", "National Beverage Supply", "Farhan Saeed", "0333-5551234", "info@nationalbeverage.pk", "Sector I-9, Islamabad", true));
        ds.saveSuppliers(suppliers);

        // Sales history
        List<Sale> sales = new ArrayList<>();
        List<SaleItem> saleItems = new ArrayList<>();
        Sale s1 = new Sale("SALE-20261027-101500-1001", today.atTime(10, 15), new BigDecimal("1080.00"), PaymentMethod.CASH, PaymentStatus.PAID);
        sales.add(s1);
        saleItems.add(new SaleItem("SI-001", s1.getSaleId(), "PROD-01", "Farm Fresh Whole Milk 1L", "B-01-WARN", 2, new BigDecimal("260.00")));
        saleItems.add(new SaleItem("SI-002", s1.getSaleId(), "PROD-05", "Crispy Organic Corn Flakes 375g", "B-05-SAFE", 1, new BigDecimal("550.00")));
        saleItems.add(new SaleItem("SI-003", s1.getSaleId(), "PROD-06", "Himalayan Pink Rock Salt 800g", "B-06-SAFE", 1, new BigDecimal("120.00")));
        ds.saveSales(sales);
        ds.saveSaleItems(saleItems);

        // Users
        List<User> users = new ArrayList<>();
        users.add(new User("USER-001", "admin", "admin", "salt", Role.ADMIN, "System Administrator", true, false));
        users.add(new User("USER-002", "cashier", "cashier", "salt", Role.CASHIER, "Fatima Noor", true, false));
        ds.saveUsers(users);

        // Settings
        AppSettings settings = new AppSettings("ShelfLife Supermarket (Gulberg)", "PKR", "Rs.", 15, 10, new BigDecimal("5.00"));
        ds.saveSettings(settings);
    }

    private static void captureLoginDialog(CsvDataStore ds, File targetFile) {
        try {
            AuthService auth = new AuthServiceImpl(ds);
            LoginDialog dlg = new LoginDialog((Frame) null, auth);
            dlg.addNotify();
            dlg.setSize(500, 380);
            dlg.doLayout();
            dlg.validate();
            renderComponentToPng(dlg, targetFile);
            dlg.dispose();
            System.out.println("Captured: " + targetFile.getName());
        } catch (Exception e) {
            System.err.println("Failed capturing LoginDialog: " + e.getMessage());
        }
    }

    private static void captureSignUpDialog(CsvDataStore ds, File targetFile) {
        try {
            AuthService auth = new AuthServiceImpl(ds);
            SignUpDialog dlg = new SignUpDialog((Frame) null, auth);
            dlg.addNotify();
            dlg.setSize(480, 520);
            dlg.doLayout();
            dlg.validate();
            renderComponentToPng(dlg, targetFile);
            dlg.dispose();
            System.out.println("Captured: " + targetFile.getName());
        } catch (Exception e) {
            System.err.println("Failed capturing SignUpDialog: " + e.getMessage());
        }
    }

    private static void captureMainWindowScreens(CsvDataStore ds, File outputDir) {
        try {
            ShelfLifeApp app = new ShelfLifeApp(ds, false);
            app.addNotify();
            app.setSize(1366, 768);
            app.doLayout();
            app.validate();

            String[] cards = {
                "inventory",
                "suppliers",
                "expiry",
                "billing",
                "returns",
                "reports",
                "reorder",
                "logs",
                "settings"
            };

            String[] fileNames = {
                "03_inventory_products.png",
                "04_suppliers.png",
                "05_expiry_alerts.png",
                "06_billing_pos.png",
                "07_returns.png",
                "08_dashboard_reports.png",
                "09_reorder_list.png",
                "10_activity_logs.png",
                "11_settings_backup.png"
            };

            for (int i = 0; i < cards.length; i++) {
                app.showCard(cards[i]);
                app.doLayout();
                app.validate();
                Thread.sleep(100); // allow layout dispatch
                File target = new File(outputDir, fileNames[i]);
                renderComponentToPng(app, target);
                System.out.println("Captured: " + target.getName() + " (card: " + cards[i] + ")");
            }

            app.dispose();
        } catch (Exception e) {
            System.err.println("Failed capturing main window screens: " + e.getMessage());
        }
    }

    private static void renderComponentToPng(Component comp, File file) throws Exception {
        comp.addNotify();
        comp.doLayout();
        comp.validate();
        int width = Math.max(comp.getWidth(), 400);
        int height = Math.max(comp.getHeight(), 300);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (comp instanceof RootPaneContainer) {
            JLayeredPane layered = ((RootPaneContainer) comp).getLayeredPane();
            layered.setSize(width, height);
            layered.doLayout();
            layered.validate();
            layered.paint(g2);
        } else {
            comp.setSize(width, height);
            comp.doLayout();
            comp.validate();
            comp.paint(g2);
        }
        g2.dispose();

        ImageIO.write(image, "png", file);
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
