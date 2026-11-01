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
        String targetSubdir = (args.length > 0) ? args[0] : "";
        File outputDir = new File("docs/screenshots/" + targetSubdir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        System.out.println(">>> Starting Screenshot Capture -> " + outputDir.getAbsolutePath());

        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("shelflife_screenshot_fixture_").toFile();
            DemoShop.build(tempDir);
            CsvDataStore ds = new CsvDataStore(tempDir);
            ds.saveSettings(new AppSettings("Al-Noor General Store", "PKR", "Rs.", 15, 10, new BigDecimal("0.00")));

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
                "overview",
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
                "08_business_overview.png",
                "09_dashboard_reports.png",
                "10_reorder_list.png",
                "11_activity_logs.png",
                "12_settings_backup.png"
            };

            for (int i = 0; i < cards.length; i++) {
                app.showCard(cards[i]);
                app.doLayout();
                app.validate();
                Thread.sleep(900); // let background data loading finish before the picture is taken
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
