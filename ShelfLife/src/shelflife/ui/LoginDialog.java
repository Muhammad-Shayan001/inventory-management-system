package shelflife.ui;

import shelflife.model.User;
import shelflife.service.AuthService;
import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Professional desktop login modal for authentication and role-based access control.
 */
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private User authenticatedUser = null;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JButton btnLogin;

    public LoginDialog(Frame owner, AuthService authService) {
        super(owner, "ShelfLife - System Authentication", true);
        this.authService = authService;

        initComponents();
        setSize(460, 440);
        setResizable(false);
        setLocationRelativeTo(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    txtUsername.requestFocusInWindow();
                    txtUsername.selectAll();
                });
            }
        });
    }

    private void initComponents() {
        getContentPane().setBackground(Theme.BG_WINDOW);
        setLayout(new BorderLayout(0, 0));

        // 1. Top Section (Centered)
        JPanel pnlTop = new JPanel();
        pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.Y_AXIS));
        pnlTop.setBackground(Theme.BG_WINDOW);
        pnlTop.setBorder(new EmptyBorder(22, 30, 8, 30));

        JLabel lblTitle = new JLabel("ShelfLife");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(Theme.ACCENT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Inventory & POS Management");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitle.setForeground(Theme.TEXT_SECONDARY);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlTop.add(lblTitle);
        pnlTop.add(Box.createVerticalStrut(4));
        pnlTop.add(lblSubtitle);

        add(pnlTop, BorderLayout.NORTH);

        // 2. Center Form (30px horizontal padding)
        JPanel pnlForm = new JPanel();
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));
        pnlForm.setBackground(Theme.BG_WINDOW);
        pnlForm.setBorder(new EmptyBorder(8, 30, 10, 30));

        // Username
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(Theme.FONT_BOLD);
        lblUser.setForeground(Theme.TEXT_PRIMARY);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsername = new JTextField("admin");
        Styles.styleTextField(txtUsername);
        txtUsername.setPreferredSize(new Dimension(380, 36));
        txtUsername.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(Theme.FONT_BOLD);
        lblPass.setForeground(Theme.TEXT_PRIMARY);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new JPasswordField("admin123");
        Styles.styleTextField(txtPassword);
        txtPassword.setPreferredSize(new Dimension(380, 36));
        txtPassword.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Error message
        lblError = new JLabel(" ");
        lblError.setFont(Theme.FONT_SMALL);
        lblError.setForeground(Theme.COLOR_DANGER_FG);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Login Button (full width, 40px height, Theme.BG_SIDEBAR_SELECTED, white bold text)
        btnLogin = new JButton("Sign In") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? Theme.ACCENT_PRIMARY_PRESSED :
                        (getModel().isRollover() ? Theme.ACCENT_PRIMARY_HOVER : Theme.BG_SIDEBAR_SELECTED));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(380, 40));
        btnLogin.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> handleLogin());

        // Key listener for Enter key
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        txtUsername.addKeyListener(enterListener);
        txtPassword.addKeyListener(enterListener);

        pnlForm.add(lblUser);
        pnlForm.add(Box.createVerticalStrut(4));
        pnlForm.add(txtUsername);
        pnlForm.add(Box.createVerticalStrut(10));
        pnlForm.add(lblPass);
        pnlForm.add(Box.createVerticalStrut(4));
        pnlForm.add(txtPassword);
        pnlForm.add(Box.createVerticalStrut(4));
        pnlForm.add(lblError);
        pnlForm.add(Box.createVerticalStrut(8));
        pnlForm.add(btnLogin);

        add(pnlForm, BorderLayout.CENTER);

        // 3. Bottom Hint & Demo Credentials Panel
        JPanel pnlBottom = new JPanel();
        pnlBottom.setLayout(new BoxLayout(pnlBottom, BoxLayout.Y_AXIS));
        pnlBottom.setBackground(Theme.BG_WINDOW);
        pnlBottom.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_LIGHT),
                new EmptyBorder(10, 20, 12, 20)
        ));

        JPanel pnlHint = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        pnlHint.setOpaque(false);

        JLabel lblHint = new JLabel("Demo Accounts:");
        lblHint.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblHint.setForeground(Theme.TEXT_SECONDARY);
        pnlHint.add(lblHint);

        JButton btnAdminChip = createChipButton("admin / admin123", () -> {
            txtUsername.setText("admin");
            txtPassword.setText("admin123");
            lblError.setText(" ");
        });
        JButton btnCashierChip = createChipButton("cashier / cashier123", () -> {
            txtUsername.setText("cashier");
            txtPassword.setText("cashier123");
            lblError.setText(" ");
        });

        pnlHint.add(btnAdminChip);
        pnlHint.add(btnCashierChip);

        JPanel pnlSubAction = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 2));
        pnlSubAction.setOpaque(false);

        JButton btnRegister = new JButton("Register New Account");
        btnRegister.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRegister.setForeground(Theme.ACCENT_PRIMARY);
        btnRegister.setBorderPainted(false);
        btnRegister.setContentAreaFilled(false);
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.addActionListener(e -> openSignUpDialog());

        pnlSubAction.add(btnRegister);

        pnlBottom.add(pnlHint);
        pnlBottom.add(Box.createVerticalStrut(2));
        pnlBottom.add(pnlSubAction);

        add(pnlBottom, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);
    }

    private JButton createChipButton(String text, Runnable onClick) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private void openSignUpDialog() {
        SignUpDialog signUpDialog = new SignUpDialog(this, authService);
        signUpDialog.setVisible(true);

        User created = signUpDialog.getCreatedUser();
        if (created != null) {
            txtUsername.setText(created.getUsername());
            txtPassword.setText("");
            lblError.setForeground(Theme.COLOR_SUCCESS_FG);
            lblError.setText("Account '" + created.getUsername() + "' registered! Please sign in.");
            txtPassword.requestFocus();
        }
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            lblError.setForeground(Theme.COLOR_DANGER_FG);
            lblError.setText("Please enter a username.");
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            lblError.setForeground(Theme.COLOR_DANGER_FG);
            lblError.setText("Please enter a password.");
            txtPassword.requestFocus();
            return;
        }

        try {
            User user = authService.login(username, password);
            // If user must change password on first login, prompt before granting access
            if (user.isMustChangePassword()) {
                boolean changed = ChangePasswordDialog.requireChange(this, authService, user);
                if (!changed) {
                    lblError.setForeground(Theme.COLOR_DANGER_FG);
                    lblError.setText("You must change your password to continue.");
                    return;
                }
            }
            this.authenticatedUser = user;
            dispose();
        } catch (Exception ex) {
            lblError.setForeground(Theme.COLOR_DANGER_FG);
            lblError.setText(ex.getMessage());
            txtPassword.selectAll();
            txtPassword.requestFocus();
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
