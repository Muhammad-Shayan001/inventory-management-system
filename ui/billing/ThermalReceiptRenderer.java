package shelflife.ui.billing;

import shelflife.model.*;
import shelflife.util.CurrencyUtil;

import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Enterprise 80mm (42-column) Thermal Receipt Layout and Printable Engine.
 * Generates both formatted text representation and vector Java Printable graphics.
 */
public class ThermalReceiptRenderer implements Printable {

    private static final int LINE_WIDTH = 42;
    private static final DateTimeFormatter RECEIPT_DT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    private final Sale sale;
    private final AppSettings settings;
    private final BigDecimal cashReceived;
    private final BigDecimal changeDue;
    private final boolean isDuplicate;
    private final int copyNumber;
    private final String cashierName;
    private final String customerName;

    public ThermalReceiptRenderer(Sale sale, AppSettings settings, BigDecimal cashReceived,
                                  BigDecimal changeDue, boolean isDuplicate, int copyNumber,
                                  String cashierName, String customerName) {
        this.sale = sale;
        this.settings = settings != null ? settings : new AppSettings();
        this.cashReceived = cashReceived != null ? cashReceived : BigDecimal.ZERO;
        this.changeDue = changeDue != null ? changeDue : BigDecimal.ZERO;
        this.isDuplicate = isDuplicate;
        this.copyNumber = copyNumber;
        this.cashierName = (cashierName != null && !cashierName.isEmpty()) ? cashierName : (sale.getCashierUsername() != null ? sale.getCashierUsername() : "cashier");
        this.customerName = (customerName != null && !customerName.isEmpty()) ? customerName : "Walk-in Customer";
    }

    /**
     * Builds the exact 42-column fixed-width text for display and printing.
     */
    public String renderTextReceipt() {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(center(settings.getShopName())).append("\n");
        if (settings.getStoreAddress() != null && !settings.getStoreAddress().isEmpty()) {
            sb.append(center(settings.getStoreAddress())).append("\n");
        }
        if (settings.getStorePhone() != null && !settings.getStorePhone().isEmpty()) {
            sb.append(center("Tel: " + settings.getStorePhone())).append("\n");
        }
        sb.append(repeat('=', LINE_WIDTH)).append("\n");

        // Duplicate Banner
        if (isDuplicate) {
            sb.append(center("*** DUPLICATE COPY #" + copyNumber + " ***")).append("\n");
            sb.append(center("REPRINTED: " + java.time.LocalDateTime.now().format(RECEIPT_DT))).append("\n");
            sb.append(repeat('-', LINE_WIDTH)).append("\n");
        }

        // Sale Metadata
        sb.append(padRight("Receipt ID: " + sale.getSaleId(), LINE_WIDTH)).append("\n");
        sb.append(padRight("Date/Time : " + sale.getSaleDateTime().format(RECEIPT_DT), LINE_WIDTH)).append("\n");
        sb.append(padRight("Cashier   : " + cashierName, LINE_WIDTH)).append("\n");
        sb.append(padRight("Customer  : " + customerName, LINE_WIDTH)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        // Table Header: Qty  Item                    Price    Total
        sb.append(padRight("Qty  Description", 24))
          .append(padLeft("Price", 8))
          .append(padLeft("Total", 10)).append("\n");
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        // Line Items
        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (name.length() > 18) {
                name = name.substring(0, 18);
            }
            String lineDesc = String.format("%-4d %-18s", item.getQuantity(), name);
            String unitPriceStr = CurrencyUtil.formatCompact(item.getUnitPrice());
            String totalStr = CurrencyUtil.formatCompact(item.getLineTotal());

            sb.append(lineDesc)
              .append(padLeft(unitPriceStr, 8))
              .append(padLeft(totalStr, 10)).append("\n");
        }
        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        // Subtotal & Financials
        sb.append(formatSummaryLine("Subtotal:", CurrencyUtil.formatCompact(sale.getSubtotal()))).append("\n");

        if (sale.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(formatSummaryLine("Discount:", "-" + CurrencyUtil.formatCompact(sale.getDiscountAmount()))).append("\n");
        }

        if (sale.getTax().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(formatSummaryLine("Tax (" + settings.getTaxRatePercent() + "%):", CurrencyUtil.formatCompact(sale.getTax()))).append("\n");
        }

        sb.append(repeat('=', LINE_WIDTH)).append("\n");
        sb.append(formatSummaryLine("TOTAL DUE (PKR):", CurrencyUtil.format(sale.getTotalAmount()))).append("\n");
        sb.append(repeat('=', LINE_WIDTH)).append("\n");

        // Payment Details
        List<PaymentLine> payments = sale.getPaymentLines();
        if (payments != null && !payments.isEmpty()) {
            sb.append("PAYMENT BREAKDOWN:\n");
            for (PaymentLine pl : payments) {
                String methodStr = pl.getPaymentMethod().getDisplayName();
                if (pl.getReferenceNote() != null && !pl.getReferenceNote().isEmpty()) {
                    methodStr += " (" + pl.getReferenceNote() + ")";
                }
                sb.append(formatSummaryLine("  " + methodStr + ":", CurrencyUtil.format(pl.getAmount()))).append("\n");
            }
        } else {
            sb.append(formatSummaryLine("Payment Method:", sale.getPaymentMethod().getDisplayName())).append("\n");
        }

        if (cashReceived.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(formatSummaryLine("Cash Tendered:", CurrencyUtil.format(cashReceived))).append("\n");
            sb.append(formatSummaryLine("Change Due:", CurrencyUtil.format(changeDue))).append("\n");
        }

        sb.append(repeat('-', LINE_WIDTH)).append("\n");

        // Footer & Barcode text representation
        sb.append(center("BARCODE: *" + sale.getSaleId() + "*")).append("\n");
        if (settings.getReceiptFooter() != null && !settings.getReceiptFooter().isEmpty()) {
            sb.append("\n").append(wrapCenter(settings.getReceiptFooter(), LINE_WIDTH)).append("\n");
        }
        sb.append(center("Software by ShelfLife POS")).append("\n");

        return sb.toString();
    }

    private String formatSummaryLine(String label, String value) {
        int valLen = value.length();
        int labelLen = LINE_WIDTH - valLen;
        if (labelLen < 0) labelLen = 0;
        return padRight(label, labelLen) + value;
    }

    private static String center(String text) {
        if (text == null) return "";
        if (text.length() >= LINE_WIDTH) return text;
        int padding = (LINE_WIDTH - text.length()) / 2;
        return repeat(' ', padding) + text;
    }

    private static String wrapCenter(String text, int width) {
        StringBuilder sb = new StringBuilder();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        for (String w : words) {
            if (currentLine.length() + w.length() + 1 > width) {
                sb.append(center(currentLine.toString())).append("\n");
                currentLine.setLength(0);
            }
            if (currentLine.length() > 0) currentLine.append(" ");
            currentLine.append(w);
        }
        if (currentLine.length() > 0) {
            sb.append(center(currentLine.toString()));
        }
        return sb.toString();
    }

    private static String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }

    private static String padLeft(String s, int n) {
        if (s == null) s = "";
        return String.format("%" + n + "s", s);
    }

    private static String repeat(char c, int count) {
        char[] arr = new char[Math.max(0, count)];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 9));

        String[] lines = renderTextReceipt().split("\n");
        int y = 15;
        for (String line : lines) {
            g2d.drawString(line, 5, y);
            y += 12;
        }

        return PAGE_EXISTS;
    }

    public void printToPrinter() throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            job.print();
        }
    }
}
