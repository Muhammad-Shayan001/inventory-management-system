package shelflife.ui.billing;

import shelflife.model.*;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Professional Return & Refund Receipt Dialog.
 * Renders a formatted return document and supports real printing.
 */
public class ReturnReceiptDialog extends JDialog {

    private static final int LINE_WIDTH = 42;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final SaleReturn saleReturn;
    private final AppSettings settings;
    private final String authorizedBy;

    public ReturnReceiptDialog(Window owner, SaleReturn saleReturn, AppSettings settings, String authorizedBy) {
        super(owner, "Return Receipt — " + (saleReturn != null && saleReturn.getReturnId() != null ? saleReturn.getReturnId() : ""), ModalityType.APPLICATION_MODAL);
        this.saleReturn = saleReturn;
        this.settings = settings != null ? settings : new AppSettings();
        this.authorizedBy = (authorizedBy != null && !authorizedBy.trim().isEmpty()) ? authorizedBy.trim() : "ADMIN";
        initComponents();
        setSize(500, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // Backward-compatible constructor
    public ReturnReceiptDialog(Window owner, SaleReturn saleReturn) {
        this(owner, saleReturn, new AppSettings(), "ADMIN");
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Preview Panel
        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Color.BLACK);
                Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 10);
                Font monoBold = new Font(Font.MONOSPACED, Font.BOLD, 10);
                g2.setFont(mono);
                FontMetrics fm = g2.getFontMetrics();
                int lh = fm.getHeight() + 2;
                int x = 14, y = 20;
                String[] lines = buildReturnText().split("\n", -1);
                for (String line : lines) {
                    if (line.contains("RETURN RECEIPT") || line.contains("TOTAL REFUND") || line.contains("RETURN / REFUND")) {
                        g2.setFont(monoBold);
                        g2.setColor(Color.decode("#DC2626"));
                    } else if (line.startsWith("=") || line.startsWith("-")) {
                        g2.setFont(mono);
                        g2.setColor(Color.GRAY);
                    } else {
                        g2.setFont(mono);
                        g2.setColor(Color.BLACK);
                    }
                    g2.drawString(line, x, y);
                    y += lh;
                }
            }
        };
        preview.setBackground(Color.WHITE);
        String[] lines = buildReturnText().split("\n", -1);
        preview.setPreferredSize(new Dimension(460, Math.max(lines.length * 14 + 50, 500)));

        JScrollPane scroll = new JScrollPane(preview);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Print Button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(new EmptyBorder(8, 0, 8, 0));
        JButton btnPrint = new JButton("\uD83D\uDDA8  Print Return Receipt");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setBackground(new Color(0xDC2626)); // Crimson Red
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setBorderPainted(false);
        btnPrint.setPreferredSize(new Dimension(220, 40));
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> performPrint());
        btnPanel.add(btnPrint);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private String buildReturnText() {
        if (saleReturn == null) return "No return data available.";
        StringBuilder sb = new StringBuilder();
        sb.append(center(settings.getShopName())).append("\n");
        if (settings.getStoreAddress() != null && !settings.getStoreAddress().isEmpty()) {
            sb.append(center(settings.getStoreAddress())).append("\n");
        }
        if (settings.getStorePhone() != null && !settings.getStorePhone().isEmpty()) {
            sb.append(center("Tel: " + settings.getStorePhone())).append("\n");
        }
        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(center("RETURN / REFUND RECEIPT")).append("\n");
        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(rpad("Return Ref  : " + (saleReturn.getReturnId() != null ? saleReturn.getReturnId() : "N/A"), LINE_WIDTH)).append("\n");
        sb.append(rpad("Orig. Sale  : " + (saleReturn.getOriginalSaleId() != null ? saleReturn.getOriginalSaleId() : "N/A"), LINE_WIDTH)).append("\n");
        sb.append(rpad("Return Date : " + (saleReturn.getReturnDate() != null ? saleReturn.getReturnDate().format(DT_FMT) : "N/A"), LINE_WIDTH)).append("\n");
        sb.append(rpad("Processed By: " + authorizedBy, LINE_WIDTH)).append("\n");
        sb.append(rpad("Reason      : " + (saleReturn.getReason() != null ? saleReturn.getReason() : "Customer Return"), LINE_WIDTH)).append("\n");
        sb.append(rep('-', LINE_WIDTH)).append("\n");
        sb.append(String.format("%-20s %4s %8s %8s\n", "Product", "Qty", "Price", "Refund"));
        sb.append(rep('-', LINE_WIDTH)).append("\n");
        List<SaleReturnItem> items = saleReturn.getItems();
        if (items != null && !items.isEmpty()) {
            for (SaleReturnItem it : items) {
                String nm = it.getProductName() != null ? it.getProductName() : (it.getProductId() != null ? it.getProductId() : "Item");
                if (nm.length() > 20) nm = nm.substring(0, 18) + "..";
                sb.append(String.format("%-20s %4d %8s %8s\n",
                        nm, it.getQuantity(),
                        CurrencyUtil.formatCompact(it.getUnitPrice()),
                        CurrencyUtil.formatCompact(it.getLineRefund())));
                boolean isRestock = "RESTOCK".equalsIgnoreCase(it.getDisposition());
                if (isRestock) {
                    sb.append(rpad("  [Restocked to inventory]", LINE_WIDTH)).append("\n");
                } else {
                    sb.append(rpad("  [Waste — not restocked]", LINE_WIDTH)).append("\n");
                }
            }
        }
        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(sumLine("TOTAL REFUND:", CurrencyUtil.format(saleReturn.getTotalRefund()))).append("\n");
        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(center("Authorized by: " + authorizedBy)).append("\n");
        sb.append(center("Signature: ________________")).append("\n");
        sb.append(rep('-', LINE_WIDTH)).append("\n");
        if (settings.getReceiptFooter() != null && !settings.getReceiptFooter().isEmpty()) {
            sb.append(center(settings.getReceiptFooter())).append("\n");
        }
        return sb.toString();
    }

    private void performPrint() {
        javax.print.PrintService[] services = PrinterJob.lookupPrintServices();
        if (services == null || services.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "No printer found. Please connect a printer and try again.",
                    "No Printer",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        String text = buildReturnText();
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
            g2.setColor(Color.BLACK);
            String[] ls = text.split("\n", -1);
            int y = 14;
            for (String l : ls) {
                g2.drawString(l, 4, y);
                y += 12;
            }
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                        "Printing failed: " + ex.getMessage(),
                        "Print Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String center(String s) {
        if (s == null) return "";
        if (s.length() >= LINE_WIDTH) return s;
        int p = (LINE_WIDTH - s.length()) / 2;
        char[] pad = new char[p];
        java.util.Arrays.fill(pad, ' ');
        return new String(pad) + s;
    }

    private String rpad(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }

    private String rep(char c, int n) {
        char[] a = new char[Math.max(0, n)];
        java.util.Arrays.fill(a, c);
        return new String(a);
    }

    private String sumLine(String label, String val) {
        if (val == null) val = "";
        int vl = val.length();
        int ll = LINE_WIDTH - vl;
        if (ll < 0) ll = 0;
        return rpad(label, ll) + val;
    }
}
