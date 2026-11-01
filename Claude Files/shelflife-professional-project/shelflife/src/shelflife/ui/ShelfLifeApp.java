package shelflife.ui;

import shelflife.model.Role;
import shelflife.model.User;
import shelflife.service.*;
import shelflife.storage.*;
import shelflife.ui.billing.BillingPanel;
import shelflife.ui.billing.ReturnsPanel;
import shelflife.ui.expiry.ExpiryAlertsPanel;
import shelflife.ui.inventory.ProductInventoryPanel;
import shelflife.ui.inventory.SuppliersPanel;
import shelflife.ui.reports.ActivityLogPanel;
import shelflife.ui.reports.DashboardReportsPanel;
import shelflife.ui.reports.ReorderListPanel;
import shelflife.util.SeedDataUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window for ShelfLife Inventory, Expiry & Billing Management System.
 * Integrates shared storage and services across Inventory, Expiry Alerts, Point-of-Sale,
 * Returns, Suppliers, Activity Logs, and Reporting modules using role-based CardLayout navigation.
 */
public class ShelfLifeApp extends JFrame {

    private final DataStore dataStore;
    private final InventoryService inventoryService;
    private final ExpiryService expiryService;
    private final SalesService salesService;
    private final ReportService reportService;
    private final AuthService authService;
    private final SupplierService supplierService;
    private final ReturnsService returnsService;
    private final ActivityLogService logService;
    private final SettingsService settingsService;

    // Advanced Commercial & Financial Services
    private final CapitalService capitalService;
    private final ExpenseService expenseService;
    private final CashAccountService cashAccountService;
    private final CustomerCreditService customerCreditService;
    private final SupplierPayableService supplierPayableService;
    private final PurchaseService purchaseService;
    private final FinancialService financialService;
    private final HeldBillService heldBillService;
    private final CashierShiftService cashierShiftService;

    // Navigation and Cards
    private CardLayout cardLayout;
    private static final java.util.Set<String> CASHIER_CARDS =
            new java.util.HashSet<>(java.util.Arrays.asList("billing", "returns", "inventory"));

    private JPanel cardPanel;
    private JPanel sidebarNavList;

    // Current user session
    private User user;

    private ProductInventoryPanel inventoryPanel;
    private SuppliersPanel suppliersPanel;
    private ExpiryAlertsPanel expiryPanel;
    private BillingPanel billingPanel;
    private ReturnsPanel returnsPanel;
    private DashboardReportsPanel reportsPanel;
    private shelflife.ui.reports.business.BusinessOverviewPanel overviewPanel;
    private ReorderListPanel reorderPanel;
    private ActivityLogPanel activityLogPanel;
    private SettingsAndBackupPanel settingsAndBackupPanel;

    // Role-restricted Menu items
    private JMenuItem menuItemBackup;
    private JMenuItem menuItemSup;
    private JMenuItem menuItemExp;
    private JMenuItem menuItemRep;
    private JMenuItem menuItemReord;
    private JMenuItem menuItemLogs;
    private JMenuItem menuItemSet;

    // Top Header Components
    private JLabel lblStoreTitle;
    private JLabel lblUserBadge;

    // Sidebar navigation buttons
    private final Map<String, JButton> navButtons = new HashMap<>();

    // Status bar
    private JLabel lblStatusMessage;
    private JLabel lblStatusDate;
    private JLabel lblStatusStorage;
    private JLabel lblStatusUser;

    private String currentCard = "inventory";

    public ShelfLifeApp() {
        this(true);
    }

    public ShelfLifeApp(boolean promptLogin) {
        this(null, promptLogin);
    }

    public ShelfLifeApp(DataStore customDataStore, boolean promptLogin) {
        if (customDataStore != null) {
            this.dataStore = customDataStore;
        } else {
            File homeDataDir = new File(System.getProperty("user.home"), "ShelfLifeData");
            if (!homeDataDir.exists()) {
                homeDataDir.mkdirs();
            }
            bootstrapDataFiles(homeDataDir);
            this.dataStore = new CsvDataStore(homeDataDir);
        }

        // Initialize Shared Business Services with Single DataStore Reference
        this.authService = new AuthServiceImpl(dataStore);
        this.logService = new ActivityLogServiceImpl(dataStore, authService);
        this.settingsService = new SettingsServiceImpl(dataStore);
        this.inventoryService = new InventoryServiceImpl(dataStore);
        this.expiryService = new ExpiryServiceImpl(inventoryService, dataStore);
        this.salesService = new SalesServiceImpl(inventoryService, dataStore);
        this.supplierService = new SupplierServiceImpl(inventoryService, dataStore);
        this.returnsService = new ReturnsServiceImpl(inventoryService, salesService, expiryService, dataStore);
        this.reportService = new ReportServiceImpl(inventoryService, salesService, dataStore);

        // Commercial Financial Services
        this.capitalService = new CapitalServiceImpl(dataStore);
        this.expenseService = new ExpenseServiceImpl(dataStore);
        this.cashAccountService = new CashAccountServiceImpl(dataStore);
        this.customerCreditService = new CustomerCreditServiceImpl(dataStore);
        this.supplierPayableService = new SupplierPayableServiceImpl(dataStore);
        this.purchaseService = new PurchaseServiceImpl(dataStore, inventoryService, supplierPayableService, cashAccountService);
        this.financialService = new FinancialServiceImpl(dataStore, inventoryService, salesService, capitalService, expenseService, cashAccountService, customerCreditService, supplierPayableService, purchaseService);
        this.heldBillService = new HeldBillServiceImpl(dataStore);
        this.cashierShiftService = new CashierShiftServiceImpl(dataStore);

        SeedDataUtil.seedIfEmpty(inventoryService, supplierService);

        initWindow();
        initPanels();

        if (promptLogin && !GraphicsEnvironment.isHeadless()) {
            promptLogin();
        } else {
            User admin = authService.getAllUsers().isEmpty() ? null : authService.getAllUsers().get(0);
            applyUserSession(admin);
        }
    }

    private void bootstrapDataFiles(File homeDir) {
        File localDir = new File("data");
        if (localDir.exists() && localDir.isDirectory()) {
            File[] files = localDir.listFiles((d, name) -> name.endsWith(".csv"));
            if (files != null) {
                for (File f : files) {
                    File dest = new File(homeDir, f.getName());
                    if (!dest.exists()) {
                        copyFile(f, dest);
                    }
                }
            }
        }
    }

    private void copyFile(File src, File dest) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception ignored) {}
    }

    private void promptLogin() {
        LoginDialog loginDialog = new LoginDialog(this, authService);
        loginDialog.setVisible(true);

        User user = loginDialog.getAuthenticatedUser();
        if (user == null) {
            System.exit(0);
        }

        if (user.isMustChangePassword() && !ChangePasswordDialog.requireChange(this, authService, user)) {
            System.exit(0);
        }

        applyUserSession(user);
    }

    private void applyUserSession(User user) {
        if (user == null) return;

        // Log session start
        logService.log(user.getUsername(), "LOGIN", "User signed in to desktop console", user.getUserId());

        // Update header & status labels
        lblUserBadge.setText(user.getFullName() + " (" + user.getRole().getDisplayName() + ")");
        lblStatusUser.setText("User: " + user.getUsername() + " [" + user.getRole().getDisplayName() + "]");
        lblStoreTitle.setText(settingsService.getSettings().getShopName());

        // Reconfigure sidebar buttons and menus based on Role
        rebuildSidebar(user.getRole());
        rebuildMenuBar(user.getRole());
        if (inventoryPanel != null) {
            inventoryPanel.applyRolePermissions(user.getRole());
        }

        // Show appropriate landing card
        if (user.getRole() == Role.CASHIER) {
            showCard("billing");
        } else {
            showCard("inventory");
        }
    }

    private void initWindow() {
        setTitle("ShelfLife — Inventory, Expiry & Billing Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Top Branded Header
        add(createTopBanner(), BorderLayout.NORTH);

        // Left Navigation Sidebar
        add(createSidebar(), BorderLayout.WEST);

        // Center Card Panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(new Color(245, 247, 250));
        add(cardPanel, BorderLayout.CENTER);

        // Bottom Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);

        // Menu Bar
        setJMenuBar(createAppMenuBar());
    }

    private JPanel createTopBanner() {
        JPanel banner = new JPanel(new BorderLayout(15, 0));
        banner.setBackground(UITheme.HEADER_BG);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel brandPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        brandPanel.setOpaque(false);

        lblStoreTitle = new JLabel("ShelfLife");
        lblStoreTitle.setFont(UITheme.FONT_APP_TITLE);
        lblStoreTitle.setForeground(Color.WHITE);

        JLabel lblTag = new JLabel("Inventory, Expiry & Point-of-Sale System");
        lblTag.setFont(UITheme.FONT_SUBTITLE);
        lblTag.setForeground(new Color(148, 163, 184));

        brandPanel.add(lblStoreTitle);
        brandPanel.add(lblTag);

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        rightInfo.setOpaque(false);

        lblUserBadge = new JLabel("User: Authenticating...");
        lblUserBadge.setFont(UITheme.FONT_BOLD);
        lblUserBadge.setForeground(new Color(226, 232, 240));
        lblUserBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));

        JButton btnSwitchUser = new JButton("Switch User / Logout");
        btnSwitchUser.setFont(UITheme.FONT_SMALL);
        btnSwitchUser.setBackground(UITheme.HEADER_BG);
        btnSwitchUser.setForeground(new Color(226, 232, 240));
        btnSwitchUser.setFocusPainted(false);
        btnSwitchUser.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSwitchUser.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));
        btnSwitchUser.addActionListener(e -> handleLogout());

        rightInfo.add(lblUserBadge);
        rightInfo.add(btnSwitchUser);

        banner.add(brandPanel, BorderLayout.WEST);
        banner.add(rightInfo, BorderLayout.EAST);

        return banner;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(shelflife.ui.theme.Theme.SIDEBAR_WIDTH, 0));
        sidebar.setBackground(shelflife.ui.theme.Theme.BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.decode("#CBD5E1")));

        // Top Sidebar Branding
        JPanel brandHeader = new JPanel(new BorderLayout(0, 2));
        brandHeader.setOpaque(false);
        brandHeader.setBorder(new EmptyBorder(16, 16, 12, 16));
        JLabel lblApp = new JLabel("ShelfLife");
        lblApp.setFont(shelflife.ui.theme.Theme.FONT_APP_TITLE);
        lblApp.setForeground(Color.BLACK);
        JLabel lblSub = new JLabel("POS & Inventory");
        lblSub.setFont(shelflife.ui.theme.Theme.FONT_HINT);
        lblSub.setForeground(Color.decode("#64748B"));
        brandHeader.add(lblApp, BorderLayout.NORTH);
        brandHeader.add(lblSub, BorderLayout.SOUTH);
        sidebar.add(brandHeader, BorderLayout.NORTH);

        sidebarNavList = new JPanel();
        sidebarNavList.setLayout(new BoxLayout(sidebarNavList, BoxLayout.Y_AXIS));
        sidebarNavList.setOpaque(false);
        sidebarNavList.setBorder(new EmptyBorder(6, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(sidebarNavList);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        sidebar.add(scrollPane, BorderLayout.CENTER);

        // Sidebar footer - shows user name, role, and Sign Out button
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 14, 6, 14));

        // User name and role
        JPanel userPanel = new JPanel(new BorderLayout(8, 0));
        userPanel.setOpaque(false);

        JLabel lblUserName = new JLabel(user != null ? user.getFullName() : "Signed in");
        lblUserName.setFont(shelflife.ui.theme.Theme.FONT_BOLD);
        lblUserName.setForeground(shelflife.ui.theme.Theme.FG_SIDEBAR_TEXT);
        lblUserName.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        JLabel lblUserRole = new JLabel(user != null ? user.getRole().getDisplayName() : "Role");
        lblUserRole.setFont(shelflife.ui.theme.Theme.FONT_SUBTITLE);
        lblUserRole.setForeground(shelflife.ui.theme.Theme.FG_SIDEBAR_TEXT);
        lblUserRole.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        userPanel.add(lblUserName, BorderLayout.NORTH);
        userPanel.add(lblUserRole, BorderLayout.SOUTH);

        // Sign Out button with standard FlatLaf styling
        JButton signOutBtn = new JButton("Sign Out");
        signOutBtn.setFont(shelflife.ui.theme.Theme.FONT_BOLD);
        signOutBtn.setForeground(shelflife.ui.theme.Theme.FG_SIDEBAR_TEXT);
        signOutBtn.setFocusPainted(false);
        signOutBtn.setContentAreaFilled(false);
        signOutBtn.setBorderPainted(false);
        signOutBtn.setOpaque(false);
        signOutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        signOutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        signOutBtn.setPreferredSize(new Dimension(200, 38));
        signOutBtn.setMinimumSize(new Dimension(200, 38));
        signOutBtn.setMaximumSize(new Dimension(32767, 38));

        signOutBtn.addActionListener(e -> handleLogout());

        JPanel btnContainer = new JPanel(new BorderLayout());
        btnContainer.setOpaque(false);
        btnContainer.add(signOutBtn, BorderLayout.CENTER);

        footer.add(userPanel, BorderLayout.NORTH);
        footer.add(btnContainer, BorderLayout.SOUTH);

        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private void rebuildSidebar(Role role) {
        sidebarNavList.removeAll();
        navButtons.clear();

        if (role == Role.ADMIN) {
            addNavButton("Inventory & Batches", "inventory");
            addNavButton("Stock Intake & Suppliers", "suppliers");
            addNavButton("Expiry Alerts & Wastage", "expiry");
            addNavButton("Billing & Sales POS", "billing");
            addNavButton("Returns & Refunds", "returns");
            addNavButton("Business Overview", "overview");
            addNavButton("Reports & Dashboard", "reports");
            addNavButton("Stock Reorder List", "reorder");
            addNavButton("Activity Audit Log", "logs");
            addNavButton("Settings & Backup", "settings");
        } else {
            // Cashier view
            addNavButton("Billing & Sales POS", "billing");
            addNavButton("Returns & Refunds", "returns");
            addNavButton("Product Catalog Lookup", "inventory");
        }

        sidebarNavList.revalidate();
        sidebarNavList.repaint();
    }

    private void addNavButton(String text, String cardKey) {
        JButton btn = createNavButton(text, cardKey);
        navButtons.put(cardKey, btn);
        sidebarNavList.add(btn);
        sidebarNavList.add(Box.createVerticalStrut(6));
    }

    private JButton createNavButton(String text, String cardKey) {
        SidebarNavButton btn = new SidebarNavButton(text);
        btn.addActionListener(e -> showCard(cardKey));
        return btn;
    }

    /**
     * Custom antialiased sidebar navigation button.
     * Guarantees 100% visible, high-contrast solid black text on clean card backgrounds,
     * immune to native Windows LookAndFeel button overrides.
     */
private static class SidebarNavButton extends JButton {
        private boolean isActive = false;
        private boolean isHovered = false;

        public SidebarNavButton(String text) {
            super(text);
            setFont(shelflife.ui.theme.Theme.FONT_BOLD);
            setForeground(shelflife.ui.theme.Theme.FG_SIDEBAR_TEXT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.LEFT);
            setPreferredSize(new Dimension(204, shelflife.ui.theme.Theme.SIDEBAR_ITEM_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, shelflife.ui.theme.Theme.SIDEBAR_ITEM_HEIGHT));
            setAlignmentX(Component.CENTER_ALIGNMENT);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            Color bg;
            Color border;

            if (isActive) {
                bg = Color.decode("#DBEAFE"); // Soft blue card for selected item
                border = Color.decode("#2563EB"); // Royal blue accent border
            } else if (isHovered) {
                bg = Color.decode("#F1F5F9"); // Subtle light gray hover
                border = Color.decode("#CBD5E1");
            } else {
                bg = Color.WHITE;
                border = Color.decode("#E2E8F0");
            }

            // Fill 8px rounded card
            g2.setColor(bg);
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 8, 8));

            // Draw border
            g2.setColor(border);
            g2.setStroke(new BasicStroke(isActive ? 1.5f : 1.0f));
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 8, 8));

            // Left indicator bar for active item
            if (isActive) {
                g2.setColor(Color.decode("#2563EB"));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(1, 3, 4, h - 6, 3, 3));
            }

            // Draw text in 100% solid black
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = isActive ? 16 : 14;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Color.BLACK);
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout(15, 0));
        status.setBackground(UITheme.BG_PAGE);
        status.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
                new EmptyBorder(5, 14, 5, 14)
        ));

        lblStatusMessage = new JLabel("System Ready.");
        lblStatusMessage.setFont(UITheme.FONT_REGULAR);
        lblStatusMessage.setForeground(UITheme.TEXT_MAIN);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);

        lblStatusUser = new JLabel("User: None");
        lblStatusUser.setFont(UITheme.FONT_SMALL);
        lblStatusUser.setForeground(UITheme.TEXT_MUTED);

        lblStatusStorage = new JLabel("Storage: " + dataStore.getDataDirectory().getName() + "/");
        lblStatusStorage.setFont(UITheme.FONT_SMALL);
        lblStatusStorage.setForeground(UITheme.TEXT_MUTED);

        lblStatusDate = new JLabel("Date: " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        lblStatusDate.setFont(UITheme.FONT_SMALL);
        lblStatusDate.setForeground(UITheme.TEXT_MUTED);

        rightPanel.add(lblStatusUser);
        rightPanel.add(lblStatusStorage);
        rightPanel.add(lblStatusDate);

        status.add(lblStatusMessage, BorderLayout.WEST);
        status.add(rightPanel, BorderLayout.EAST);

        return status;
    }

    private JMenuBar createAppMenuBar() {
        JMenuBar mb = new JMenuBar();

        // File Menu
        JMenu menuFile = new JMenu("File");
        JMenuItem itemReload = new JMenuItem("Reload Database");
        itemReload.addActionListener(e -> {
            inventoryService.reload();
            salesService.reload();
            supplierService.reload();
            returnsService.reload();
            reportService.refreshData();
            refreshActiveCard();
            JOptionPane.showMessageDialog(this, "All database records reloaded from CSV files.", "Reloaded", JOptionPane.INFORMATION_MESSAGE);
        });

        menuItemBackup = new JMenuItem("Create Backup Now");
        menuItemBackup.addActionListener(e -> {
            if (authService.getCurrentUser() != null && authService.getCurrentUser().getRole() != Role.ADMIN) {
                JOptionPane.showMessageDialog(this, "Access restricted: Database backup creation is restricted to Administrators.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                File dir = dataStore.createBackup(null);
                JOptionPane.showMessageDialog(this, "Backup archive created at:\n" + dir.getAbsolutePath(), "Backup Succeeded", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Backup Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JMenuItem itemLogout = new JMenuItem("Switch User / Logout");
        itemLogout.addActionListener(e -> handleLogout());

        JMenuItem itemExit = new JMenuItem("Exit");
        itemExit.addActionListener(e -> confirmExit());

        menuFile.add(itemReload);
        menuFile.add(menuItemBackup);
        menuFile.addSeparator();
        menuFile.add(itemLogout);
        menuFile.add(itemExit);

        // View Menu
        JMenu menuView = new JMenu("View");
        JMenuItem itemInv = new JMenuItem("Inventory & Batches");
        itemInv.addActionListener(e -> showCard("inventory"));
        menuItemSup = new JMenuItem("Stock Intake & Suppliers");
        menuItemSup.addActionListener(e -> showCard("suppliers"));
        menuItemExp = new JMenuItem("Expiry & Wastage");
        menuItemExp.addActionListener(e -> showCard("expiry"));
        JMenuItem itemBill = new JMenuItem("Billing & POS");
        itemBill.addActionListener(e -> showCard("billing"));
        JMenuItem itemRet = new JMenuItem("Returns & Refunds");
        itemRet.addActionListener(e -> showCard("returns"));
        menuItemRep = new JMenuItem("Reports & Dashboard");
        menuItemRep.addActionListener(e -> showCard("reports"));
        menuItemReord = new JMenuItem("Reorder List");
        menuItemReord.addActionListener(e -> showCard("reorder"));
        menuItemLogs = new JMenuItem("Activity Audit Log");
        menuItemLogs.addActionListener(e -> showCard("logs"));
        menuItemSet = new JMenuItem("Settings & Backup");
        menuItemSet.addActionListener(e -> showCard("settings"));

        menuView.add(itemInv);
        menuView.add(menuItemSup);
        menuView.add(menuItemExp);
        menuView.add(itemBill);
        menuView.add(itemRet);
        menuView.add(menuItemRep);
        menuView.add(menuItemReord);
        menuView.add(menuItemLogs);
        menuView.add(menuItemSet);

        // Help Menu
        JMenu menuHelp = new JMenu("Help");
        JMenuItem itemAbout = new JMenuItem("About ShelfLife");
        itemAbout.addActionListener(e -> showAboutDialog());
        menuHelp.add(itemAbout);

        mb.add(menuFile);
        mb.add(menuView);
        mb.add(menuHelp);

        // Apply menu colors from theme
        menuFile.setBackground(UITheme.BG_PAGE);
        menuFile.setForeground(UITheme.TEXT_MAIN);
        menuView.setBackground(UITheme.BG_PAGE);
        menuView.setForeground(UITheme.TEXT_MAIN);
        menuHelp.setBackground(UITheme.BG_PAGE);
        menuHelp.setForeground(UITheme.TEXT_MAIN);

        return mb;
    }

    private void rebuildMenuBar(Role role) {
        boolean isAdmin = (role == Role.ADMIN);
        if (menuItemBackup != null) menuItemBackup.setEnabled(isAdmin);
        if (menuItemSup != null) menuItemSup.setEnabled(isAdmin);
        if (menuItemExp != null) menuItemExp.setEnabled(isAdmin);
        if (menuItemRep != null) menuItemRep.setEnabled(isAdmin);
        if (menuItemReord != null) menuItemReord.setEnabled(isAdmin);
        if (menuItemLogs != null) menuItemLogs.setEnabled(isAdmin);
        if (menuItemSet != null) menuItemSet.setEnabled(isAdmin);
    }

    private void initPanels() {
        inventoryPanel = new ProductInventoryPanel(inventoryService);
        suppliersPanel = new SuppliersPanel(inventoryService, supplierService);
        expiryPanel = new ExpiryAlertsPanel(inventoryService, expiryService);
        billingPanel = new BillingPanel(inventoryService, salesService);
        returnsPanel = new ReturnsPanel(returnsService, salesService, authService);
        reportsPanel = new DashboardReportsPanel(reportService, financialService, capitalService, expenseService, cashAccountService);
        overviewPanel = new shelflife.ui.reports.business.BusinessOverviewPanel(reportService);
        reorderPanel = new ReorderListPanel(inventoryService, supplierService, reportService);
        activityLogPanel = new ActivityLogPanel(logService);
        settingsAndBackupPanel = new SettingsAndBackupPanel(settingsService, authService, dataStore);

        cardPanel.add(inventoryPanel, "inventory");
        cardPanel.add(suppliersPanel, "suppliers");
        cardPanel.add(expiryPanel, "expiry");
        cardPanel.add(billingPanel, "billing");
        cardPanel.add(returnsPanel, "returns");
        cardPanel.add(overviewPanel, "overview");
        cardPanel.add(reportsPanel, "reports");
        cardPanel.add(reorderPanel, "reorder");
        cardPanel.add(activityLogPanel, "logs");
        cardPanel.add(settingsAndBackupPanel, "settings");
    }

    public void showCard(String cardKey) {
        if ("activity".equalsIgnoreCase(cardKey)) {
            cardKey = "logs";
        }
        // Only administrators may open every module; other roles are limited to the POS screens.
        User current = authService.getCurrentUser();
        if (current != null && !AccessControl.isAdmin(current) && !CASHIER_CARDS.contains(cardKey)) {
            JOptionPane.showMessageDialog(this, "Access restricted: Cashier role is limited to Billing, Returns, and Product Lookup.", "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.currentCard = cardKey;
        cardLayout.show(cardPanel, cardKey);
        updateNavButtonHighlights(cardKey);
        refreshActiveCard();
        lblStatusMessage.setText("Module: " + formatModuleName(cardKey) + " active.");
    }

    private void refreshActiveCard() {
        switch (currentCard) {
            case "inventory":
                if (inventoryPanel != null) inventoryPanel.refreshData();
                break;
            case "suppliers":
                if (suppliersPanel != null) suppliersPanel.refreshData();
                break;
            case "expiry":
                if (expiryPanel != null) expiryPanel.refreshData();
                break;
            case "billing":
                if (billingPanel != null) billingPanel.refreshData();
                break;
            case "returns":
                if (returnsPanel != null) returnsPanel.refreshData();
                break;
            case "overview":
                if (overviewPanel != null) overviewPanel.refreshData();
                break;
            case "reports":
                if (reportsPanel != null) reportsPanel.refreshData();
                break;
            case "reorder":
                if (reorderPanel != null) reorderPanel.refreshData();
                break;
            case "logs":
                if (activityLogPanel != null) activityLogPanel.refreshData();
                break;
            case "settings":
                if (settingsAndBackupPanel != null) settingsAndBackupPanel.refreshData();
                break;
        }
    }

    private void updateNavButtonHighlights(String activeKey) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean isActive = entry.getKey().equals(activeKey);
            JButton btn = entry.getValue();
            if (btn instanceof SidebarNavButton) {
                ((SidebarNavButton) btn).setActive(isActive);
            }
        }
    }

    private String formatModuleName(String key) {
        switch (key) {
            case "inventory": return "Product & Inventory Management";
            case "suppliers": return "Stock Intake & Supplier Directory";
            case "expiry": return "Expiry Alerts & Stock Wastage";
            case "billing": return "Point of Sale (POS) Billing";
            case "returns": return "Customer Returns & Refund Management";
            case "reports": return "Operational Reports & KPI Dashboard";
            case "reorder": return "Stock Reorder Planning";
            case "logs": return "Activity & Security Audit Trail";
            case "settings": return "System Settings & Database Backups";
            default: return key;
        }
    }

    private void handleLogout() {
        // Show confirmation dialog
        int res = JOptionPane.showConfirmDialog(this,
                "Do you want to sign out of ShelfLife?",
                "Sign Out",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (res != JOptionPane.YES_OPTION) {
            return;
        }

        if (authService.getCurrentUser() != null) {
            logService.log(authService.getCurrentUser().getUsername(), "LOGOUT", "User logged out of session", authService.getCurrentUser().getUserId());
        }
        authService.logout();
        promptLogin();
    }

    private void confirmExit() {
        int res = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit ShelfLife?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            if (authService.getCurrentUser() != null) {
                logService.log(authService.getCurrentUser().getUsername(), "SYSTEM_EXIT", "Application closed normally", "-");
            }
            System.exit(0);
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "ShelfLife — Inventory, Expiry & Billing Management System\n" +
                "Version 1.0 (Full Functional Release)\n\n" +
                "Team Architectural Attribution:\n" +
                "• Member 1: Product Catalog & Batch Inventory Management\n" +
                "• Member 2: Batch Expiry Date Monitoring & Alert Rules\n" +
                "• Member 3: Billing POS, FEFO Checkout & Receipt Printing\n" +
                "• Member 4: Reporting, KPI Dashboard & Data Analysis\n" +
                "• Member 5: System Integration, Core Architecture & UI Shell\n\n" +
                "Features:\n" +
                "• Salted SHA-256 Authentication & Role-Based Navigation\n" +
                "• ACID Multi-File CSV Storage Engine with .bak Snapshot Rollback\n" +
                "• Automated FEFO Batch Stock Allocation\n" +
                "• Complete Customer Returns & Refund Arithmetic\n" +
                "• Supplier Directory & Stock Intake Receiving\n" +
                "• Automated Reorder List & Live Activity Audit Trail\n" +
                "• Exact BigDecimal Currency Arithmetic (PKR)",
                "About ShelfLife",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String args[]) {
        UITheme.initLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            ShelfLifeApp app = new ShelfLifeApp();
            app.setVisible(true);
        });
    }
}