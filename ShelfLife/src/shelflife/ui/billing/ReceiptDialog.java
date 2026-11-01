package shelflife.ui.billing;

import shelflife.model.AppSettings;
import shelflife.model.Sale;
import shelflife.model.SaleItem;

import javax.print.PrintService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.util.List;

/**
 * Professional Sales Receipt Dialog.
 * Renders an authentic 80mm thermal receipt visually on a custom canvas
 * and routes real thermal/laser print jobs to the operating system print spooler.
 */
public class ReceiptDialog extends JDialog {

    private final Sale sale;
    private final List<SaleItem> items;
    private final AppSettings settings;
    private final BigDecimal cashReceived;
    private final BigDecimal changeDue;
    private final String cashierName;
    private final String customerName;

    private ReceiptPreviewPanel previewPanel;

    public ReceiptDialog(Window owner, Sale sale, List<SaleItem> items, AppSettings settings,
                         BigDecimal cashReceived, BigDecimal changeDue,
                         String cashierName, String customerName) {
        super(owner, "Sales Receipt — " + (sale != null && sale.getSaleId() != null ? sale.getSaleId() : "New Sale"), ModalityType.APPLICATION_MODAL);
        this.sale = sale;
        this.items = items != null && !items.isEmpty() ? items : (sale != null ? sale.getItems() : null);
        if (this.sale != null && (this.sale.getItems() == null || this.sale.getItems().isEmpty()) && this.items != null) {
            this.sale.setItems(this.items);
        }
        this.settings = settings != null ? settings : new AppSettings();
        this.cashReceived = cashReceived != null ? cashReceived : BigDecimal.ZERO;
        this.changeDue = changeDue != null ? changeDue : BigDecimal.ZERO;
        this.cashierName = (cashierName != null && !cashierName.trim().isEmpty())
                ? cashierName.trim()
                : (sale != null && sale.getCashierUsername() != null ? sale.getCashierUsername() : "cashier");
        this.customerName = (customerName != null && !customerName.trim().isEmpty())
                ? customerName.trim()
                : (sale != null && sale.getCustomerId() != null && !sale.getCustomerId().trim().isEmpty() ? sale.getCustomerId().trim() : "Walk-in Customer");

        initComponents();
        setSize(500, 700);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    public ReceiptDialog(Window owner, Sale sale, List<SaleItem> items) {
        this(owner, sale, items, new AppSettings(), BigDecimal.ZERO, BigDecimal.ZERO,
                sale != null ? sale.getCashierUsername() : "cashier", "Walk-in Customer");
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Center Panel: Custom Visual Receipt Preview
        previewPanel = new ReceiptPreviewPanel();
        JScrollPane scrollPane = new JScrollPane(previewPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // South Panel: Single Styled Print Button
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        pnlBottom.setBackground(Color.WHITE);
        pnlBottom.setBorder(new EmptyBorder(8, 0, 8, 0));

        JButton btnPrint = new JButton("\uD83D\uDDA8  Print Receipt");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setBackground(new Color(0x2563EB)); // Royal Blue
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setBorderPainted(false);
        btnPrint.setPreferredSize(new Dimension(200, 40));
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> performPrint());

        pnlBottom.add(btnPrint);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private void performPrint() {
        if (sale == null) {
            JOptionPane.showMessageDialog(this, "No sales transaction to print.", "Print Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PrintService[] services = PrinterJob.lookupPrintServices();
        if (services == null || services.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "No printer found. Please connect a printer and try again.",
                    "No Printer",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ThermalReceiptRenderer renderer = new ThermalReceiptRenderer(
                    sale, settings, cashReceived, changeDue,
                    sale.isDuplicateReceipt(), sale.getReprintCount(),
                    cashierName, customerName
            );
            renderer.printToPrinter();

            // After successful print job initiation, mark reprint count for future prints
            sale.incrementReprintCount();
            if (previewPanel != null) {
                previewPanel.recalculateSizeAndRepaint();
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                    "Printing failed. Please check your printer and try again.\n" + ex.getMessage(),
                    "Print Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Custom visual renderer panel simulating thermal paper output.
     */
    private class ReceiptPreviewPanel extends JPanel {

        public ReceiptPreviewPanel() {
            setBackground(Color.WHITE);
            recalculateSizeAndRepaint();
        }

        public void recalculateSizeAndRepaint() {
            int lineCount = 30;
            if (sale != null) {
                ThermalReceiptRenderer renderer = new ThermalReceiptRenderer(
                        sale, settings, cashReceived, changeDue,
                        sale.isDuplicateReceipt(), sale.getReprintCount(),
                        cashierName, customerName
                );
                String text = renderer.renderTextReceipt();
                String[] lines = text.split("\n", -1);
                lineCount = lines.length;
            }
            int calcHeight = lineCount * 14 + 50;
            setPreferredSize(new Dimension(460, Math.max(calcHeight, 620)));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.BLACK);

            Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
            Font monoBold = new Font(Font.MONOSPACED, Font.BOLD, 10);
            Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, 13);
            g2.setFont(monoFont);
            FontMetrics fm = g2.getFontMetrics();
            int lineH = fm.getHeight() + 2;
            int x = 14;
            int y = 20;

            if (sale == null) {
                g2.setFont(monoFont);
                g2.drawString("No sales transaction data available.", x, y);
                return;
            }

            ThermalReceiptRenderer renderer = new ThermalReceiptRenderer(
                    sale, settings, cashReceived, changeDue,
                    sale.isDuplicateReceipt(), sale.getReprintCount(),
                    cashierName, customerName
            );
            String text = renderer.renderTextReceipt();
            String[] lines = text.split("\n", -1);

            String shopName = (settings != null && settings.getShopName() != null) ? settings.getShopName().trim() : "";
            String saleId = (sale != null && sale.getSaleId() != null) ? sale.getSaleId().trim() : "";

            for (String line : lines) {
                if (line.startsWith("=") || line.startsWith("-")) {
                    g2.setFont(monoFont);
                    g2.setColor(Color.GRAY);
                } else if (line.contains("TOTAL DUE") || line.contains("DUPLICATE") || line.contains("REPRINT")) {
                    g2.setFont(monoBold);
                    g2.setColor(line.contains("DUPLICATE") ? Color.RED : Color.BLACK);
                } else if ((!saleId.isEmpty() && line.contains(saleId))
                        || (!shopName.isEmpty() && line.contains(shopName))) {
                    g2.setFont(titleFont);
                    g2.setColor(Color.BLACK);
                } else {
                    g2.setFont(monoFont);
                    g2.setColor(Color.BLACK);
                }
                g2.drawString(line, x, y);
                y += lineH;
            }
        }
    }
}
