package shelflife.ui;

import shelflife.model.Role;
import shelflife.model.User;
import shelflife.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Clean, professional desktop dialog for registering new user accounts.
 * Enables creating Administrator or Cashier credentials with validation.
 */
public class SignUpDialog extends JDialog {

    private final AuthService authService;
    private User createdUser = null;

    private JTextField txtUsername;
    private JTextField txtFullName;
    private JComboBox<Role> cmbRole;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel lblError;
    private JButton btnSignUp;
    private JButton btnCancel;

    public SignUpDialog(Dialog owner, AuthService authService) {
        super(owner, "ShelfLife - Register New Account", true);
        this.authService = authService;

        initComponents();
        setSize(520, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public SignUpDialog(Frame owner, AuthService authService) {
        super(owner, "ShelfLife - Register New Account", true);
        this.authService = authService;

        initComponents();
        setSize(520, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG_PAGE);

        // Header Banner
        JPanel header = new JPanel(new BorderLayout(10, 4));
        header.setBackground(UITheme.HEADER_BG);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel("Create System Account");
        lblTitle.setFont(UITheme.FONT_APP_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Register an operator profile for POS billing or administration");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(new Color(148, 163, 184));

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Form Body
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(16, 28, 12, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Row 0: Username
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_MAIN);
        form.add(lblUser, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtUsername = new JTextField(16);
        UITheme.styleTextField(txtUsername);
        txtUsername.setToolTipText("Enter a unique login username (min 3 characters)");
        form.add(txtUsername, gbc);

        // Row 1: Full Name
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblName = new JLabel("Full Name:");
        lblName.setFont(UITheme.FONT_BOLD);
        lblName.setForeground(UITheme.TEXT_MAIN);
        form.add(lblName, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtFullName = new JTextField(16);
        UITheme.styleTextField(txtFullName);
        txtFullName.setToolTipText("Enter operator's display name");
        form.add(txtFullName, gbc);

        // Row 2: Role
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblRole = new JLabel("Role / Access:");
        lblRole.setFont(UITheme.FONT_BOLD);
        lblRole.setForeground(UITheme.TEXT_MAIN);
        form.add(lblRole, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        cmbRole = new JComboBox<>(Role.values());
        cmbRole.setFont(UITheme.FONT_REGULAR);
        cmbRole.setSelectedItem(Role.CASHIER); // default to Cashier
        form.add(cmbRole, gbc);

        // Row 3: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_MAIN);
        form.add(lblPass, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtPassword = new JPasswordField(16);
        UITheme.styleTextField(txtPassword);
        txtPassword.setToolTipText("Enter password (min 4 characters)");
        form.add(txtPassword, gbc);

        // Row 4: Confirm Password
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lblConfirm = new JLabel("Confirm Password:");
        lblConfirm.setFont(UITheme.FONT_BOLD);
        lblConfirm.setForeground(UITheme.TEXT_MAIN);
        form.add(lblConfirm, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtConfirmPassword = new JPasswordField(16);
        UITheme.styleTextField(txtConfirmPassword);
        txtConfirmPassword.setToolTipText("Re-type password to verify");
        form.add(txtConfirmPassword, gbc);

        // Row 5: Error / Feedback Label
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        lblError = new JLabel(" ");
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.DANGER);
        form.add(lblError, gbc);

        add(form, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UITheme.BG_PAGE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> {
            createdUser = null;
            dispose();
        });

        btnSignUp = UITheme.createSuccessButton("Create Account");
        btnSignUp.addActionListener(e -> handleSignUp());

        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSignUp();
                }
            }
        };
        txtUsername.addKeyListener(enterListener);
        txtFullName.addKeyListener(enterListener);
        txtPassword.addKeyListener(enterListener);
        txtConfirmPassword.addKeyListener(enterListener);

        footer.add(btnCancel);
        footer.add(btnSignUp);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleSignUp() {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        Role role = (Role) cmbRole.getSelectedItem();
        String pass = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());

        if (username.isEmpty()) {
            lblError.setText("Please choose a username.");
            txtUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            lblError.setText("Username must be at least 3 characters.");
            txtUsername.requestFocus();
            return;
        }

        if (fullName.isEmpty()) {
            fullName = username;
        }

        if (pass.isEmpty()) {
            lblError.setText("Please enter a password.");
            txtPassword.requestFocus();
            return;
        }

        if (pass.length() < 4) {
            lblError.setText("Password must be at least 4 characters.");
            txtPassword.requestFocus();
            return;
        }

        if (!pass.equals(confirm)) {
            lblError.setText("Passwords do not match. Please re-enter.");
            txtConfirmPassword.selectAll();
            txtConfirmPassword.requestFocus();
            return;
        }

        try {
            String uid = "USER-" + (System.currentTimeMillis() % 100000);
            User newUser = new User(uid, username, "", "", role, fullName, true, false);

            authService.createUser(newUser, pass);
            this.createdUser = newUser;

            JOptionPane.showMessageDialog(this,
                    "Account '" + username + "' (" + role.getDisplayName() + ") created successfully!\nYou can now sign in.",
                    "Registration Successful",
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();
        } catch (Exception ex) {
            lblError.setText(ex.getMessage());
        }
    }

    public User getCreatedUser() {
        return createdUser;
    }
}
