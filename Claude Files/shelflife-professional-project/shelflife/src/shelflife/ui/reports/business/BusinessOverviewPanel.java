package shelflife.ui.reports.business;

import shelflife.model.Product;
import shelflife.model.ProductBatch;
import shelflife.model.ProductSales;
import shelflife.model.MonthlyRow;
import shelflife.model.Sale;
import shelflife.model.StockHealth;
import shelflife.service.ReportService;
import shelflife.ui.Refreshable;
import shelflife.ui.chart.BarChartPanel;
import shelflife.ui.chart.DonutChartPanel;
import shelflife.ui.chart.LineChartPanel;
import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;
import shelflife.util.CurrencyUtil;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard that summarises the shop's sales, profit, stock health and
 * risks. All numbers come from {@link ReportService}; nothing is hard-coded.
 * Data is loaded in a background thread and applied on the Swing thread.
 */
public class BusinessOverviewPanel extends JPanel implements Refreshable {

    private static final String[] RANGES = {"Today", "Last 7 Days", "Last 30 Days", "This Month"};
    private static final int NEAR_EXPIRY_DAYS = 7;
    private static final int DEAD_STOCK_DAYS = 30;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");

    private final ReportService reportService;

    private final JComboBox<String> rangeCombo = new JComboBox<>(RANGES);
    private final JLabel lastUpdated = new JLabel(" ");
    private final JButton refreshButton = Styles.createPrimaryButton("Refresh");
    private final JButton exportCsvButton = Styles.createSecondaryButton("Export CSV");
    private final JButton exportImageButton = Styles.createSecondaryButton("Export Image");

    private final KpiTile revenueTile = new KpiTile("Revenue");
    private final KpiTile profitTile = new KpiTile("Gross profit");
    private final KpiTile salesCountTile = new KpiTile("Sales");
    private final KpiTile averageTile = new KpiTile("Average sale");
    private final KpiTile stockValueTile = new KpiTile("Stock value (at cost)");
    private final KpiTile lowStockTile = new KpiTile("Low-stock products");
    private final KpiTile nearExpiryTile = new KpiTile("Near expiry (" + NEAR_EXPIRY_DAYS + " days)");
    private final KpiTile expiredTile = new KpiTile("Expired batches");

    private final LineChartPanel trendChart = new LineChartPanel();
    private final BarChartPanel topProductsChart = new BarChartPanel();
    private final DonutChartPanel paymentChart = new DonutChartPanel();
    private final DonutChartPanel stockHealthChart = new DonutChartPanel();
    private final BarChartPanel wastageChart = new BarChartPanel();
    private final BarChartPanel monthlyChart = new BarChartPanel();
    private final BarChartPanel hourlyChart = new BarChartPanel();

    private final DefaultTableModel expiringModel = model("Product", "Batch", "Qty", "Expires");
    private final DefaultTableModel lowStockModel = model("Product", "In stock", "Reorder at");
    private final DefaultTableModel recentSalesModel = model("Sale", "Time", "Total", "Paid by");
    private final DefaultTableModel deadStockModel = model("Product", "In stock");

    private final JPanel content = new JPanel();
    private Snapshot lastSnapshot;
    private LocalDate rangeFrom = LocalDate.now();
    private LocalDate rangeTo = LocalDate.now();
    private boolean loading;

    public BusinessOverviewPanel(ReportService reportService) {
        if (reportService == null) {
            throw new IllegalArgumentException("ReportService is required");
        }
        this.reportService = reportService;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_WINDOW);
        add(buildToolbar(), BorderLayout.NORTH);
        add(Styles.createScrollPane(buildContent()), BorderLayout.CENTER);

        rangeCombo.setSelectedItem("Last 30 Days");
        rangeCombo.addActionListener(e -> refreshData());
        refreshButton.addActionListener(e -> refreshData());
        exportCsvButton.addActionListener(e -> exportCsv());
        exportImageButton.addActionListener(e -> exportImage());
        refreshData();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG_WINDOW);
        bar.setBorder(BorderFactory.createEmptyBorder(Theme.PAGE_PADDING, Theme.PAGE_PADDING, 8, Theme.PAGE_PADDING));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Business Overview");
        title.setFont(Theme.FONT_PAGE_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        lastUpdated.setFont(Theme.FONT_SMALL);
        lastUpdated.setForeground(Theme.TEXT_HINT);
        titles.add(title);
        titles.add(lastUpdated);
        bar.add(titles, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        Styles.styleComboBox(rangeCombo);
        rangeCombo.setPreferredSize(new Dimension(140, Theme.INPUT_HEIGHT));
        actions.add(rangeCombo);
        actions.add(refreshButton);
        actions.add(exportCsvButton);
        actions.add(exportImageButton);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildContent() {
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BG_WINDOW);
        content.setBorder(BorderFactory.createEmptyBorder(0, Theme.PAGE_PADDING, Theme.PAGE_PADDING, Theme.PAGE_PADDING));

        JPanel kpis = new JPanel(new GridLayout(2, 4, 12, 12));
        kpis.setOpaque(false);
        for (KpiTile t : new KpiTile[] {revenueTile, profitTile, salesCountTile, averageTile,
                stockValueTile, lowStockTile, nearExpiryTile, expiredTile}) {
            kpis.add(t);
        }
        addBlock(kpis);

        JPanel charts = new JPanel(new GridLayout(0, 2, 12, 12));
        charts.setOpaque(false);
        charts.add(trendChart);
        charts.add(topProductsChart);
        charts.add(paymentChart);
        charts.add(stockHealthChart);
        charts.add(wastageChart);
        charts.add(monthlyChart);
        charts.add(hourlyChart);
        addBlock(charts);

        JPanel tables = new JPanel(new GridLayout(0, 2, 12, 12));
        tables.setOpaque(false);
        tables.add(tableCard("Expiring soon", expiringModel));
        tables.add(tableCard("Needs reordering", lowStockModel));
        tables.add(tableCard("Recent sales", recentSalesModel));
        tables.add(tableCard("Not sold in " + DEAD_STOCK_DAYS + " days", deadStockModel));
        addBlock(tables);
        return content;
    }

    private void addBlock(JPanel block) {
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(block);
        content.add(Box.createVerticalStrut(12));
    }

    private JPanel tableCard(String title, DefaultTableModel model) {
        JPanel card = Styles.createCard();
        card.setLayout(new BorderLayout(0, 8));
        card.add(Styles.createSectionTitle(title), BorderLayout.NORTH);
        JTable table = new JTable(model);
        Styles.styleTable(table);
        table.setEnabled(false);
        if (model.getColumnName(0).equals("Sale")) {
            table.getColumnModel().getColumn(0).setPreferredWidth(210);
        }
        JScrollPane scroll = Styles.createScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(300, 190));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // ------------------------------------------------------------------
    // Loading data (background thread -> Swing thread)
    // ------------------------------------------------------------------

    @Override
    public void refreshData() {
        if (loading) {
            return;
        }
        loading = true;
        refreshButton.setEnabled(false);
        final String selected = (String) rangeCombo.getSelectedItem();
        final LocalDate today = LocalDate.now();
        final LocalDate from = rangeStart(selected, today);
        rangeFrom = from;
        rangeTo = today;

        new SwingWorker<Snapshot, Void>() {
            @Override
            protected Snapshot doInBackground() {
                return loadSnapshot(from, today);
            }

            @Override
            protected void done() {
                loading = false;
                refreshButton.setEnabled(true);
                try {
                    lastSnapshot = get();
                    apply(lastSnapshot, selected);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(BusinessOverviewPanel.this,
                            "Could not load the overview: " + rootMessage(ex),
                            "Business Overview", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static LocalDate rangeStart(String selected, LocalDate today) {
        if ("Today".equals(selected)) {
            return today;
        }
        if ("Last 7 Days".equals(selected)) {
            return today.minusDays(6);
        }
        if ("This Month".equals(selected)) {
            return today.withDayOfMonth(1);
        }
        return today.minusDays(29);
    }

    private Snapshot loadSnapshot(LocalDate from, LocalDate to) {
        reportService.refreshData();
        Snapshot s = new Snapshot();
        s.from = from;
        s.to = to;
        s.revenue = reportService.getRevenueForRange(from, to);
        s.profit = reportService.getGrossProfitForRange(from, to);
        s.average = reportService.getAverageSaleValue(from, to);
        s.salesCount = countSales(from, to);
        s.stockValue = reportService.getStockValueAtCost();
        s.lowStock = reportService.getLowStockCount();
        s.nearExpiry = reportService.getNearExpiryBatchCount(NEAR_EXPIRY_DAYS);
        s.expired = reportService.getExpiredBatchCount();

        LocalDate trendFrom = from.isAfter(to.minusDays(6)) ? to.minusDays(6) : from;
        s.trendFrom = trendFrom;
        s.dailySales = reportService.getDailySales(trendFrom, to);
        s.topProducts = reportService.getTopProducts(from, to, 5);
        s.byPayment = reportService.getSalesByPaymentMethod(from, to);
        s.health = reportService.getStockHealth(to, NEAR_EXPIRY_DAYS);
        s.wastage = reportService.getWastageByReason(from, to);
        s.monthly = reportService.getMonthlySalesAndProfit(6);
        s.byHour = reportService.getSalesByHour(from, to);

        s.reorder = reportService.getReorderList();
        s.recentSales = recentSales(10);
        s.dead = reportService.getDeadStock(DEAD_STOCK_DAYS);
        s.expiring = expiringBatches();
        s.loadedAt = LocalDateTime.now();
        return s;
    }

    private int countSales(LocalDate from, LocalDate to) {
        int n = 0;
        for (Sale sale : reportService.getSalesHistory()) {
            LocalDate d = sale.getSaleDateTime().toLocalDate();
            if (!d.isBefore(from) && !d.isAfter(to)
                    && sale.getPaymentStatus() == shelflife.model.PaymentStatus.PAID) {
                n++;
            }
        }
        return n;
    }

    private List<Sale> recentSales(int limit) {
        List<Sale> all = new ArrayList<>(reportService.getSalesHistory());
        all.sort((a, b) -> b.getSaleDateTime().compareTo(a.getSaleDateTime()));
        return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
    }

    private List<ProductBatch> expiringBatches() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(NEAR_EXPIRY_DAYS);
        List<ProductBatch> list = new ArrayList<>();
        for (ProductBatch b : reportService.getStockInformation()) {
            if (b.getQuantity() > 0 && !b.isRecalled() && !b.getExpiryDate().isBefore(today)
                    && !b.getExpiryDate().isAfter(limit)) {
                list.add(b);
            }
        }
        list.sort((a, b) -> a.getExpiryDate().compareTo(b.getExpiryDate()));
        return list;
    }

    // ------------------------------------------------------------------
    // Showing data
    // ------------------------------------------------------------------

    private void apply(Snapshot s, String rangeName) {
        revenueTile.set(CurrencyUtil.format(s.revenue), rangeName);
        profitTile.set(CurrencyUtil.format(s.profit), marginText(s));
        salesCountTile.set(String.valueOf(s.salesCount), rangeName);
        averageTile.set(CurrencyUtil.format(s.average), "per paid sale");
        stockValueTile.set(CurrencyUtil.format(s.stockValue), "on-hand units x cost");
        lowStockTile.set(String.valueOf(s.lowStock), "products at or below threshold");
        nearExpiryTile.set(String.valueOf(s.nearExpiry), "batches");
        expiredTile.set(String.valueOf(s.expired), "batches");

        List<String> days = new ArrayList<>();
        List<BigDecimal> dayValues = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : s.dailySales.entrySet()) {
            days.add(e.getKey().format(DAY));
            dayValues.add(e.getValue());
        }
        trendChart.setData("Daily sales", s.trendFrom.format(DAY) + " - " + s.to.format(DAY), days, dayValues);

        List<String> names = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        for (ProductSales p : s.topProducts) {
            names.add(p.getProductName());
            revenues.add(p.getRevenue());
        }
        topProductsChart.setData("Top products by revenue", rangeName, names, revenues);

        paymentChart.setData("Payment methods", rangeName,
                new ArrayList<>(s.byPayment.keySet()), new ArrayList<>(s.byPayment.values()));
        paymentChart.setCenterCaption("PKR");

        stockHealthChart.setData("Stock health", "units on hand",
                java.util.Arrays.asList("Good", "Near expiry", "Expired / recalled"),
                java.util.Arrays.asList(BigDecimal.valueOf(s.health.getGoodQuantity()),
                        BigDecimal.valueOf(s.health.getNearExpiryQuantity()),
                        BigDecimal.valueOf(s.health.getExpiredQuantity())),
                java.util.Arrays.asList(Color.decode("#10B981"), Color.decode("#F59E0B"), Color.decode("#EF4444")));
        stockHealthChart.setCenterCaption("units");

        wastageChart.setData("Wastage by reason", rangeName + " (units)",
                new ArrayList<>(s.wastage.keySet()), new ArrayList<>(s.wastage.values()));

        List<String> months = new ArrayList<>();
        List<BigDecimal> monthRevenue = new ArrayList<>();
        List<BigDecimal> monthProfit = new ArrayList<>();
        for (MonthlyRow r : s.monthly) {
            months.add(r.getMonthLabel());
            monthRevenue.add(r.getRevenue());
            monthProfit.add(r.getProfit().max(BigDecimal.ZERO));
        }
        monthlyChart.setGroupedData("Revenue vs profit", "last 6 months", months,
                monthRevenue, monthProfit, "Revenue", "Profit");

        List<String> hours = new ArrayList<>();
        List<BigDecimal> hourValues = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hours.add(String.format("%02d", h));
            BigDecimal v = s.byHour.get(h);
            hourValues.add(v == null ? BigDecimal.ZERO : v);
        }
        hourlyChart.setData("Sales by hour of day", rangeName, hours, hourValues);

        fill(expiringModel, s.expiring.size(), i -> {
            ProductBatch b = s.expiring.get(i);
            Product p = reportService.getProductById(b.getProductId());
            return new Object[] {p == null ? b.getProductId() : p.getName(), b.getBatchId(),
                    b.getQuantity(), b.getExpiryDate().toString()};
        });
        fill(lowStockModel, s.reorder.size(), i -> {
            Product p = s.reorder.get(i);
            return new Object[] {p.getName(), stockOf(p), p.getLowStockThreshold()};
        });
        fill(recentSalesModel, s.recentSales.size(), i -> {
            Sale sale = s.recentSales.get(i);
            return new Object[] {sale.getSaleId(), sale.getSaleDateTime().format(TIME),
                    CurrencyUtil.format(sale.getTotalAmount()),
                    sale.getPaymentMethod() == null ? "-" : sale.getPaymentMethod().getDisplayName()};
        });
        fill(deadStockModel, s.dead.size(), i -> {
            Product p = s.dead.get(i);
            return new Object[] {p.getName(), stockOf(p)};
        });

        lastUpdated.setText("Updated " + s.loadedAt.format(TIME) + "  |  "
                + s.from.format(DAY) + " - " + s.to.format(DAY));
        revalidate();
        repaint();
    }

    private int stockOf(Product p) {
        int total = 0;
        for (ProductBatch b : reportService.getStockInformation()) {
            if (p.getProductId().equals(b.getProductId())) {
                total += b.getQuantity();
            }
        }
        return total;
    }

    private static String marginText(Snapshot s) {
        if (s.revenue.signum() <= 0) {
            return "no sales yet";
        }
        BigDecimal margin = s.profit.multiply(BigDecimal.valueOf(100))
                .divide(s.revenue, 1, java.math.RoundingMode.HALF_UP);
        return margin.toPlainString() + "% margin";
    }

    private interface RowBuilder {
        Object[] row(int index);
    }

    private static void fill(DefaultTableModel model, int count, RowBuilder builder) {
        model.setRowCount(0);
        for (int i = 0; i < count; i++) {
            model.addRow(builder.row(i));
        }
    }

    // ------------------------------------------------------------------
    // Exports
    // ------------------------------------------------------------------

    private void exportCsv() {
        if (lastSnapshot == null) {
            JOptionPane.showMessageDialog(this, "Nothing to export yet. Please refresh first.",
                    "Export CSV", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("business_overview_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                Files.newOutputStream(target.toPath()), StandardCharsets.UTF_8))) {
            out.print('\uFEFF');
            Snapshot s = lastSnapshot;
            out.println("Business overview," + csv(s.from + " to " + s.to));
            out.println("Revenue," + s.revenue.toPlainString());
            out.println("Gross profit," + s.profit.toPlainString());
            out.println("Paid sales," + s.salesCount);
            out.println("Average sale," + s.average.toPlainString());
            out.println("Stock value at cost," + s.stockValue.toPlainString());
            out.println();
            out.println("Date,Sales");
            for (Map.Entry<LocalDate, BigDecimal> e : s.dailySales.entrySet()) {
                out.println(e.getKey() + "," + e.getValue().toPlainString());
            }
            out.println();
            out.println("Product,Units sold,Revenue,Profit");
            for (ProductSales p : s.topProducts) {
                out.println(csv(p.getProductName()) + "," + p.getQuantity() + ","
                        + p.getRevenue().toPlainString() + "," + p.getProfit().toPlainString());
            }
            out.flush();
            JOptionPane.showMessageDialog(this, "Saved to " + target.getAbsolutePath(),
                    "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the file: " + ex.getMessage(),
                    "Export CSV", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("business_overview_" + LocalDate.now() + ".png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        try {
            Dimension size = content.getSize();
            BufferedImage image = new BufferedImage(Math.max(1, size.width), Math.max(1, size.height),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            try {
                g2.setColor(Theme.BG_WINDOW);
                g2.fillRect(0, 0, image.getWidth(), image.getHeight());
                content.paint(g2);
            } finally {
                g2.dispose();
            }
            ImageIO.write(image, "png", target);
            JOptionPane.showMessageDialog(this, "Saved to " + target.getAbsolutePath(),
                    "Export Image", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save the image: " + ex.getMessage(),
                    "Export Image", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() == null ? cur.getClass().getSimpleName() : cur.getMessage();
    }

    // ------------------------------------------------------------------
    // Small helper types
    // ------------------------------------------------------------------

    /** Everything the screen shows, gathered off the Swing thread. */
    private static final class Snapshot {
        LocalDate from;
        LocalDate to;
        LocalDate trendFrom;
        LocalDateTime loadedAt;
        BigDecimal revenue;
        BigDecimal profit;
        BigDecimal average;
        BigDecimal stockValue;
        int salesCount;
        int lowStock;
        int nearExpiry;
        int expired;
        Map<LocalDate, BigDecimal> dailySales;
        List<ProductSales> topProducts;
        Map<String, BigDecimal> byPayment;
        StockHealth health;
        Map<String, BigDecimal> wastage;
        List<MonthlyRow> monthly;
        Map<Integer, BigDecimal> byHour;
        List<Product> reorder;
        List<Sale> recentSales;
        List<Product> dead;
        List<ProductBatch> expiring;
    }

    /** One number with a caption, shown as a card. */
    private static final class KpiTile extends JPanel {
        private final JLabel value = new JLabel("-");
        private final JLabel hint = new JLabel(" ");

        KpiTile(String title) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new Styles.RoundedBorder(Theme.BORDER_LIGHT, 1, Theme.CORNER_RADIUS),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            JLabel caption = new JLabel(title);
            caption.setFont(Theme.FONT_SUBTITLE);
            caption.setForeground(Theme.TEXT_SECONDARY);
            value.setFont(Theme.FONT_PAGE_TITLE);
            value.setForeground(Theme.TEXT_PRIMARY);
            hint.setFont(Theme.FONT_SMALL);
            hint.setForeground(Theme.TEXT_HINT);
            add(caption);
            add(Box.createVerticalStrut(4));
            add(value);
            add(Box.createVerticalStrut(2));
            add(hint);
        }

        void set(String text, String note) {
            value.setText(text);
            hint.setText(note == null || note.isEmpty() ? " " : note);
        }
    }
}
