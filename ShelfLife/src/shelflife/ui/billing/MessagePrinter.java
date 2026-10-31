package shelflife.ui.billing;

import shelflife.model.Sale;
import shelflife.model.SaleItem;

import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import javax.swing.*;

public class MessagePrinter {

    public static void print(Sale sale, Component parent) {
        if (sale == null) {
            JOptionPane.showMessageDialog(parent,
                    "No sales data available to print.",
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();

        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) {
                    return Printable.NO_SUCH_PAGE;
                }

                Graphics2D g2d = (Graphics2D) graphics.create();
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                g2d.scale(0.95, 0.95);

                String text = buildReceiptText(sale);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));

                int y = 15;
                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (line.isEmpty()) {
                        y += 10;
                        continue;
                    }
                    g2d.drawString(line, 15, y);
                    y += 14;
                }
                g2d.dispose();
                return PAGE_EXISTS;
            }

            private String buildReceiptText(Sale sale) {
                StringBuilder sb = new StringBuilder();
                sb.append("========================================\n");
                sb.append("      SHELFLIFE RETAIL & POS           \n");
                sb.append("   Inventory, Expiry & Billing Management\n");
                sb.append("========================================\n\n");
                sb.append(String.format("Receipt No  : %s\n", sale.getSaleId()));
                sb.append(String.format("Date & Time : %s\n",
                        sale.getSaleDateTime() != null ? sale.getSaleDateTime().toString() : "N/A"));
                sb.append(String.format("Payment Mode: %s\n",
                        sale.getPaymentMethod() != null ? sale.getPaymentMethod().getDisplayName() : "Cash"));
                sb.append(String.format("Status      : %s\n", sale.getPaymentStatus() != null ? sale.getPaymentStatus() : "PAID"));
                sb.append("----------------------------------------\n");
                sb.append(String.format("%-18s %-10s %5s %10s %10s\n", "Item", "Batch", "Qty", "Price", "Total"));
                sb.append("----------------------------------------\n");

                if (sale.getItems() != null && !sale.getItems().isEmpty()) {
                    int totalUnits = 0;
                    for (SaleItem item : sale.getItems()) {
                        String name = item.getProductName();
                        if (name == null || name.isEmpty()) {
                            name = item.getProductId();
                        }
                        if (name.length() > 15) {
                            name = name.substring(0, 14) + "...";
                        }
                        String batch = item.getBatchId() != null ? item.getBatchId() : "-";
                        int qty = item.getQuantity();
                        totalUnits += qty;
                        BigDecimal price = item.getUnitPrice();
                        BigDecimal lineTot = item.getLineTotal();

                        sb.append(String.format("%-18s %-10s %5d %10s %10s\n",
                                name, batch, qty, price.toPlainString(), lineTot.toPlainString()));
                    }
                    sb.append("----------------------------------------\n");
                    sb.append(String.format("Total Units Sold : %d\n", totalUnits));
                }

                sb.append(String.format("Subtotal         : Rs %s\n",
                        sale.getSubtotal() != null ? sale.getSubtotal().toPlainString() : "0.00"));
                if (sale.getTax() != null && sale.getTax().compareTo(BigDecimal.ZERO) > 0) {
                    sb.append(String.format("Tax (GST)        : Rs %s\n", sale.getTax().toPlainString()));
                }
                sb.append(String.format("FINAL TOTAL      : Rs %s\n", sale.getTotalAmount().toPlainString()));
                sb.append("========================================\n");
                sb.append("   Thank you for shopping with ShelfLife!\n");
                sb.append("  Please retain this receipt for returns.\n");
                sb.append("========================================");
                return sb.toString();
            }
        });

        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent,
                        "Printing failed: " + ex.getMessage(),
                        "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}