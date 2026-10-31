package shelflife.ui.billing;

import shelflife.model.Sale;
import shelflife.model.SaleItem;

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
 * Shows sale ID, date, item allocations with specific batch IDs, unit prices,
 * monetary totals, and payment details.
 */
public class ReceiptDialog extends JDialog {

    private final Sale sale;
    private final List<SaleItem> items;
    private JTextArea txtReceipt;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReceiptDialog(Window owner, Sale sale, List<SaleItem> items) {
        super(owner, "Sales Receipt — " + (sale != null ? sale.getSaleId() : "New Sale"), ModalityType.APPLICATION_MODAL);
        this.sale = sale;
        this.items = items != null && !items.isEmpty() ? items : (sale != null ? sale.getItems() : null);

        initComponents();
        setSize(480, 620);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Receipt text area in scroll pane
        txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtReceipt.setBackground(new Color(255, 255, 255));
        txtReceipt.setForeground(new Color(30, 30, 30));
        txtReceipt.setBorder(new EmptyBorder(12, 12, 12, 12));
        txtReceipt.setText(buildReceiptText());
        txtReceipt.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(txtReceipt);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 220)));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Buttons
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        pnlButtons.setBorder(new EmptyBorder(0, 10, 10, 10));

        JButton btnCopy = new JButton("Copy to Clipboard");
        btnCopy.setFocusPainted(false);
        btnCopy.addActionListener(e -> copyToClipboard());

        JButton btnSave = new JButton("Save as File...");
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> saveToFile());

        JButton btnClose = new JButton("Close");
        btnClose.setFocusPainted(false);
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
        String lineSeparator = "==============================================================\n";
        String dashSeparator = "--------------------------------------------------------------\n";

        sb.append(lineSeparator);
        sb.append("                  SHELFLIFE SUPERMARKET & POS                 \n");
        sb.append("              Batch-Tracked Inventory & Billing               \n");
        sb.append("                 Tel: +92-300-1234567 | Member 3              \n");
        sb.append(lineSeparator);
        sb.append(String.format("Receipt No  : %s\n", sale.getSaleId()));
        sb.append(String.format("Date & Time : %s\n", sale.getSaleDateTime() != null ? sale.getSaleDateTime().format(DISPLAY_FMT) : "N/A"));
        sb.append(String.format("Payment Mode: %s\n", sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "Cash"));
        sb.append(String.format("Status      : %s\n", sale.getPaymentStatus() != null ? sale.getPaymentStatus() : "PAID"));
        sb.append(dashSeparator);
        sb.append(String.format("%-20s %-10s %5s %10s %10s\n", "Item", "Batch", "Qty", "Price", "Total"));
        sb.append(dashSeparator);

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        if (items != null && !items.isEmpty()) {
            for (SaleItem item : items) {
                String name = item.getProductName() != null ? item.getProductName() : item.getProductId();
                if (name.length() > 20) {
                    name = name.substring(0, 17) + "...";
                }
                String batch = item.getBatchId() != null ? item.getBatchId() : "N/A";
                int qty = item.getQuantity();
                BigDecimal price = item.getUnitPrice();
                BigDecimal total = item.getLineTotal();

                calculatedSubtotal = calculatedSubtotal.add(total);

                sb.append(String.format("%-20s %-10s %5d %10.2f %10.2f\n",
                        name, batch, qty, price.doubleValue(), total.doubleValue()));
            }
        } else {
            sb.append("  (No item details recorded)\n");
        }

        sb.append(dashSeparator);
        BigDecimal subtotal = sale.getSubtotal() != null ? sale.getSubtotal() : calculatedSubtotal;
        BigDecimal tax = sale.getTax() != null ? sale.getTax() : BigDecimal.ZERO;
        BigDecimal finalTotal = sale.getTotalAmount() != null ? sale.getTotalAmount() : subtotal.add(tax);

        sb.append(String.format("%-40s Rs. %12.2f\n", "Subtotal:", subtotal.doubleValue()));
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("%-40s Rs. %12.2f\n", "Tax / Surcharge:", tax.doubleValue()));
        }
        sb.append(lineSeparator);
        sb.append(String.format("%-40s Rs. %12.2f\n", "FINAL TOTAL PAID:", finalTotal.doubleValue()));
        sb.append(lineSeparator);
        sb.append("             FEFO: Multi-Batch Allocation Verified            \n");
        sb.append("         Thank you for choosing ShelfLife Supermarket!        \n");
        sb.append("                 Please retain your receipt!                  \n");
        sb.append(lineSeparator);

        return sb.toString();
    }

    private void copyToClipboard() {
        try {
            String text = txtReceipt.getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "Receipt copied to system clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to copy receipt: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Receipt_" + (sale != null ? sale.getSaleId() : "Sale") + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(target, "UTF-8")) {
                writer.print(txtReceipt.getText());
                JOptionPane.showMessageDialog(this, "Receipt saved successfully to:\n" + target.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
