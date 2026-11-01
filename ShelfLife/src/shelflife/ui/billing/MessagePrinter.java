package shelflife.ui.billing;

import shelflife.model.Sale;

import javax.swing.*;
import java.awt.*;

/**
 * Legacy adapter routing print requests to the modern ReceiptDialog.
 */
public class MessagePrinter {

    private MessagePrinter() {}

    public static void print(Sale sale, Component parent) {
        if (sale == null) {
            JOptionPane.showMessageDialog(parent,
                    "No sales data available to print.",
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Window owner = (parent instanceof Window) ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        ReceiptDialog dialog = new ReceiptDialog(owner, sale, sale.getItems());
        dialog.setVisible(true);
    }
}