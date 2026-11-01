package shelflife.ui.billing;

import shelflife.model.Customer;
import shelflife.model.PaymentLine;
import shelflife.model.PaymentMethod;
import shelflife.service.CustomerCreditService;
import shelflife.ui.theme.Theme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Split Payment Allocation Dialog.
 * Allows customers to settle bills across multiple tenders (Cash, Easypaisa, JazzCash, Card, Khata).
 */
public class SplitPaymentDialog extends JDialog {

    private final BigDecimal totalDue;
    private final CustomerCreditService creditService;

    private final JTextField txtCash = new JTextField("0.00");
    private final JTextField txtCashTendered = new JTextField("0.00");
    private final JLabel lblCashChange = new JLabel("Change Due: Rs. 0.00");

    private final JTextField txtEasypaisa = new JTextField("0.00");
    private final JTextField txtEasypaisaRef = new JTextField();

    private final JTextField txtJazzCash = new JTextField("0.00");
    private final JTextField txtJazzCashRef = new JTextField();

    private final JTextField txtCard = new JTextField("0.00");
    private final JTextField txtCardRef = new JTextField();

    private final JTextField txtKhata = new JTextField("0.00");
    private final JComboBox<Customer> cmbCustomer = new JComboBox<>();

    private final JLabel lblAllocated;
    private final JLabel lblRemaining;
    private final JButton btnConfirm;

    private boolean confirmed = false;
    private final List<PaymentLine> resultPayments = new ArrayList<>();
    private BigDecimal confirmedCashTendered = BigDecimal.ZERO;
    private BigDecimal confirmedChangeDue = BigDecimal.ZERO;
    private String selectedCustomerId = null;

    public SplitPaymentDialog(Frame parent, BigDecimal totalDue, CustomerCreditService creditService) {
        super(parent, "Split Payment Allocation", true);
        this.totalDue = totalDue != null ? totalDue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.creditService = creditService;

        setSize(650, 560);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(14, 20, 10, 20));

        JLabel lblTitle = new JLabel("Split Payment Allocation");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblDue = new JLabel("Total Bill Amount Due: " + CurrencyUtil.format(totalDue));
        lblDue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDue.setForeground(Theme.ACCENT_PRIMARY);

        topPanel.add(lblTitle);
        topPanel.add(lblDue);
        add(topPanel, BorderLayout.NORTH);

        // Center: Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Cash row
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("Cash Amount (PKR):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        form.add(txtCash, gbc);
        gbc.gridx = 2; gbc.weightx = 0.35;
        JPanel cashExtra = new JPanel(new GridLayout(2, 1));
        cashExtra.setOpaque(false);
        JPanel pTender = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pTender.setOpaque(false);
        pTender.add(new JLabel("Tendered:"));
        txtCashTendered.setPreferredSize(new Dimension(80, 26));
        pTender.add(txtCashTendered);
        cashExtra.add(pTender);
        lblCashChange.setFont(Theme.FONT_SMALL);
        lblCashChange.setForeground(new Color(0x166534));
        cashExtra.add(lblCashChange);
        form.add(cashExtra, gbc);

        // Easypaisa row
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("Easypaisa Amount (PKR):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        form.add(txtEasypaisa, gbc);
        gbc.gridx = 2; gbc.weightx = 0.35;
        txtEasypaisaRef.setToolTipText("Transaction ID / Phone #");
        form.add(wrapFieldWithLabel("Trans ID:", txtEasypaisaRef), gbc);

        // JazzCash row
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("JazzCash Amount (PKR):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        form.add(txtJazzCash, gbc);
        gbc.gridx = 2; gbc.weightx = 0.35;
        txtJazzCashRef.setToolTipText("Transaction ID / Phone #");
        form.add(wrapFieldWithLabel("Trans ID:", txtJazzCashRef), gbc);

        // Card row
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("Card Amount (PKR):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        form.add(txtCard, gbc);
        gbc.gridx = 2; gbc.weightx = 0.35;
        txtCardRef.setToolTipText("Approval Code / Last 4 Digits");
        form.add(wrapFieldWithLabel("Appr Code:", txtCardRef), gbc);

        // Customer Khata row
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        form.add(new JLabel("Customer Credit (Khata):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        form.add(txtKhata, gbc);
        gbc.gridx = 2; gbc.weightx = 0.35;
        populateCustomerCombo();
        form.add(cmbCustomer, gbc);

        // Status Summary Card
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        JPanel summaryCard = new JPanel(new GridLayout(2, 1, 4, 4));
        summaryCard.setBackground(new Color(0xF8FAFC));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                new EmptyBorder(10, 14, 10, 14)
        ));

        lblAllocated = new JLabel("Total Allocated: Rs. 0.00");
        lblAllocated.setFont(Theme.FONT_BOLD);
        lblRemaining = new JLabel("Remaining to Allocate: " + CurrencyUtil.format(totalDue));
        lblRemaining.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRemaining.setForeground(Theme.COLOR_DANGER_FG);

        summaryCard.add(lblAllocated);
        summaryCard.add(lblRemaining);
        form.add(summaryCard, gbc);

        add(new JScrollPane(form), BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(Theme.FONT_REGULAR);
        btnCancel.addActionListener(e -> dispose());

        btnConfirm = new JButton("Complete Split Checkout");
        btnConfirm.setFont(Theme.FONT_BOLD);
        btnConfirm.setBackground(Theme.ACCENT_PRIMARY);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setEnabled(false);
        btnConfirm.addActionListener(e -> onConfirm());

        bottomBar.add(btnCancel);
        bottomBar.add(btnConfirm);
        add(bottomBar, BorderLayout.SOUTH);

        // Hook live recalculation listeners
        txtCash.addCaretListener(e -> recalculate());
        txtCashTendered.addCaretListener(e -> recalculate());
        txtEasypaisa.addCaretListener(e -> recalculate());
        txtJazzCash.addCaretListener(e -> recalculate());
        txtCard.addCaretListener(e -> recalculate());
        txtKhata.addCaretListener(e -> recalculate());

        recalculate();
    }

    private JPanel wrapFieldWithLabel(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SMALL);
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void populateCustomerCombo() {
        cmbCustomer.removeAllItems();
        if (creditService != null) {
            for (Customer c : creditService.getAllCustomers()) {
                if (c.isActive()) {
                    cmbCustomer.addItem(c);
                }
            }
        }
    }

    private void recalculate() {
        BigDecimal cash = parseAmount(txtCash.getText());
        BigDecimal tendered = parseAmount(txtCashTendered.getText());
        BigDecimal ep = parseAmount(txtEasypaisa.getText());
        BigDecimal jc = parseAmount(txtJazzCash.getText());
        BigDecimal card = parseAmount(txtCard.getText());
        BigDecimal khata = parseAmount(txtKhata.getText());

        // Cash change
        if (cash.compareTo(BigDecimal.ZERO) > 0 && tendered.compareTo(cash) >= 0) {
            BigDecimal chg = tendered.subtract(cash);
            lblCashChange.setText("Change: " + CurrencyUtil.format(chg));
            confirmedChangeDue = chg;
        } else {
            lblCashChange.setText("Change: Rs. 0.00");
            confirmedChangeDue = BigDecimal.ZERO;
        }

        BigDecimal allocated = cash.add(ep).add(jc).add(card).add(khata);
        BigDecimal rem = totalDue.subtract(allocated);

        lblAllocated.setText("Total Allocated: " + CurrencyUtil.format(allocated));
        if (rem.compareTo(BigDecimal.ZERO) == 0) {
            lblRemaining.setText("Balance Settled Exactly (Rs. 0.00)");
            lblRemaining.setForeground(Theme.COLOR_SUCCESS_FG);
            btnConfirm.setEnabled(true);
        } else if (rem.compareTo(BigDecimal.ZERO) > 0) {
            lblRemaining.setText("Remaining to Allocate: " + CurrencyUtil.format(rem));
            lblRemaining.setForeground(Theme.COLOR_DANGER_FG);
            btnConfirm.setEnabled(false);
        } else {
            lblRemaining.setText("Over-allocated by: " + CurrencyUtil.format(rem.abs()));
            lblRemaining.setForeground(Theme.COLOR_DANGER_FG);
            btnConfirm.setEnabled(false);
        }
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void onConfirm() {
        resultPayments.clear();

        BigDecimal cash = parseAmount(txtCash.getText());
        if (cash.compareTo(BigDecimal.ZERO) > 0) {
            resultPayments.add(new PaymentLine(null, null, PaymentMethod.CASH, cash, "Cash Tender", LocalDateTime.now()));
            confirmedCashTendered = parseAmount(txtCashTendered.getText());
            if (confirmedCashTendered.compareTo(cash) < 0) {
                confirmedCashTendered = cash;
            }
        }

        BigDecimal ep = parseAmount(txtEasypaisa.getText());
        if (ep.compareTo(BigDecimal.ZERO) > 0) {
            resultPayments.add(new PaymentLine(null, null, PaymentMethod.EASYPAISA, ep, txtEasypaisaRef.getText().trim(), LocalDateTime.now()));
        }

        BigDecimal jc = parseAmount(txtJazzCash.getText());
        if (jc.compareTo(BigDecimal.ZERO) > 0) {
            resultPayments.add(new PaymentLine(null, null, PaymentMethod.JAZZCASH, jc, txtJazzCashRef.getText().trim(), LocalDateTime.now()));
        }

        BigDecimal card = parseAmount(txtCard.getText());
        if (card.compareTo(BigDecimal.ZERO) > 0) {
            resultPayments.add(new PaymentLine(null, null, PaymentMethod.DEBIT_CARD, card, txtCardRef.getText().trim(), LocalDateTime.now()));
        }

        BigDecimal khata = parseAmount(txtKhata.getText());
        if (khata.compareTo(BigDecimal.ZERO) > 0) {
            Customer cust = (Customer) cmbCustomer.getSelectedItem();
            if (cust == null) {
                JOptionPane.showMessageDialog(this, "Please select a valid customer for Khata credit.", "No Customer Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!creditService.hasSufficientCredit(cust.getCustomerId(), khata)) {
                JOptionPane.showMessageDialog(this, "Selected customer '" + cust.getFullName() + 
                        "' has insufficient credit limit (Limit: " + CurrencyUtil.format(cust.getCreditLimit()) + 
                        ", Balance: " + CurrencyUtil.format(cust.getCurrentBalance()) + ")", "Credit Limit Exceeded", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedCustomerId = cust.getCustomerId();
            resultPayments.add(new PaymentLine(null, null, PaymentMethod.STORE_CREDIT, khata, "Khata: " + cust.getFullName(), LocalDateTime.now()));
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public List<PaymentLine> getResultPayments() {
        return resultPayments;
    }

    public List<PaymentLine> getAllocatedPayments() {
        return resultPayments;
    }

    public BigDecimal getConfirmedCashTendered() {
        return confirmedCashTendered;
    }

    public BigDecimal getConfirmedChangeDue() {
        return confirmedChangeDue;
    }

    public String getSelectedCustomerId() {
        return selectedCustomerId;
    }
}
