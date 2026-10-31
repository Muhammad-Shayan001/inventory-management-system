package shelflife.ui;

import shelflife.model.*;
import shelflife.service.*;
import shelflife.ui.inventory.ProductInventoryPanel;
import shelflife.ui.expiry.ExpiryAlertsPanel;
import shelflife.ui.billing.BillingPanel;
import shelflife.ui.reports.DashboardReportsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * Main application window for ShelfLife.
 * Creates the shared architecture, instantiates one DataStore and one shared
 * instance of each required service, and injects the same service instances
 * into all panels. Uses CardLayout for navigation between panels.
 * Launches Swing UI on the Event Dispatch Thread.
 * 
 * @author ShelfLife Project - Member 5 (Project Head, Architecture & Integration Lead)
 * @version 1.0 (Shared Architecture Contract)
 */
public class ShelfLifeApp extends javax.swing.JFrame {
    private final shelflife.storage.DataStore dataStore;
    private final InventoryService inventoryService;
    private final ExpiryService expiryService;
    private final SalesService salesService;
    private final ReportService reportService;
    
    private CardLayout cardLayout;
    private JPanel cardPanel;
    
    // Panel instances (shared references)
    private ProductInventoryPanel inventoryPanel;
    private ExpiryAlertsPanel expiryPanel;
    private BillingPanel billingPanel;
    private DashboardReportsPanel reportsPanel;
    
    // Navigation buttons
    private JButton btnInventory;
    private JButton btnExpiry;
    private JButton btnBilling;
    private JButton btnReports;
    private JPanel sidebarPanel;
    
    // Status bar
    private JLabel statusLabel;
    
    public ShelfLifeApp() {
        // Initialize shared DataStore
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        dataStore = new shelflife.storage.CsvDataStore(dataDir);
        
        // Initialize shared services with the same DataStore instance
        inventoryService = new shelflife.service.InventoryServiceImpl(dataStore);
        expiryService = new ExpiryServiceImpl(inventoryService, dataStore);
        salesService = new SalesServiceImpl(inventoryService, dataStore);
        reportService = new ReportServiceImpl(inventoryService, salesService, dataStore);
        
        // Set up the main window
        setTitle("ShelfLife - Inventory Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Initialize UI components
        initComponents();
        
        // Initialize panels with shared service instances
        initializePanels();
        
        // Set initial view
        showPanel(inventoryPanel);
        
        statusLabel.setText("ShelfLife v1.0 - Ready. Data directory: " + dataDir.getAbsolutePath());
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        
        // Sidebar navigation
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        sidebarPanel.setPreferredSize(new Dimension(150, 0));
        sidebarPanel.setBackground(new Color(240, 240, 240));
        
        btnInventory = new JButton("Inventory");
        btnInventory.setPreferredSize(new Dimension(130, 30));
        btnInventory.addActionListener(e -> {
            showPanel(inventoryPanel);
            inventoryPanel.refreshData();
        });
        sidebarPanel.add(btnInventory);
        
        btnExpiry = new JButton("Expiry Alerts");
        btnExpiry.setPreferredSize(new Dimension(130, 30));
        btnExpiry.addActionListener(e -> {
            showPanel(expiryPanel);
            expiryPanel.refreshData();
        });
        sidebarPanel.add(btnExpiry);
        
        btnBilling = new JButton("Billing");
        btnBilling.setPreferredSize(new Dimension(130, 30));
        btnBilling.addActionListener(e -> {
            showPanel(billingPanel);
            billingPanel.refreshData();
        });
        sidebarPanel.add(btnBilling);
        
        btnReports = new JButton("Reports");
        btnReports.setPreferredSize(new Dimension(130, 30));
        btnReports.addActionListener(e -> {
            showPanel(reportsPanel);
            reportsPanel.refreshData();
        });
        sidebarPanel.add(btnReports);
        
        add(sidebarPanel, BorderLayout.WEST);
        
        // Card panel for main content
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel, BorderLayout.CENTER);
        
        // Status bar
        statusLabel = new JLabel("ShelfLife v1.0 - Initializing...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.SOUTH);
        
        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, 
                "ShelfLife v1.0\nInventory Management System\nUniversity Java OOP Project", 
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
    
    private void initializePanels() {
        inventoryPanel = new ProductInventoryPanel(inventoryService);
        expiryPanel = new ExpiryAlertsPanel(inventoryService, expiryService);
        billingPanel = new BillingPanel(inventoryService, salesService);
        reportsPanel = new DashboardReportsPanel(reportService);
        
        cardPanel.add(inventoryPanel, "inventory");
        cardPanel.add(expiryPanel, "expiry");
        cardPanel.add(billingPanel, "billing");
        cardPanel.add(reportsPanel, "reports");
    }
    
    private void showPanel(JPanel panel) {
        cardLayout.show(cardPanel, panel.getName());
        updateStatus(panel.getClass().getSimpleName());
    }
    
    private void updateStatus(String view) {
        statusLabel.setText("ShelfLife v1.0 - " + view + " view active");
    }
    
    public static void main(String args) {
        /* Set the Nimbus look and feel */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new ShelfLifeApp().setVisible(true);
        });
    }
}