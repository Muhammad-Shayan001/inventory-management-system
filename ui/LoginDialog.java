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
        setSize(580, 420);
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header Banner
        JPanel header = new JPanel(new BorderLayout(10, 4));
        header.setBackground(UITheme.HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel lblTitle = new JLabel("ShelfLife Console");
        lblTitle.setFont(UITheme.FONT_APP_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Inventory & Point-of-Sale Access");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(UITheme.TEXT_LIGHT);

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Center Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_PAGE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 15, 28));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Username
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(UITheme.TEXT_MAIN);
        form.add(lblUser, gbc);

        gbc.gridx = 1;
        txtUsername = new JTextField(15);
        UITheme.styleTextField(txtUsername);
        txtUsername.setText("admin"); // convenient default
        form.add(txtUsername, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setForeground(UITheme.TEXT_MAIN);
        form.add(lblPass, gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        UITheme.styleTextField(txtPassword);
        txtPassword.setText("admin123");
        form.add(txtPassword, gbc);

        // Error message label
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        lblError = new JLabel(" ");
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.DANGER);
        form.add(lblError, gbc);

        // Quick login shortcut buttons panel
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel pnlQuickFill = new JPanel(new GridLayout(1, 3, 8, 0));
        pnlQuickFill.setOpaque(false);
        JLabel lblDemo = new JLabel("Demo Accounts:");
        lblDemo.setFont(UITheme.FONT_SMALL);
        lblDemo.setForeground(UITheme.TEXT_MUTED);
        pnlQuickFill.add(lblDemo);

        JButton btnFillAdmin = Styles.createSecondaryButton("Admin");
        btnFillAdmin.setFont(Theme.FONT_SMALL);
        btnFillAdmin.addActionListener(e -> {
            txtUsername.setText("admin");
            txtPassword.setText("admin123");
            lblError.setText(" ");
        });

        JButton btnFillCashier = Styles.createSecondaryButton("Cashier");
        btnFillCashier.setFont(Theme.FONT_SMALL);
        btnFillCashier.addActionListener(e -> {
            txtUsername.setText("cashier");
            txtPassword.setText("cashier123");
            lblError.setText(" ");
        });

        pnlQuickFill.add(btnFillAdmin);
        pnlQuickFill.add(btnFillCashier);
        form.add(pnlQuickFill, gbc);

        add(form, BorderLayout.CENTER);

        // Bottom Actions - Split Left (Sign Up) and Right (Exit / Sign In)
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBackground(UITheme.BG_PAGE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JButton btnSignUp = Styles.createSecondaryButton("Register New Account");
        btnSignUp.setToolTipText("Create a new Cashier or Administrator profile");
        btnSignUp.addActionListener(e -> openSignUpDialog());

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton btnExit = Styles.createSecondaryButton("Exit Application");
        btnExit.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
        });

        btnLogin = Styles.createPrimaryButton("Sign In to Console");
        btnLogin.addActionListener(e -> handleLogin());

        // Enter key listener on inputs
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

        rightActions.add(btnExit);
        rightActions.add(btnLogin);

        footer.add(btnSignUp, BorderLayout.WEST);
        footer.add(rightActions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private void openSignUpDialog() {
        SignUpDialog signUpDialog = new SignUpDialog(this, authService);
        signUpDialog.setVisible(true);

        User created = signUpDialog.getCreatedUser();
        if (created != null) {
            txtUsername.setText(created.getUsername());
            txtPassword.setText("");
            lblError.setForeground(UITheme.SUCCESS);
            lblError.setText("Account '" + created.getUsername() + "' registered! Please sign in.");
            txtPassword.requestFocus();
        }
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            lblError.setText("Please enter a username.");
            txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            lblError.setText("Please enter a password.");
            txtPassword.requestFocus();
            return;
        }

        try {
            User user = authService.login(username, password);
            this.authenticatedUser = user;
            dispose();
        } catch (Exception ex) {
            lblError.setText(ex.getMessage());
            txtPassword.selectAll();
            txtPassword.requestFocus();
        }
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
