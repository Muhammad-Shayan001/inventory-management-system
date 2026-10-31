package shelflife.util;

import shelflife.model.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.util.List;

/**
 * Printable utility for generating 24-up A4 barcode label sheets
 * using pure JDK vector graphics (Graphics2D.fillRect) with zero third-party dependencies.
 */
public class BarcodeSheetPrinter implements Printable {

    private final List<Product> products;
    private static final int COLS = 3;
    private static final int ROWS = 8;
    private static final int LABELS_PER_PAGE = COLS * ROWS; // 24-up

    public BarcodeSheetPrinter(List<Product> products) {
        this.products = products;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        int totalPages = (int) Math.ceil((double) products.size() / LABELS_PER_PAGE);
        if (pageIndex >= totalPages) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double labelWidth = pageFormat.getImageableWidth() / COLS;
        double labelHeight = pageFormat.getImageableHeight() / ROWS;

        int startIndex = pageIndex * LABELS_PER_PAGE;
        int endIndex = Math.min(startIndex + LABELS_PER_PAGE, products.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            int col = slot % COLS;
            int row = slot / COLS;

            double x = col * labelWidth + 6;
            double y = row * labelHeight + 6;
            double w = labelWidth - 12;
            double h = labelHeight - 12;

            drawLabel(g2, products.get(i), (int) x, (int) y, (int) w, (int) h);
        }

        return PAGE_EXISTS;
    }

    private void drawLabel(Graphics2D g2, Product p, int x, int y, int w, int h) {
        // Label border
        g2.setColor(new Color(0xE2E8F0));
        g2.setStroke(new BasicStroke(0.75f));
        g2.drawRoundRect(x, y, w, h, 6, 6);

        // Product Name
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        String name = p.getName();
        if (name.length() > 22) name = name.substring(0, 20) + "..";
        g2.drawString(name, x + 8, y + 16);

        // Price
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(new Color(0x1E40AF));
        g2.drawString(CurrencyUtil.format(p.getSellingPrice()), x + 8, y + 30);

        // Barcode lines (Deterministic vector pseudo Code-128 barcode)
        String code = (p.getBarcode() != null && !p.getBarcode().isEmpty()) ? p.getBarcode() : p.getProductId();
        drawVectorBarcode(g2, code, x + 8, y + 36, w - 16, 22);

        // Human readable code text
        g2.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g2.setColor(new Color(0x4B5563));
        g2.drawString(code, x + 8, y + 68);
    }

    /**
     * Draws deterministic barcode bars directly using vector fillRect based on characters in code.
     */
    private void drawVectorBarcode(Graphics2D g2, String code, int x, int y, int width, int height) {
        g2.setColor(Color.BLACK);
        // Start guard pattern
        int curX = x;
        g2.fillRect(curX, y, 2, height);
        curX += 4;
        g2.fillRect(curX, y, 1, height);
        curX += 3;

        int step = Math.max(2, (width - 20) / Math.max(1, code.length() * 4));

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            int hash = (c * 31 + i * 17) & 0xFF;
            for (int b = 0; b < 3; b++) {
                boolean bar = ((hash >> b) & 1) == 1;
                int barW = (b == 1 && ((hash >> 3) & 1) == 1) ? step + 1 : step;
                if (bar) {
                    g2.fillRect(curX, y, barW, height);
                }
                curX += barW + 1;
                if (curX > x + width - 10) break;
            }
            if (curX > x + width - 10) break;
        }

        // Stop guard pattern
        curX = Math.min(curX + 2, x + width - 4);
        g2.fillRect(curX, y, 2, height);
    }

    public static void printLabels(Component parent, List<Product> products) {
        if (products == null || products.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No products selected to generate barcode labels.", "No Products", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("ShelfLife Barcode Labels");
        job.setPrintable(new BarcodeSheetPrinter(products));
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent, "Printing failed: " + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
