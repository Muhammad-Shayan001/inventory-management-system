package shelflife.ui.reports;

import shelflife.model.ActivityLogEntry;
import shelflife.service.ActivityLogService;
import shelflife.ui.Refreshable;
import shelflife.ui.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only Activity Audit Trail Panel for administrative oversight.
 */
public class ActivityLogPanel extends JPanel implements Refreshable {

    private final ActivityLogService logService;

    private DefaultTableModel tableModel;
    private JTable tblLogs;
    private JTextField txtSearch;
    private JComboBox<String> cmbUserFilter;

    public ActivityLogPanel(ActivityLogService logService) {
        this.logService = logService;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Top Filter Header
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBackground(UITheme.CARD_BG);
        topPanel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filterBar.setOpaque(false);

        JLabel lblSearch = new JLabel("Search Action/Details:");
        lblSearch.setFont(UITheme.FONT_REGULAR);
        filterBar.add(lblSearch);

        txtSearch = new JTextField(18);
        UITheme.styleTextField(txtSearch);
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyFilter();
            }
        });
        filterBar.add(txtSearch);

        JLabel lblUser = new JLabel("User:");
        lblUser.setFont(UITheme.FONT_REGULAR);
        filterBar.add(lblUser);

        cmbUserFilter = new JComboBox<>(new String[]{"All Users"});
        cmbUserFilter.setFont(UITheme.FONT_REGULAR);
        cmbUserFilter.addActionListener(e -> applyFilter());
        filterBar.add(cmbUserFilter);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnExport = UITheme.createPrimaryButton("Export Logs (CSV)");
        btnExport.addActionListener(e -> exportToCsv());

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh");
        btnRefresh.addActionListener(e -> refreshData());

        btnBar.add(btnExport);
        btnBar.add(btnRefresh);

        topPanel.add(filterBar, BorderLayout.WEST);
        topPanel.add(btnBar, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"Log ID", "Date & Time", "User", "Action", "Record ID", "Details"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblLogs = new JTable(tableModel);
        UITheme.styleTable(tblLogs);
        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(140);
        tblLogs.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblLogs.getColumnModel().getColumn(3).setPreferredWidth(130);
        tblLogs.getColumnModel().getColumn(4).setPreferredWidth(120);
        tblLogs.getColumnModel().getColumn(5).setPreferredWidth(350);

        JScrollPane scroll = new JScrollPane(tblLogs);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        if (logService != null) {
            logService.reload();
        }

        // Refresh user filter choices
        if (cmbUserFilter != null) {
            String selected = (String) cmbUserFilter.getSelectedItem();
            cmbUserFilter.removeAllItems();
            cmbUserFilter.addItem("All Users");
            if (logService != null) {
                java.util.Set<String> users = new java.util.HashSet<>();
                for (ActivityLogEntry l : logService.getAllLogs()) {
                    if (l.getUsername() != null && !l.getUsername().isEmpty()) {
                        users.add(l.getUsername());
                    }
                }
                for (String u : users) {
                    cmbUserFilter.addItem(u);
                }
            }
            if (selected != null) {
                cmbUserFilter.setSelectedItem(selected);
            }
        }

        applyFilter();
    }

    private void applyFilter() {
        if (tableModel == null || logService == null) return;
        tableModel.setRowCount(0);

        String q = (txtSearch != null) ? txtSearch.getText().trim().toLowerCase() : "";
        String user = (cmbUserFilter != null && cmbUserFilter.getSelectedItem() != null) ?
                cmbUserFilter.getSelectedItem().toString() : "All Users";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ActivityLogEntry l : logService.getAllLogs()) {
            boolean userMatches = "All Users".equalsIgnoreCase(user) || user.equalsIgnoreCase(l.getUsername());
            boolean textMatches = q.isEmpty() ||
                    l.getAction().toLowerCase().contains(q) ||
                    l.getDetails().toLowerCase().contains(q) ||
                    l.getRecordId().toLowerCase().contains(q);

            if (userMatches && textMatches) {
                tableModel.addRow(new Object[]{
                        l.getLogId(),
                        l.getTimestamp().format(dtf),
                        l.getUsername(),
                        l.getAction(),
                        l.getRecordId(),
                        l.getDetails()
                });
            }
        }
    }

    private void exportToCsv() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No logs available to export.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Activity_Logs_" + DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()) + ".csv"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.UTF_8))) {
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    pw.print("\"" + tableModel.getColumnName(c) + "\"" + (c < tableModel.getColumnCount() - 1 ? "," : "\n"));
                }
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        Object val = tableModel.getValueAt(r, c);
                        pw.print("\"" + (val != null ? val.toString().replace("\"", "\"\"") : "") + "\"" + (c < tableModel.getColumnCount() - 1 ? "," : "\n"));
                    }
                }
                JOptionPane.showMessageDialog(this, "Logs exported successfully to:\n" + dest.getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
