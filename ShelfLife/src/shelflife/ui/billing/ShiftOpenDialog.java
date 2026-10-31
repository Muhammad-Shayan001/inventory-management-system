package shelflife.ui.billing;

import shelflife.model.CashierShift;
import shelflife.service.CashierShiftService;
import shelflife.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Dialog to initialize and open a cashier shift with opening cash float.
 */
public class ShiftOpenDialog extends JDialog {

    private final CashierShiftService shiftService;
    private final String cashierUsername;
    private CashierShift openedShift = null;

    public ShiftOpenDialog(Frame parent, CashierShiftService shiftService, String cashierUsername) {
        super(parent, "Open Cashier Shift", true);
        this.shiftService = shiftService;
        this.cashierUsername = cashierUsername != null ? cashierUsername : "cashier";

        setSize(420, 280);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(14, 18, 10, 18));
        JLabel lblTitle = new JLabel("Open Cashier Register Shift");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel lblSub = new JLabel("Cashier: " + this.cashierUsername);
        lblSub.setFont(Theme.FONT_SUBTITLE);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        topPanel.add(lblTitle);
        topPanel.add(lblSub);
        add(topPanel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 1, 8, 8));
        form.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel lblFloat = new JLabel("Opening Drawer Cash Float (PKR):");
        lblFloat.setFont(Theme.FONT_BOLD);
        JTextField txtFloat = new JTextField("2000.00");
        txtFloat.setFont(Theme.FONT_REGULAR);

        JLabel lblNotes = new JLabel("Shift Notes (Optional):");
        lblNotes.setFont(Theme.FONT_BOLD);
        JTextField txtNotes = new JTextField("Morning Shift Register 1");
        txtNotes.setFont(Theme.FONT_REGULAR);

        form.add(lblFloat);
        form.add(txtFloat);
        form.add(lblNotes);
        form.add(txtNotes);
        add(form, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(Theme.FONT_REGULAR);
        btnCancel.addActionListener(e -> dispose());

        JButton btnOpen = new JButton("Open Shift");
        btnOpen.setFont(Theme.FONT_BOLD);
        btnOpen.setBackground(Theme.ACCENT_PRIMARY);
        btnOpen.setForeground(Color.WHITE);
        btnOpen.addActionListener(e -> {
            try {
                BigDecimal fl = new BigDecimal(txtFloat.getText().trim());
                openedShift = shiftService.openShift(this.cashierUsername, fl, txtNotes.getText().trim());
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open shift: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomBar.add(btnCancel);
        bottomBar.add(btnOpen);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public CashierShift getOpenedShift() {
        return openedShift;
    }
}
