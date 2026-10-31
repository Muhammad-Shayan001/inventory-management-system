package shelflife.ui.billing;

import shelflife.model.Sale;
import shelflife.model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.format.DateTimeFormatter;

/**
 * Receipt presentation dialog showing completed sale details,
 * item breakdown, batch allocations, and monetary totals.
 */
public class ReceiptDialog extends JDialog {
    private static final DateTimeFormatter RECEIPT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReceiptDialog(Frame owner, Sale sale) {
        super(owner, "Official Sales Receipt - " + (sale != null ? sale.getSaleId() : ""), true);
        initUI(sale);
    }

    public ReceiptDialog(Dialog owner, Sale sale) {
        super(owner, "Official Sales Receipt - " + (sale != null ? sale.getSaleId() : ""), true);
        initUI(sale);
    }

    private void initUI(Sale sale) {
        setLayout(new BorderLayout(10, 10));
        setSize(480, 560);
        setLocationRelativeTo(getOwner());

        String receiptText = generateReceiptText(sale);

        JTextArea txtReceipt = new JTextArea(receiptText);
        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtReceipt.setMargin(new Insets(12, 12, 12, 12));
        txtReceipt.setBackground(new Color(252, 252, 250));

        JScrollPane scrollPane = new JScrollPane(txtReceipt);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JButton btnCopy = new JButton("Copy Receipt");
        btnCopy.setToolTipText("Copy formatted receipt text to system clipboard");
        btnCopy.addActionListener(e -> {
            StringSelection selection = new StringSelection(receiptText);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            JOptionPane.showMessageDialog(this, "Receipt copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        buttonPanel.add(btnCopy);
        buttonPanel.add(btnClose);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public static String generateReceiptText(Sale sale) {
        if (sale == null) {
            return "No sale details available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("                SHELFLIFE SUPERMARKET               \n");
        sb.append("          Point of Sale - Cashier Terminal          \n");
        sb.append("====================================================\n");
        sb.append(String.format("Sale ID:    %s\n", sale.getSaleId()));
        sb.append(String.format("Date/Time:  %s\n", sale.getSaleDateTime().format(RECEIPT_DATE_FMT)));
        sb.append(String.format("Payment:    %s (%s)\n",
                sale.getPaymentMethod() != null ? sale.getPaymentMethod().getDisplayName() : "N/A",
                sale.getPaymentStatus() != null ? sale.getPaymentStatus().getDisplayName() : "N/A"));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-26s %4s %8s %9s\n", "ITEM / BATCH", "QTY", "PRICE", "TOTAL"));
        sb.append("----------------------------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            String prodName = item.getProductName();
            if (prodName.length() > 25) {
                prodName = prodName.substring(0, 22) + "...";
            }
            sb.append(String.format("%-26s %4d %8s %9s\n",
                    prodName,
                    item.getQuantity(),
                    "$" + item.getUnitPrice().toPlainString(),
                    "$" + item.getLineTotal().toPlainString()));
            sb.append(String.format("  [Batch: %s]\n", item.getBatchId()));
        }

        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-38s %11s\n", "Subtotal:", "$" + sale.getSubtotal().toPlainString()));
        sb.append(String.format("%-38s %11s\n", "Tax (0.00%):", "$" + sale.getTax().toPlainString()));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-38s %11s\n", "FINAL TOTAL:", "$" + sale.getFinalTotal().toPlainString()));
        sb.append("====================================================\n");
        sb.append("         FEFO Inventory Allocation Committed        \n");
        sb.append("         Thank you for shopping at ShelfLife!       \n");
        sb.append("====================================================\n");

        return sb.toString();
    }

    public static void showReceipt(Component parent, Sale sale) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        ReceiptDialog dialog;
        if (window instanceof Frame) {
            dialog = new ReceiptDialog((Frame) window, sale);
        } else if (window instanceof Dialog) {
            dialog = new ReceiptDialog((Dialog) window, sale);
        } else {
            dialog = new ReceiptDialog((Frame) null, sale);
        }
        dialog.setVisible(true);
    }
}
