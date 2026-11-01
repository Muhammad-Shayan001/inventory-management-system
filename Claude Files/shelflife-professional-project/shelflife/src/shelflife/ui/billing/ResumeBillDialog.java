package shelflife.ui.billing;

import shelflife.model.HeldBill;
import shelflife.model.HeldBillItem;
import shelflife.service.HeldBillService;
import shelflife.ui.ToastNotification;
import shelflife.ui.theme.Theme;
import shelflife.util.CurrencyUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog to inspect, resume, or discard held billing sessions.
 */
public class ResumeBillDialog extends JDialog {

    private final HeldBillService heldBillService;
    private final DefaultTableModel billsModel;
    private final JTable tblBills;
    private final DefaultTableModel itemsModel;
    private final JTable tblItems;
    private List<HeldBillItem> resumedItems = null;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MMM HH:mm:ss");

    public ResumeBillDialog(Frame parent, HeldBillService heldBillService) {
        super(parent, "Resume Held Bills", true);
        this.heldBillService = heldBillService;

        setSize(780, 480);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(14, 18, 10, 18));
        JLabel lblTitle = new JLabel("Suspended POS Billing Sessions");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel lblSub = new JLabel("Select a held bill to inspect items and restore them into your active POS cart.");
        lblSub.setFont(Theme.FONT_SUBTITLE);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Center split: Bills on top, Items of selected bill on bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.55);

        String[] billCols = {"Hold ID", "Created At", "Cashier", "Customer", "Items", "Est. Total", "Note"};
        billsModel = new DefaultTableModel(billCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBills = new JTable(billsModel);
        tblBills.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tblBills.setFont(Theme.FONT_TABLE_CELL);
        tblBills.getTableHeader().setFont(Theme.FONT_TABLE_HEADER);
        tblBills.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onBillSelected();
            }
        });

        JScrollPane scrollBills = new JScrollPane(tblBills);
        scrollBills.setBorder(BorderFactory.createTitledBorder("Active Held Bills"));
        splitPane.setTopComponent(scrollBills);

        String[] itemCols = {"Product ID", "Description", "Quantity", "Unit Price", "Line Total"};
        itemsModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblItems = new JTable(itemsModel);
        tblItems.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tblItems.setFont(Theme.FONT_TABLE_CELL);
        tblItems.getTableHeader().setFont(Theme.FONT_TABLE_HEADER);

        JScrollPane scrollItems = new JScrollPane(tblItems);
        scrollItems.setBorder(BorderFactory.createTitledBorder("Held Bill Items Preview"));
        splitPane.setBottomComponent(scrollItems);

        add(splitPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnDiscard = new JButton("Discard Selected");
        btnDiscard.setFont(Theme.FONT_REGULAR);
        btnDiscard.setForeground(Theme.COLOR_DANGER_FG);
        btnDiscard.addActionListener(e -> discardSelected());

        JButton btnCancel = new JButton("Close");
        btnCancel.setFont(Theme.FONT_REGULAR);
        btnCancel.addActionListener(e -> dispose());

        JButton btnResume = new JButton("Resume Bill into Cart");
        btnResume.setFont(Theme.FONT_BOLD);
        btnResume.setBackground(Theme.ACCENT_PRIMARY);
        btnResume.setForeground(Color.WHITE);
        btnResume.addActionListener(e -> resumeSelected());

        bottomBar.add(btnDiscard);
        bottomBar.add(btnCancel);
        bottomBar.add(btnResume);
        add(bottomBar, BorderLayout.SOUTH);

        refreshBills();
    }

    private void refreshBills() {
        billsModel.setRowCount(0);
        itemsModel.setRowCount(0);
        List<HeldBill> list = heldBillService.getAllHeldBills();
        for (HeldBill b : list) {
            billsModel.addRow(new Object[]{
                    b.getHeldBillId(),
                    b.getCreatedAt().format(DT_FMT),
                    b.getCashierUsername(),
                    b.getCustomerName(),
                    b.getItemCount(),
                    CurrencyUtil.format(b.getEstimatedTotal()),
                    b.getNote()
            });
        }
    }

    private void onBillSelected() {
        itemsModel.setRowCount(0);
        int row = tblBills.getSelectedRow();
        if (row < 0) return;

        String billId = (String) billsModel.getValueAt(row, 0);
        List<HeldBillItem> items = heldBillService.getHeldBillItems(billId);
        for (HeldBillItem it : items) {
            itemsModel.addRow(new Object[]{
                    it.getProductId(),
                    it.getProductName(),
                    it.getQuantity(),
                    CurrencyUtil.format(it.getUnitPrice()),
                    CurrencyUtil.format(it.getLineTotal())
            });
        }
    }

    private void resumeSelected() {
        int row = tblBills.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a held bill to resume.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String billId = (String) billsModel.getValueAt(row, 0);
        try {
            resumedItems = heldBillService.resumeBill(billId);
            ToastNotification.show(getParent(), "Resumed held bill " + billId + " with " + resumedItems.size() + " items.", ToastNotification.Type.SUCCESS);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to resume bill: " + ex.getMessage(), "Resume Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void discardSelected() {
        int row = tblBills.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a held bill to discard.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String billId = (String) billsModel.getValueAt(row, 0);
        int conf = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently discard held bill: " + billId + "?", "Confirm Discard", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            try {
                heldBillService.discardBill(billId);
                ToastNotification.show(this, "Held bill discarded.", ToastNotification.Type.INFO);
                refreshBills();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to discard bill: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public List<HeldBillItem> getResumedItems() {
        return resumedItems;
    }
}
