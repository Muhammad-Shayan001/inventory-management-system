package shelflife.ui;

import shelflife.model.AppSettings;
import shelflife.model.Role;
import shelflife.model.User;
import shelflife.service.AuthService;
import shelflife.service.SettingsService;
import shelflife.storage.DataStore;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Administrative panel for configuring system settings, user credentials,
 * and performing database backups and restores.
 */
public class SettingsAndBackupPanel extends JPanel implements Refreshable {

    private final SettingsService settingsService;
    private final AuthService authService;
    private final DataStore dataStore;

    // Settings fields
    private JTextField txtShopName;
    private JTextField txtCurrencyCode;
    private JTextField txtCurrencySymbol;
    private JSpinner spnWarningDays;
    private JSpinner spnLowStockThreshold;
    private JTextField txtTaxRate;

    // User management fields
    private JTextField txtNewUsername;
    private JPasswordField txtNewPassword;
    private JTextField txtNewFullName;
    private JComboBox<Role> cmbNewRole;

    public SettingsAndBackupPanel(SettingsService settingsService, AuthService authService, DataStore dataStore) {
        this.settingsService = settingsService;
        this.authService = authService;
        this.dataStore = dataStore;

        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_PAGE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        initComponents();
        refreshData();
    }

    private void initComponents() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // Left Column: System Settings Form
        splitPane.setLeftComponent(createSettingsSection());

        // Right Column: Backup/Restore & User Management
        JPanel rightCol = new JPanel(new GridLayout(2, 1, 0, 16));
        rightCol.setOpaque(false);
        rightCol.add(createBackupRestoreSection());
        rightCol.add(createUserManagementSection());
        splitPane.setRightComponent(rightCol);

        JScrollPane scrollPane = shelflife.ui.theme.Styles.createScrollPane(splitPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSettingsSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER),
                        "Shop & Operational Configuration",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        UITheme.FONT_SECTION,
                        UITheme.TEXT_MAIN
                ),
                new EmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Shop Name
        gbc.gridx = 0; gbc.gridy = row++;
        JLabel lblShop = new JLabel("Shop / Store Name:");
        lblShop.setFont(UITheme.FONT_REGULAR);
        panel.add(lblShop, gbc);
        gbc.gridy = row++;
        txtShopName = new JTextField();
        UITheme.styleTextField(txtShopName);
        panel.add(txtShopName, gbc);

        // Currency
        gbc.gridy = row++;
        JLabel lblCurrCode = new JLabel("Currency Code:");
        lblCurrCode.setFont(UITheme.FONT_REGULAR);
        panel.add(lblCurrCode, gbc);
        gbc.gridy = row++;
        txtCurrencyCode = new JTextField("PKR");
        UITheme.styleTextField(txtCurrencyCode);
        panel.add(txtCurrencyCode, gbc);

        gbc.gridy = row++;
        JLabel lblCurrSym = new JLabel("Currency Symbol:");
        lblCurrSym.setFont(UITheme.FONT_REGULAR);
        panel.add(lblCurrSym, gbc);
        gbc.gridy = row++;
        txtCurrencySymbol = new JTextField("Rs. ");
        UITheme.styleTextField(txtCurrencySymbol);
        panel.add(txtCurrencySymbol, gbc);

        // Expiry Warning Horizon
        gbc.gridy = row++;
        JLabel lblExpHor = new JLabel("Expiry Alert Horizon (Days):");
        lblExpHor.setFont(UITheme.FONT_REGULAR);
        panel.add(lblExpHor, gbc);
        gbc.gridy = row++;
        spnWarningDays = new JSpinner(new SpinnerNumberModel(14, 1, 180, 1));
        spnWarningDays.setFont(UITheme.FONT_REGULAR);
        panel.add(spnWarningDays, gbc);

        // Default Low Stock Threshold
        gbc.gridy = row++;
        JLabel lblThresh = new JLabel("Default Low Stock Reorder Threshold:");
        lblThresh.setFont(UITheme.FONT_REGULAR);
        panel.add(lblThresh, gbc);
        gbc.gridy = row++;
        spnLowStockThreshold = new JSpinner(new SpinnerNumberModel(10, 0, 500, 1));
        spnLowStockThreshold.setFont(UITheme.FONT_REGULAR);
        panel.add(spnLowStockThreshold, gbc);

        // Sales Tax Rate
        gbc.gridy = row++;
        JLabel lblTax = new JLabel("Sales Tax Rate (%):");
        lblTax.setFont(UITheme.FONT_REGULAR);
        panel.add(lblTax, gbc);
        gbc.gridy = row++;
        txtTaxRate = new JTextField("10.00");
        UITheme.styleTextField(txtTaxRate);
        panel.add(txtTaxRate, gbc);

        // Save Settings Button
        gbc.gridy = row++;
        gbc.insets = new Insets(14, 6, 6, 6);
        JButton btnSaveSettings = UITheme.createPrimaryButton("Save Configuration Changes");
        btnSaveSettings.addActionListener(e -> saveSettings());
        panel.add(btnSaveSettings, gbc);

        // Filler
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createBackupRestoreSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER),
                        "Database Backup & Disaster Recovery",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        UITheme.FONT_SECTION,
                        UITheme.TEXT_MAIN
                ),
                new EmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel lblDesc = new JLabel("<html><font color='#475569'>Create snapshot archives of all CSV records, or restore from an existing backup folder with automatic safety snapshotting.</font></html>");
        lblDesc.setFont(UITheme.FONT_SMALL);
        gbc.gridy = 0;
        panel.add(lblDesc, gbc);

        JButton btnCreateBackup = UITheme.createPrimaryButton("Create Backup Snapshot Now");
        btnCreateBackup.addActionListener(e -> handleCreateBackup());
        gbc.gridy = 1;
        panel.add(btnCreateBackup, gbc);

        JButton btnRestoreBackup = UITheme.createSecondaryButton("Restore Database from Backup Folder...");
        btnRestoreBackup.addActionListener(e -> handleRestoreBackup());
        gbc.gridy = 2;
        panel.add(btnRestoreBackup, gbc);

        return panel;
    }

    private JPanel createUserManagementSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.CARD_BG);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER),
                        "User Account Provisioning",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        UITheme.FONT_SECTION,
                        UITheme.TEXT_MAIN
                ),
                new EmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblU = new JLabel("Username:");
        lblU.setFont(UITheme.FONT_REGULAR);
        panel.add(lblU, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        txtNewUsername = new JTextField(12);
        UITheme.styleTextField(txtNewUsername);
        panel.add(txtNewUsername, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblF = new JLabel("Full Name:");
        lblF.setFont(UITheme.FONT_REGULAR);
        panel.add(lblF, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        txtNewFullName = new JTextField(12);
        UITheme.styleTextField(txtNewFullName);
        panel.add(txtNewFullName, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblP = new JLabel("Password:");
        lblP.setFont(UITheme.FONT_REGULAR);
        panel.add(lblP, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        txtNewPassword = new JPasswordField(12);
        UITheme.styleTextField(txtNewPassword);
        panel.add(txtNewPassword, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblR = new JLabel("System Role:");
        lblR.setFont(UITheme.FONT_REGULAR);
        panel.add(lblR, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        cmbNewRole = new JComboBox<>(Role.values());
        cmbNewRole.setFont(UITheme.FONT_REGULAR);
        panel.add(cmbNewRole, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2; gbc.weightx = 1.0;
        JButton btnCreateUser = UITheme.createPrimaryButton("Create User Account");
        btnCreateUser.addActionListener(e -> handleCreateUser());
        panel.add(btnCreateUser, gbc);

        return panel;
    }

    private void saveSettings() {
        try {
            String shop = txtShopName.getText().trim();
            String code = txtCurrencyCode.getText().trim();
            String sym = txtCurrencySymbol.getText();
            int warn = (int) spnWarningDays.getValue();
            int thresh = (int) spnLowStockThreshold.getValue();
            BigDecimal tax = new BigDecimal(txtTaxRate.getText().trim());

            AppSettings s = new AppSettings(shop, code, sym, warn, thresh, tax);
            settingsService.updateSettings(s);
            JOptionPane.showMessageDialog(this, "Application settings saved successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCreateBackup() {
        try {
            File backupDir = dataStore.createBackup(null);
            JOptionPane.showMessageDialog(this,
                    "Backup snapshot created successfully at:\n" + backupDir.getAbsolutePath(),
                    "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Backup Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRestoreBackup() {
        JFileChooser fc = new JFileChooser(dataStore.getDataDirectory().getParentFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select ShelfLife Backup Directory to Restore");
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File sel = fc.getSelectedFile();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "WARNING: Restoring will overwrite all current data with records from:\n" + sel.getName() +
                    "\nA safety backup will be created automatically before restore proceeds.\nContinue?",
                    "Confirm Database Restore",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    dataStore.restoreBackup(sel);
                    JOptionPane.showMessageDialog(this,
                            "Database restore completed successfully!\nPlease restart or click File -> Reload Database to sync all modules.",
                            "Restore Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Restore failed: " + ex.getMessage(), "Restore Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void handleCreateUser() {
        String username = txtNewUsername.getText().trim();
        String fullName = txtNewFullName.getText().trim();
        String pass = new String(txtNewPassword.getPassword());
        Role role = (Role) cmbNewRole.getSelectedItem();

        if (username.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String uid = "USER-" + System.currentTimeMillis() % 10000;
        User user = new User(uid, username, "", "", role, fullName, true, false);

        try {
            authService.createUser(user, pass);
            JOptionPane.showMessageDialog(this, "User account '" + username + "' created successfully.", "User Created", JOptionPane.INFORMATION_MESSAGE);
            txtNewUsername.setText("");
            txtNewFullName.setText("");
            txtNewPassword.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to create user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshData() {
        if (settingsService != null) {
            settingsService.reload();
            AppSettings s = settingsService.getSettings();
            if (s != null) {
                txtShopName.setText(s.getShopName());
                txtCurrencyCode.setText(s.getCurrencyCode());
                txtCurrencySymbol.setText(s.getCurrencySymbol());
                spnWarningDays.setValue(s.getExpiryWarningDays());
                spnLowStockThreshold.setValue(s.getDefaultLowStockThreshold());
                txtTaxRate.setText(s.getTaxRatePercent().toPlainString());
            }
        }
    }
}
