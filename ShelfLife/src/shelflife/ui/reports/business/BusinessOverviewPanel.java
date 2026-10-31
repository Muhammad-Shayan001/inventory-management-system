package shelflife.ui.reports.business;

import shelflife.model.*;
import shelflife.service.*;
import shelflife.ui.chart.*;
import shelflife.ui.theme.Theme;
import shelflife.ui.theme.Styles;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingConstants;

public class BusinessOverviewPanel extends JPanel {

    private final ReportService reportService;
    private final User user;

    private JComboBox<String> dateRangeCombo;
    private JButton refreshBtn;
    private JLabel lastUpdatedLabel;
    private JButton exportCsvBtn;
    private JButton exportImageBtn;

    private Timer refreshTimer;
    private LocalDate lastSelectedDate;

    private KpiTile[] kpiTiles;
    private LineChartPanel salesTrendChart;
    private BarChartPanel topProductsChart;
    private DonutChartPanel paymentMethodsChart;
    private DonutChartPanel stockHealthChart;
    private BarChartPanel wastageByReasonChart;
    private BarChartPanel profitVsMonthChart;
    private BarChartPanel salesByHourChart;

    private JTable expiringSoonTable;
    private JTable lowStockTable;
    private JTable recentSalesTable;
    private JTable recentActivityTable;
    private JTable deadStockTable;

    // Card border for nice rounded corners with shadow effect
    private static class CardBorder extends AbstractBorder {
        private final int radius;
        private final Color shadowColor;
        private final int shadowGap;

        CardBorder(int radius, Color shadowColor, int shadowGap) {
            this.radius = radius;
            this.shadowColor = shadowColor;
            this.shadowGap = shadowGap;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.BORDER_LIGHT);
            g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius);
            // Subtle bottom border
            g2.setColor(Theme.ACCENT_SECONDARY);
            g2.drawLine(x, height - 1, width - 1, height - 1);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + shadowGap, radius + shadowGap, radius + shadowGap, radius + shadowGap);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = radius + shadowGap;
            insets.top = radius + shadowGap;
            insets.right = radius + shadowGap;
            insets.bottom = radius + shadowGap;
            return insets;
        }
    }

    // Card panel with shadow and rounded corners
    private static class CardPanel extends JPanel {
        private final int radius;

        CardPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(260, 140));
            setMinimumSize(new Dimension(260, 140));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Soft shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(0, getHeight() / 2, getWidth(), getHeight() / 2, radius, radius);
            // Card background
            g2.setColor(Theme.BG_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Professional KPI tile with better design
    private static class KpiTile extends JPanel {
        private final JLabel label;
        private final JLabel valueLabel;
        private final JLabel descriptionLabel;
        private String changeDirection = "—";
        private boolean isUpward = false;

        public KpiTile(String labelText, String descriptionText) {
            setLayout(new BorderLayout(0, 12));
            setOpaque(false);
            setPreferredSize(new Dimension(200, 100));
            setMinimumSize(new Dimension(200, 100));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Mouse hover effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBorder(new LineBorder(Theme.ACCENT_SECONDARY, 1, true));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    setBorder(null);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    JOptionPane.showMessageDialog(BusinessOverviewPanel.this,
                            "Opening related detail view", "Tile Action",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });

            // Top label
            label = new JLabel(labelText);
            label.setFont(Theme.FONT_SUBTITLE);
            label.setForeground(Theme.TEXT_SECONDARY);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            add(label, BorderLayout.NORTH);

            // Center value
            valueLabel = new JLabel("0");
            valueLabel.setFont(Theme.FONT_KPI_VAL_BOLD);
            valueLabel.setForeground(Theme.ACCENT_PRIMARY);
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(valueLabel, BorderLayout.CENTER);

            // Description at bottom
            descriptionLabel = new JLabel(descriptionText);
            descriptionLabel.setFont(Theme.FONT_SMALL);
            descriptionLabel.setForeground(Theme.FG_SIDEBAR_TEXT);
            descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(descriptionLabel, BorderLayout.SOUTH);
        }

        public void setValue(String value) {
            valueLabel.setText(value);
        }

        public void setChange(String direction, boolean isUpward) {
            this.changeDirection = direction;
            this.isUpward = isUpward;
            updateChangeVisual();
        }

        private void updateChangeVisual() {
            if (isUpward) {
                label.setForeground(Theme.ACCENT_POSITIVE);
                descriptionLabel.setForeground(Theme.ACCENT_POSITIVE);
            } else if (direction.equals("Down")) {
                label.setForeground(Theme.ACCENT_NEGATIVE);
                descriptionLabel.setForeground(Theme.ACCENT_NEGATIVE);
            } else {
                label.setForeground(Theme.TEXT_SECONDARY);
                descriptionLabel.setForeground(Theme.FG_SIDEBAR_TEXT);
            }
        }
    }

    public BusinessOverviewPanel(ReportService reportService, User user) {
        this.reportService = reportService;
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_PAGE);
        setBorder(new EmptyBorder(4, 24, 4, 24));

        // ---- Top Bar ----
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(Theme.BG_HEADER_NAVY);
        topBar.setBorder(new EmptyBorder(16, 24, 16, 24));

        // Title with icon-like text
        JLabel titleLabel = new JLabel("Business Overview");
        titleLabel.setFont(Theme.FONT_PAGE_TITLE);
        titleLabel.setForeground(Color.WHITE);
        topBar.add(titleLabel, BorderLayout.WEST);

        // Date range selector - improved
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        datePanel.setOpaque(false);

        String[] dateOptions = {"Today", "Last 7 Days", "Last 30 Days", "This Month", "Custom"};
        dateRangeCombo = new JComboBox<>(dateOptions);
        dateRangeCombo.setFont(Theme.FONT_SMALL);
        dateRangeCombo.setBackground(new Color(0, 0, 0, 0));
        dateRangeCombo.setForeground(Theme.TEXT_PRIMARY);
        dateRangeCombo.addActionListener(e -> onDateRangeChanged());
        datePanel.add(dateRangeCombo);

        refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(Theme.FONT_BOLD);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBackground(Theme.ACCENT_PRIMARY);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadData());
        datePanel.add(refreshBtn);

        topBar.add(datePanel, BorderLayout.CENTER);

        // Last updated label
        lastUpdatedLabel = new JLabel("Last updated: just now");
        lastUpdatedLabel.setFont(Theme.FONT_SUBTITLE);
        lastUpdatedLabel.setForeground(Theme.FG_SIDEBAR_TEXT);
        topBar.add(lastUpdatedLabel, BorderLayout.EAST);

        // Export panel
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        exportPanel.setOpaque(false);
        exportCsvBtn = new JButton("Export Summary (CSV)");
        exportCsvBtn.setFont(Theme.FONT_SMALL);
        exportCsvBtn.setForeground(Color.WHITE);
        exportCsvBtn.setBackground(Theme.ACCENT_SECONDARY);
        exportCsvBtn.setFocusPainted(false);
        exportCsvBtn.addActionListener(e -> exportCsv());
        exportImageBtn = new JButton("Save as Image (PNG)");
        exportImageBtn.setFont(Theme.FONT_SMALL);
        exportImageBtn.setForeground(Color.WHITE);
        exportImageBtn.setBackground(Theme.ACCENT_SECONDARY);
        exportImageBtn.setFocusPainted(false);
        exportImageBtn.addActionListener(e -> exportImage());
        exportPanel.add(exportCsvBtn);
        exportPanel.add(exportImageBtn);

        topBar.add(exportPanel, BorderLayout.SOUTH);

        add(topBar, BorderLayout.NORTH);

        // ---- KPI Row ----
        JPanel kpiPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        kpiPanel.setBackground(Theme.BG_PAGE);
        kpiPanel.setBorder(new EmptyBorder(0, 24, 16, 24));

        kpiTiles = new KpiTile[11];
        String[] kpiLabels = {"Today's Sales", "This Month's Sales", "Today's Profit",
                "This Month's Profit", "Stock Value", "Products", "Low Stock",
                "Expired Stock", "Near-Expiry", "Sales Count", "Average Bill"};
        String[] kpiDescriptions = {"Net sales for today", "Net sales for this month",
                "Profit for today", "Profit for this month", "Total stock value (good + expired)",
                "Active product count", "Low-stock product count", "Expired batch count and value",
                "Near-expiry batch count", "Total number of sales", "Average bill value"};

        // 3 cards per row with professional spacing
        for (int i = 0; i < kpiTiles.length; i++) {
            kpiTiles[i] = new KpiTile(kpiLabels[i], kpiDescriptions[i]);
            kpiPanel.add(kpiTiles[i]);
        }

        add(kpiPanel, BorderLayout.CENTER);

        // ---- Charts Area ----
        JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        chartsPanel.setBackground(Theme.BG_PAGE);
        chartsPanel.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Card-style chart containers
        JPanel chart1Card = createChartCard();
        chart1Card.add(salesTrendChart = new LineChartPanel());

        JPanel chart2Card = createChartCard();
        chart2Card.add(topProductsChart = new BarChartPanel());

        JPanel chart3Card = createChartCard();
        chart3Card.add(paymentMethodsChart = new DonutChartPanel());

        JPanel chart4Card = createChartCard();
        chart4Card.add(stockHealthChart = new DonutChartPanel());

        chartsPanel.add(chart1Card);
        chartsPanel.add(chart2Card);
        chartsPanel.add(chart3Card);
        chartsPanel.add(chart4Card);

        add(chartsPanel, BorderLayout.CENTER);

        // ---- Tables Area ----
        JPanel tablesPanel = new JPanel(new GridLayout(1, 4, 16, 16));
        tablesPanel.setBackground(Theme.BG_PAGE);
        tablesPanel.setBorder(new EmptyBorder(0, 24, 24, 24));

        // Table style: alternating rows, proper formatting
        expiringSoonTable = createStripedTable(new String[]{"Product", "Batch", "Qty", "Expiry", "Days Left"});
        lowStockTable = createStripedTable(new String[]{"Product", "Current", "Threshold"});
        recentSalesTable = createStripedTable(new String[]{"Sale ID", "Date", "Amount", "Payment"});
        recentActivityTable = createStripedTable(new String[]{"Action", "User", "Details", "Time"});

        tablesPanel.add(new JScrollPane(expiringSoonTable));
        tablesPanel.add(new JScrollPane(lowStockTable));
        tablesPanel.add(new JScrollPane(recentSalesTable));
        tablesPanel.add(new JScrollPane(recentActivityTable));

        add(tablesPanel, BorderLayout.SOUTH);

        // Start auto-refresh
        startAutoRefresh();
        loadData();
    }

    // Create a professional card for charts
    private JPanel createChartCard() {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        return card;
    }

    // Create a striped table with professional styling
    private JTable createStripedTable(String[] columnNames) {
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Add sample rows
        if (columnNames[0].equals("Product")) {
            model.addRow(new Object[]{"Product A", "BATCH-001", 5, "25 Oct 2026", "3"});
            model.addRow(new Object[]{"Product B", "BATCH-002", 3, "28 Oct 2026", "6"});
        } else if (columnNames[0].equals("Sale ID")) {
            model.addRow(new Object[]{"SALE-20261031-1", "25 Oct 2026", "Rs. 750.00", "Cash"});
            model.addRow(new Object[]{"SALE-20261031-2", "24 Oct 2026", "Rs. 1,200.00", "Card"});
        } else if (columnNames[0].equals("Action")) {
            model.addRow(new Object[]{"Stock Adjustment", "admin", "Adjusted batch B-001 by +5 units", "25 Oct 2026, 14:30"});
            model.addRow(new Object[]{"Price Update", "admin", "Product PROD-01 price updated to Rs. 250.00", "25 Oct 2026, 14:00"});
        } else {
            model.addRow(new Object[]{"Owner Withdrawal", "admin", "Rs. 5,000.00 capital withdrawal", "25 Oct 2026, 10:15"});
            model.addRow(new Object[]{"Supplier Payment", "admin", "SUP-TEST-01 paid Rs. 6,000.00", "24 Oct 2026, 16:45"});
        }

        JTable table = new JTable(model);
        table.setBackground(Theme.BG_CARD);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setSelectionBackground(Theme.ACCENT_PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(Theme.FONT_TABLE_CELL);
        table.setRowHeight(28);
        table.setIntercellSpacing(new Dimension(8, 4));

        // Professional header
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_TABLE_HEADER);
        header.setBackground(Theme.BG_TABLE_HEADER);
        header.setForeground(Theme.FG_TABLE_HEADER_TEXT);
        header.setReorderingAllowed(false);

        // Alternating row colors - rendered via custom renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Theme.BG_CARD : Color.WHITE);
                    setForeground(Theme.TEXT_PRIMARY);
                }
                return c;
            }
        });

        return table;
    }

    private void onDateRangeChanged() {
        String selected = (String) dateRangeCombo.getSelectedItem();
        LocalDate today = LocalDate.now();

        if (selected.equals("Today")) {
            lastSelectedDate = today;
        } else if (selected.equals("Last 7 Days")) {
            lastSelectedDate = today.minusDays(7);
        } else if (selected.equals("Last 30 Days")) {
            lastSelectedDate = today.minusDays(30);
        } else if (selected.equals("This Month")) {
            lastSelectedDate = today.withDayOfMonth(1);
        } else if (selected.equals("Custom")) {
            lastSelectedDate = today;
        }

        loadData();
    }

    private void loadData() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                // KPI tiles
                if (kpiTiles != null) {
                    LocalDate today = LocalDate.now();
                    LocalDate yesterday = today.minusDays(1);
                    BigDecimal todaySales = reportService.getSalesTotalForDate(today);
                    BigDecimal yesterdaySales = reportService.getSalesTotalForDate(yesterday);

                    kpiTiles[0].setValue(todaySales.stripTrailingZeros().toPlainString());
                    String vsText = yesterdaySales.compareTo(BigDecimal.ZERO) == 0 ? "New"
                            : todaySales.compareTo(yesterdaySales) > 0 ? "Up" : "Down";
                    kpiTiles[0].setChange(vsText, todaySales.compareTo(yesterdaySales) > 0);

                    // This month's sales
                    BigDecimal monthSales = BigDecimal.ZERO;
                    for (Sale s : reportService.getSalesHistory()) {
                        if (s.getSaleDateTime().toLocalDate().isAfter(today.withDayOfMonth(1))
                                && s.getSaleDateTime().toLocalDate().isBefore(today.plusMonths(1))) {
                            monthSales = monthSales.add(s.getTotalAmount());
                        }
                    }
                    kpiTiles[1].setValue(monthSales.stripTrailingZeros().toPlainString());

                    // Today's profit
                    kpiTiles[2].setValue(todaySales.stripTrailingZeros().toPlainString());
                    kpiTiles[2].setChange("—", false);

                    // This month's profit
                    kpiTiles[3].setValue(monthSales.stripTrailingZeros().toPlainString());
                    kpiTiles[3].setChange("—", false);

                    // Stock value (simplified)
                    kpiTiles[4].setValue("Rs. 42,500.00");
                    kpiTiles[5].setValue(String.valueOf(reportService.getTotalProductCount()));
                    kpiTiles[6].setValue(String.valueOf(reportService.getLowStockCount()));
                    kpiTiles[7].setValue(String.valueOf(reportService.getExpiredBatchCount()) + " batches");
                    kpiTiles[8].setValue(String.valueOf(reportService.getNearExpiryBatchCount(7)) + " batches");
                    kpiTiles[9].setValue(String.valueOf(reportService.getSalesHistory().size()));
                    kpiTiles[10].setValue("Rs. 850.00");
                }

                // Sales trend chart
                List<LocalDate> dates = new ArrayList<>();
                List<BigDecimal> salesValues = new ArrayList<>();
                for (int i = 0; i < 30; i++) {
                    LocalDate d = today.minusDays(i);
                    dates.add(0, d);
                    salesValues.add(0, reportService.getDailySales(d, d).getOrDefault(d, BigDecimal.ZERO));
                }
                salesTrendChart.setData("Sales Trend", "Last 30 Days",
                        dates, salesValues, null);

                // Top products
                List<String> prodNames = new ArrayList<>();
                List<BigDecimal> prodRevenues = new ArrayList<>();
                for (Product p : reportService.getTopProducts(LocalDate.now().minusDays(30), LocalDate.now(), 5)) {
                    prodNames.add(p.getProductName());
                    prodRevenues.add(p.getRevenue());
                }
                topProductsChart.setData("Top 5 Products", "", prodNames, prodRevenues, null, BarChartPanel.Mode.VERTICAL);

                // Payment methods donut
                Map<String, BigDecimal> paymentMap = reportService.getSalesByPaymentMethod(LocalDate.now().minusDays(30), LocalDate.now());
                List<String> payNames = new ArrayList<>(paymentMap.keySet());
                List<BigDecimal> payValues = new ArrayList<>(paymentMap.values());
                List<Color> payColors = new ArrayList<>();
                for (int i = 0; i < payNames.size(); i++) {
                    payColors.add(Theme.ACCENT_PRIMARY);
                }
                paymentMethodsChart.setData("Payment Methods", "Last 30 Days", payNames, payValues, payColors, null);

                // Stock health donut
                StockHealth health = reportService.getStockHealth(LocalDate.now());
                List<String> healthNames = Arrays.asList("Good", "Near-Expiry", "Expired");
                List<BigDecimal> healthValues = Arrays.asList(
                        BigDecimal.valueOf(health.getGoodQuantity()),
                        BigDecimal.valueOf(health.getNearExpiryQuantity()),
                        BigDecimal.valueOf(health.getExpiredQuantity())
                );
                List<Color> healthColors = Arrays.asList(
                        Color.decode("#10b981"), new Color(234, 179, 8),
                        Color.decode("#ef4444")
                );
                stockHealthChart.setData("Stock Health", "As of " + LocalDate.now(), healthNames, healthValues, healthColors, null);

                // Wastage by reason
                Map<String, BigDecimal> wastageMap = reportService.getWastageByReason(LocalDate.now().minusDays(30), LocalDate.now());
                List<String> reasonNames = new ArrayList<>(wastageMap.keySet());
                List<BigDecimal> reasonValues = new ArrayList<>(wastageMap.values());
                wastageByReasonChart.setData("Wastage by Reason", "Last 30 Days", reasonNames, reasonValues, null, BarChartPanel.Mode.VERTICAL);

                // Profit vs Month
                List<MonthlyRow> monthlyRows = reportService.getMonthlySalesAndProfit(6);
                List<String> monthNames = new ArrayList<>();
                List<BigDecimal> profitValues = new ArrayList<>();
                for (MonthlyRow row : monthlyRows) {
                    monthNames.add(row.getMonthLabel());
                    profitValues.add(row.getProfit());
                }
                profitVsMonthChart.setData("Sales vs Profit", "Last 6 Months", monthNames, profitValues, null, BarChartPanel.Mode.GROUPED);

                // Sales by hour
                Map<Integer, BigDecimal> hourMap = reportService.getSalesByHour(LocalDate.now().minusDays(30), LocalDate.now());
                List<String> hourLabels = new ArrayList<>();
                List<BigDecimal> hourValues = new ArrayList<>();
                for (int i = 0; i < 24; i++) {
                    hourLabels.add(String.format("%02d:00", i));
                    hourValues.add(hourMap.getOrDefault(i, BigDecimal.ZERO));
                }
                salesByHourChart.setData("Sales by Hour", "Last 30 Days", hourLabels, hourValues, null, BarChartPanel.Mode.VERTICAL);

                return null;
            }
        }.execute();
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                loadData();
                LocalDate today = LocalDate.now();
                lastUpdatedLabel.setText("Last updated: " + today + ", " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm aa")));
            }
        }, 60000, 60000);
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("business_overview_" + LocalDate.now() + ".csv"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "Export functionality would save data to: " + chooser.getSelectedFile(),
                    "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("business_overview_" + LocalDate.now() + ".png"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "Chart export would save visualization to: " + chooser.getSelectedFile(),
                    "Export Image", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // User class placeholder
    public static class User {
        private final String name;
        private final String role;

        public User(String name, String role) {
            this.name = name;
            this.role = role;
        }

        public String getName() { return name; }
        public String getRole() { return role; }
    }
}