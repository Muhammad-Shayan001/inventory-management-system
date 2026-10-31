package shelflife.ui.billing;

import shelflife.model.Sale;
import shelflife.model.SaleItem;
import shelflife.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modal dialog displaying an itemized customer sales receipt.
 * Shows sale ID, date, line items with batch allocations, unit prices,
 * monetary totals, and payment method details.
 */
public class ReceiptDialog extends JDialog {

    private final Sale sale;
    private final List<SaleItem> items;
    private JTextArea txtReceipt;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReceiptDialog(Window owner, Sale sale, List<SaleItem> items) {
        super(owner, "Sales Receipt - " + (sale != null ? sale.getSaleId() : "New Sale"), ModalityType.APPLICATION_MODAL);
        this.sale = sale;
        this.items = items != null && !items.isEmpty() ? items : (sale != null ? sale.getItems() : null);

        initComponents();
        setSize(480, 620);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UITheme.BG_PAGE);

        txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtReceipt.setBackground(UITheme.CARD_BG);
        txtReceipt.setForeground(UITheme.TEXT_MAIN);
        txtReceipt.setBorder(new EmptyBorder(14, 16, 14, 16));
        txtReceipt.setText(buildReceiptText());
        txtReceipt.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(txtReceipt);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        pnlButtons.setOpaque(false);
        pnlButtons.setBorder(new EmptyBorder(0, 10, 10, 10));

        JButton btnCopy = UITheme.createSecondaryButton("Copy to Clipboard");
        btnCopy.addActionListener(e -> copyToClipboard());

        JButton btnSave = UITheme.createSecondaryButton("Save as File...");
        btnSave.addActionListener(e -> saveToFile());

        JButton btnClose = UITheme.createPrimaryButton("Close");
        btnClose.addActionListener(e -> dispose());

        pnlButtons.add(btnCopy);
        pnlButtons.add(btnSave);
        pnlButtons.add(btnClose);

        add(pnlButtons, BorderLayout.SOUTH);
    }

    private String buildReceiptText() {
        if (sale == null) {
            return "No sales transaction data available.";
        }

        StringBuilder sb = new StringBuilder();
        String lineSep = "==============================================================\n";
        String dashSep = "--------------------------------------------------------------\n";

        sb.append(lineSep);
        sb.append("                  SHELFLIFE RETAIL & POS                      \n");
        sb.append("            Inventory, Expiry & Billing Management            \n");
        sb.append(lineSep);
        sb.append(String.format("Receipt No  : %s\n", sale.getSaleId()));
        sb.append(String.format("Date & Time : %s\n", sale.getSaleDateTime() != null ? sale.getSaleDateTime().format(DISPLAY_FMT) : "N/A"));
        sb.append(String.format("Payment Mode: %s\n", sale.getPaymentMethod() != null ? sale.getPaymentMethod().getDisplayName() : "Cash"));
        sb.append(String.format("Status      : %s\n", sale.getPaymentStatus() != null ? sale.getPaymentStatus() : "PAID"));
        sb.append(dashSep);
        sb.append(String.format("%-18s %-10s %5s %10s %10s\n", "Item", "Batch", "Qty", "Price", "Total"));
        sb.append(dashSep);

        int totalUnits = 0;
        if (items != null && !items.isEmpty()) {
            for (SaleItem item : items) {
                String name = item.getProductName();
                if (name == null || name.isEmpty()) {
                    name = item.getProductId();
                }
                if (name.length() > 18) {
                    name = name.substring(0, 15) + "...";
                }

                String batch = item.getBatchId() != null ? item.getBatchId() : "-";
                if (batch.length() > 10) {
                    batch = batch.substring(0, 9) + ".";
                }

                int qty = item.getQuantity();
                totalUnits += qty;
                BigDecimal price = item.getUnitPrice();
                BigDecimal lineTot = item.getLineTotal();

                sb.append(String.format("%-18s %-10s %5d %10s %10s\n",
                        name, batch, qty, price.toPlainString(), lineTot.toPlainString()));
            }
        } else {
            sb.append("  (No itemized batch records found)\n");
        }

        sb.append(dashSep);
        sb.append(String.format("Total Units Sold : %d\n", totalUnits));
        sb.append(String.format("Subtotal         : Rs %s\n", sale.getSubtotal() != null ? sale.getSubtotal().toPlainString() : sale.getTotalAmount().toPlainString()));
        if (sale.getTax() != null && sale.getTax().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Tax (GST)        : Rs %s\n", sale.getTax().toPlainString()));
        }
        sb.append(String.format("FINAL TOTAL      : Rs %s\n", sale.getTotalAmount().toPlainString()));
        sb.append(lineSep);
        sb.append("           Thank you for shopping with ShelfLife!             \n");
        sb.append("        Please retain this receipt for batch returns.         \n");
        sb.append(lineSep);

        return sb.toString();
    }

    private void copyToClipboard() {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txtReceipt.getText()), null);
            JOptionPane.showMessageDialog(this, "Receipt copied to system clipboard.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to copy: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("receipt_" + (sale != null ? sale.getSaleId() : "sale") + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(target)) {
                out.print(txtReceipt.getText());
                JOptionPane.showMessageDialog(this, "Receipt saved successfully to: " + target.getName(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
