package shelflife.ui.billing;

import shelflife.model.CashierShift;
import shelflife.service.CashierShiftService;
import shelflife.ui.ToastNotification;
import shelflife.ui.theme.Theme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Dialog to close and reconcile a cashier shift using the exact cash formula:
 * Expected Cash = Opening Float + Cash Sales - Cash Refunds
 * Cash Difference = Actual Counted Cash - Expected Cash
 */
public class ShiftCloseDialog extends JDialog {

    private final CashierShiftService shiftService;
    private final CashierShift shift;

    private final JTextField txtActualCash = new JTextField();
    private final JLabel lblDifference = new JLabel("Difference: Rs. 0.00");
    private final JTextField txtCloseNotes = new JTextField();
    private boolean shiftClosed = false;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    public ShiftCloseDialog(Frame parent, CashierShiftService shiftService, CashierShift shift) {
        super(parent, "Close & Reconcile Shift", true);
        this.shiftService = shiftService;
        this.shift = shift;

        setSize(480, 480);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(14, 20, 10, 20));
        JLabel lblTitle = new JLabel("Cash Drawer Shift Reconciliation");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel lblSub = new JLabel("Shift ID: " + shift.getShiftId() + " | Cashier: " + shift.getCashierUsername());
        lblSub.setFont(Theme.FONT_SUBTITLE);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        topPanel.add(lblTitle);
        topPanel.add(lblSub);
        add(topPanel, BorderLayout.NORTH);

        // Center breakdown
        JPanel body = new JPanel(new GridLayout(7, 2, 8, 8));
        body.setBorder(new EmptyBorder(10, 20, 10, 20));

        body.add(new JLabel("Shift Opened At:"));
        body.add(new JLabel(shift.getOpenedAt().format(DT_FMT)));

        body.add(new JLabel("Opening Float:"));
        body.add(new JLabel(CurrencyUtil.format(shift.getOpeningFloat())));

        body.add(new JLabel("Total Cash Sales (+):"));
        body.add(new JLabel(CurrencyUtil.format(shift.getCashSales())));

        body.add(new JLabel("Total Cash Refunds (-):"));
        body.add(new JLabel(CurrencyUtil.format(shift.getCashRefunds())));

        BigDecimal expected = shift.calculateExpectedCash();
        JLabel lblExpTitle = new JLabel("Expected Cash in Drawer:");
        lblExpTitle.setFont(Theme.FONT_BOLD);
        JLabel lblExpVal = new JLabel(CurrencyUtil.format(expected));
        lblExpVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblExpVal.setForeground(Theme.ACCENT_PRIMARY);
        body.add(lblExpTitle);
        body.add(lblExpVal);

        JLabel lblAct = new JLabel("Actual Counted Cash (PKR):");
        lblAct.setFont(Theme.FONT_BOLD);
        body.add(lblAct);
        body.add(txtActualCash);

        body.add(new JLabel("Cash Variance (Diff):"));
        lblDifference.setFont(Theme.FONT_BOLD);
        body.add(lblDifference);

        add(body, BorderLayout.CENTER);

        // Notes and buttons
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBorder(new EmptyBorder(10, 20, 14, 20));

        JPanel pNote = new JPanel(new BorderLayout(4, 0));
        pNote.add(new JLabel("Closing Notes / Variance Reason:"), BorderLayout.NORTH);
        pNote.add(txtCloseNotes, BorderLayout.CENTER);
        bottom.add(pNote, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(Theme.FONT_REGULAR);
        btnCancel.addActionListener(e -> dispose());

        JButton btnCloseShift = new JButton("Reconcile & Close Shift");
        btnCloseShift.setFont(Theme.FONT_BOLD);
        btnCloseShift.setBackground(new Color(0x991B1B)); // Red
        btnCloseShift.setForeground(Color.WHITE);
        btnCloseShift.addActionListener(e -> onReconcile());

        btnPanel.add(btnCancel);
        btnPanel.add(btnCloseShift);
        bottom.add(btnPanel, BorderLayout.SOUTH);

        add(bottom, BorderLayout.SOUTH);

        txtActualCash.addCaretListener(e -> updateDifference());
        updateDifference();
    }

    private void updateDifference() {
        BigDecimal actual = parseAmount(txtActualCash.getText());
        BigDecimal expected = shift.calculateExpectedCash();
        BigDecimal diff = actual.subtract(expected);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            lblDifference.setText("Balanced Exactly (Rs. 0.00)");
            lblDifference.setForeground(Theme.COLOR_SUCCESS_FG);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            lblDifference.setText("SHORT: -" + CurrencyUtil.format(diff.abs()));
            lblDifference.setForeground(Theme.COLOR_DANGER_FG);
        } else {
            lblDifference.setText("OVER: +" + CurrencyUtil.format(diff));
            lblDifference.setForeground(new Color(0x92400E));
        }
    }

    private BigDecimal parseAmount(String t) {
        if (t == null || t.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(t.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void onReconcile() {
        if (txtActualCash.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the actual cash counted in the drawer.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal actual = new BigDecimal(txtActualCash.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            shiftService.closeShift(shift.getShiftId(), actual, txtCloseNotes.getText().trim());
            shiftClosed = true;
            ToastNotification.show(getParent(), "Shift " + shift.getShiftId() + " reconciled and closed.", ToastNotification.Type.SUCCESS);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to close shift: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isShiftClosed() {
        return shiftClosed;
    }
}
