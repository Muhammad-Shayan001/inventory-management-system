package shelflife.ui.billing;

import shelflife.model.AppSettings;
import shelflife.model.SupplierPayable;
import shelflife.model.SupplierPayment;
import shelflife.util.CurrencyUtil;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.format.DateTimeFormatter;

/**
 * Official Supplier Payment Voucher Dialog.
 * Generates an executive voucher document suitable for recordkeeping and A4 printing.
 */
public class SupplierPaymentVoucherDialog extends JDialog {

    private static final int LINE_WIDTH = 56;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private final SupplierPayment payment;
    private final SupplierPayable payable;
    private final AppSettings settings;
    private final String recordedBy;

    public SupplierPaymentVoucherDialog(Window owner, SupplierPayment payment, SupplierPayable payable,
                                        AppSettings settings, String recordedBy) {
        super(owner, "Payment Voucher — " + (payment != null && payment.getPaymentId() != null ? payment.getPaymentId() : ""), ModalityType.APPLICATION_MODAL);
        this.payment = payment;
        this.payable = payable;
        this.settings = settings != null ? settings : new AppSettings();
        this.recordedBy = (recordedBy != null && !recordedBy.trim().isEmpty())
                ? recordedBy.trim()
                : (payment != null && payment.getRecordedBy() != null ? payment.getRecordedBy() : "admin");

        initComponents();
        setSize(560, 680);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    public SupplierPaymentVoucherDialog(Window owner, SupplierPayment payment, SupplierPayable payable) {
        this(owner, payment, payable, new AppSettings(), "admin");
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
                Font titleFont = new Font(Font.MONOSPACED, Font.BOLD, 12);
                g2.setFont(mono);
                FontMetrics fm = g2.getFontMetrics();
                int lh = fm.getHeight() + 2;
                int x = 20, y = 24;

                String[] lines = buildVoucherText().split("\n", -1);
                for (String line : lines) {
                    if (line.contains("OFFICIAL PAYMENT VOUCHER") || line.contains("Settlement Amount") || line.contains("TOTAL DISBURSED")) {
                        g2.setFont(titleFont);
                        g2.setColor(Color.decode("#1E40AF")); // Deep Blue
                    } else if (line.startsWith("=") || line.startsWith("-")) {
                        g2.setFont(mono);
                        g2.setColor(Color.GRAY);
                    } else if (line.contains("Remaining Balance") || line.contains("Total Payable Amount")) {
                        g2.setFont(monoBold);
                        g2.setColor(Color.BLACK);
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
        String[] lines = buildVoucherText().split("\n", -1);
        preview.setPreferredSize(new Dimension(520, Math.max(lines.length * 15 + 60, 580)));

        JScrollPane scroll = new JScrollPane(preview);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Print Button in South Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        JButton btnPrint = new JButton("\uD83D\uDDA8  Print Voucher (A4)");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setBackground(new Color(0x2563EB));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setBorderPainted(false);
        btnPrint.setPreferredSize(new Dimension(220, 40));
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> performPrint());

        btnPanel.add(btnPrint);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private String buildVoucherText() {
        if (payment == null && payable == null) {
            return "No supplier payment voucher data available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(center(settings.getShopName())).append("\n");
        if (settings.getStoreAddress() != null && !settings.getStoreAddress().isEmpty()) {
            sb.append(center(settings.getStoreAddress())).append("\n");
        }
        if (settings.getStorePhone() != null && !settings.getStorePhone().isEmpty()) {
            sb.append(center("Tel: " + settings.getStorePhone())).append("\n");
        }
        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(center("OFFICIAL PAYMENT VOUCHER")).append("\n");
        sb.append(rep('=', LINE_WIDTH)).append("\n");

        String pmtId = payment != null && payment.getPaymentId() != null ? payment.getPaymentId() : "VCH-" + System.currentTimeMillis();
        String pmtDate = payment != null && payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FMT) : "N/A";
        String suppId = payment != null && payment.getSupplierId() != null ? payment.getSupplierId() : (payable != null ? payable.getSupplierId() : "N/A");
        String purchRef = payable != null && payable.getPurchaseRef() != null ? payable.getPurchaseRef() : "N/A";
        String payId = payable != null && payable.getPayableId() != null ? payable.getPayableId() : "N/A";

        sb.append(rpad("Voucher No   : " + pmtId, LINE_WIDTH)).append("\n");
        sb.append(rpad("Payment Date : " + pmtDate, LINE_WIDTH)).append("\n");
        sb.append(rpad("Paid To      : " + suppId, LINE_WIDTH)).append("\n");
        sb.append(rpad("Payable Ref  : " + payId + " (PO: " + purchRef + ")", LINE_WIDTH)).append("\n");
        sb.append(rep('-', LINE_WIDTH)).append("\n");

        sb.append(center("SETTLEMENT & FINANCIAL BREAKDOWN")).append("\n");
        sb.append(rep('-', LINE_WIDTH)).append("\n");

        if (payable != null) {
            sb.append(sumLine("Total Payable Amount :", CurrencyUtil.format(payable.getTotalAmount()))).append("\n");
        }
        if (payment != null) {
            sb.append(sumLine("Settlement Amount    :", CurrencyUtil.format(payment.getAmount()))).append("\n");
            sb.append(rpad("Payment Method       : " + (payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "CASH"), LINE_WIDTH)).append("\n");
            if (payment.getReferenceNumber() != null && !payment.getReferenceNumber().isEmpty()) {
                sb.append(rpad("Reference / Trx No   : " + payment.getReferenceNumber(), LINE_WIDTH)).append("\n");
            }
            if (payment.getNotes() != null && !payment.getNotes().isEmpty()) {
                sb.append(rpad("Notes                : " + payment.getNotes(), LINE_WIDTH)).append("\n");
            }
        }
        if (payable != null) {
            sb.append(sumLine("Remaining Balance Due:", CurrencyUtil.format(payable.getBalanceDue()))).append("\n");
            sb.append(rpad("Account Status       : " + payable.getStatus(), LINE_WIDTH)).append("\n");
        }

        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(rpad("Recorded By : " + recordedBy, LINE_WIDTH)).append("\n");
        sb.append(rpad("Verified On : " + java.time.LocalDate.now().format(DATE_FMT), LINE_WIDTH)).append("\n");
        sb.append(rep('-', LINE_WIDTH)).append("\n");
        sb.append("\n");
        sb.append("Prepared By: __________________   Authorized: __________________\n");
        sb.append("\n");
        sb.append("Received By (Payee Signature): _________________________________\n");
        sb.append(rep('=', LINE_WIDTH)).append("\n");
        sb.append(center("System-Generated Commercial Payment Document")).append("\n");

        return sb.toString();
    }

    private void performPrint() {
        PrintService[] services = PrinterJob.lookupPrintServices();
        if (services == null || services.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "No printer found. Please connect a printer and try again.",
                    "No Printer",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        Paper paper = pf.getPaper();
        // A4 Paper: 595.27 x 841.89 points (72 DPI)
        paper.setSize(595.27, 841.89);
        paper.setImageableArea(36, 36, 523.27, 769.89);
        pf.setPaper(paper);

        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(MediaSizeName.ISO_A4);

        String text = buildVoucherText();
        job.setPrintable((g, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));

            String[] ls = text.split("\n", -1);
            int y = 20;
            for (String l : ls) {
                g2.drawString(l, 10, y);
                y += 14;
            }
            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog(attrs)) {
            try {
                job.print(attrs);
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
