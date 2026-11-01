package shelflife.ui;

import shelflife.model.Role;
import shelflife.model.User;
import shelflife.service.AuthService;
import shelflife.ui.theme.Theme;
import shelflife.util.PasswordUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Admin User Management Dialog.
 * Allows adding system users, toggling active status, and resetting credentials.
 * Strictly prevents deactivating the last remaining active Administrator.
 */
public class UserManagementDialog extends JDialog {

    private final AuthService authService;
    private final DefaultTableModel userModel;
    private final JTable tblUsers;

    public UserManagementDialog(Frame parent, AuthService authService) {
        super(parent, "System User & Role Management", true);
        this.authService = authService;

        setSize(720, 460);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(14, 18, 10, 18));
        JLabel lblTitle = new JLabel("System User Directory & Roles");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel lblSub = new JLabel("Create cashiers and administrators, toggle active status, and reset passwords.");
        lblSub.setFont(Theme.FONT_SUBTITLE);
        lblSub.setForeground(Theme.TEXT_SECONDARY);
        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"User ID", "Username", "Full Name", "Role", "Active Status"};
        userModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblUsers = new JTable(userModel);
        tblUsers.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        tblUsers.setFont(Theme.FONT_TABLE_CELL);
        tblUsers.getTableHeader().setFont(Theme.FONT_TABLE_HEADER);

        JScrollPane scrollPane = new JScrollPane(tblUsers);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnAdd = new JButton("+ Add User");
        btnAdd.setFont(Theme.FONT_BOLD);
        btnAdd.setBackground(Theme.ACCENT_PRIMARY);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> showAddUserDialog());

        JButton btnToggle = new JButton("Toggle Active/Disabled");
        btnToggle.setFont(Theme.FONT_REGULAR);
        btnToggle.addActionListener(e -> toggleActiveStatus());

        JButton btnResetPwd = new JButton("Reset Password");
        btnResetPwd.setFont(Theme.FONT_REGULAR);
        btnResetPwd.addActionListener(e -> resetPassword());

        JButton btnClose = new JButton("Close");
        btnClose.setFont(Theme.FONT_REGULAR);
        btnClose.addActionListener(e -> dispose());

        bottomBar.add(btnAdd);
        bottomBar.add(btnToggle);
        bottomBar.add(btnResetPwd);
        bottomBar.add(btnClose);
        add(bottomBar, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        userModel.setRowCount(0);
        List<User> list = authService.getAllUsers();
        for (User u : list) {
            userModel.addRow(new Object[]{
                    u.getUserId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getRole().getDisplayName(),
                    u.isActive() ? "Active" : "Disabled"
            });
        }
    }

    private void showAddUserDialog() {
        JTextField txtUid = new JTextField("USER-" + (int)(Math.random() * 9000 + 1000));
        JTextField txtUsername = new JTextField();
        JTextField txtFullName = new JTextField();
        JComboBox<Role> cmbRole = new JComboBox<>(Role.values());
        JPasswordField txtPwd = new JPasswordField();

        Object[] fields = {
                "User ID:", txtUid,
                "Username:", txtUsername,
                "Full Name:", txtFullName,
                "Role:", cmbRole,
                "Password:", txtPwd
        };

        int res = JOptionPane.showConfirmDialog(this, fields, "Create New User Account", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String u = txtUsername.getText().trim();
            String p = new String(txtPwd.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String salt = PasswordUtil.generateSalt();
                String hash = PasswordUtil.hashPassword(p, salt);
                User newUser = new User(txtUid.getText().trim(), u, hash, salt, (Role) cmbRole.getSelectedItem(), txtFullName.getText().trim(), true, false);
                authService.registerUser(newUser);
                ToastNotification.show(this, "User '" + u + "' created successfully.", ToastNotification.Type.SUCCESS);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to create user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleActiveStatus() {
        int row = tblUsers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to toggle.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) userModel.getValueAt(row, 1);
        User target = authService.getUserByUsername(username);
        if (target == null) return;

        // Last Admin protection
        if (target.getRole() == Role.ADMIN && target.isActive()) {
            int activeAdminCount = 0;
            for (User u : authService.getAllUsers()) {
                if (u.getRole() == Role.ADMIN && u.isActive()) {
                    activeAdminCount++;
                }
            }
            if (activeAdminCount <= 1) {
                JOptionPane.showMessageDialog(this,
                        "Action Denied: Cannot deactivate the last remaining active Administrator in the system.",
                        "Security Safeguard",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            target.setActive(!target.isActive());
            authService.updateUser(target);
            ToastNotification.show(this, "User '" + target.getUsername() + "' status updated to " + (target.isActive() ? "Active" : "Disabled"), ToastNotification.Type.INFO);
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetPassword() {
        int row = tblUsers.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to reset password.", "None Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) userModel.getValueAt(row, 1);
        User target = authService.getUserByUsername(username);
        if (target == null) return;

        JPasswordField pwdField = new JPasswordField();
        int res = JOptionPane.showConfirmDialog(this, new Object[]{"Enter new password for " + username + ":", pwdField}, "Reset Password", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String p = new String(pwdField.getPassword());
            if (p.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Password cannot be empty.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                String salt = PasswordUtil.generateSalt();
                String hash = PasswordUtil.hashPassword(p, salt);
                target.setSalt(salt);
                target.setPasswordHash(hash);
                authService.updateUser(target);
                ToastNotification.show(this, "Password reset successfully for " + username, ToastNotification.Type.SUCCESS);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to reset password: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
