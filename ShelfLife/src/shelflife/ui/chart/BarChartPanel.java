package shelflife.ui.chart;

import shelflive.ui.theme.Theme;
import shelflive.ui.theme.Styles;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Bar chart panel with vertical, horizontal, and grouped modes.
 */
public class BarChartPanel extends JPanel {

    private enum Mode { VERTICAL, HORIZONTAL, GROUPED }

    private String title;
    private String subtitle;
    private Mode mode;
    private List<String> categoryNames;
    private List<BigDecimal> values;
    private List<BigDecimal> secondaryValues; // for grouped bars
    private List<String> legendLabels;

    public BarChartPanel() {
        setPreferredSize(new Dimension(400, 250));
        setMinimumSize(new Dimension(300, 200));
        setMaximumSize(new Dimension(800, 400));
        setBackground(Theme.BG_CARD);
        setBorder(new RoundedBorder(Theme.BORDER_LIGHT, 1, Theme.CORNER_RADIUS));
    }

    public void setData(String title, String subtitle, Mode mode,
                        List<String> categoryNames, List<BigDecimal> values,
                        List<BigDecimal> secondaryValues, List<String> legendLabels) {
        this.title = title;
        this.subtitle = subtitle;
        this.mode = mode;
        this.categoryNames = categoryNames;
        this.values = values;
        this.secondaryValues = secondaryValues;
        this.legendLabels = legendLabels;
        repaint();
    }

    public void clear() {
        this.mode = null;
        this.categoryNames = null;
        this.values = null;
        this.secondaryValues = null;
        this.legendLabels = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        Insets insets = getInsets();
        int plotW = w - insets.left - insets.right - 30;
        int plotH = h - insets.top - insets.bottom - 40;

        if (mode == null || categoryNames == null || categoryNames.isEmpty()) {
            // Empty state
            g2.setColor(Theme.TEXT_HINT);
            g2.setFont(Theme.FONT_SUBTITLE);
            String msg = "No data for this period.";
            int msgW = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (w - msgW) / 2, h / 2);
            g2.dispose();
            return;
        }

        // Determine Y scale
        BigDecimal maxY = BigDecimal.ZERO;
        if (values != null) {
            for (BigDecimal v : values) {
                if (v != null && v.compareTo(maxY) > 0) maxY = v;
            }
        }
        if (secondaryValues != null) {
            for (BigDecimal v : secondaryValues) {
                if (v != null && v.compareTo(maxY) > 0) maxY = v;
            }
        }

        // Add padding
        BigDecimal range = maxY.multiply(new BigDecimal("1.2"));
        if (maxY.compareTo(BigDecimal.ZERO) == 0) range = BigDecimal.ONE;
        BigDecimal minPadded = BigDecimal.ZERO;

        double yScale = (double) plotH / range.stripTrailingZeros().doubleValue();
        double yOffset = 0; // starting from 0

        // Draw title
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_SECTION);
        String titleText = title != null ? title : "Chart";
        int titleW = g2.getFontMetrics().stringWidth(titleText);
        g2.drawString(titleText, (w - titleW) / 2, insets.top + 20);

        // Draw subtitle
        if (subtitle != null) {
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.setFont(Theme.FONT_SUBTITLE);
            g2.drawString(subtitle, (w - g2.getFontMetrics().stringWidth(subtitle)) / 2, insets.top + 40);
        }

        // Draw bars
        int barCount = categoryNames.size();
        int barWidth = Math.max(20, (plotW - 30) / barCount / 2);
        int barSpacing = 8;

        boolean isGrouped = mode == Mode.GROUPED && secondaryValues != null && secondaryValues.size() == values.size();

        for (int i = 0; i < barCount; i++) {
            BigDecimal value = values != null && i < values.size() ? values.get(i) : null;
            BigDecimal secValue = isGrouped && secondaryValues != null && i < secondaryValues.size() ? secondaryValues.get(i) : null;

            String category = i < categoryNames.size() ? categoryNames.get(i) : "Category " + i;
            String legend = i < legendLabels.size() ? legendLabels.get(i) : "";

            double valueHeight = (value != null && value.compareTo(BigDecimal.ZERO) > 0)
                    ? value.stripTrailingZeros().doubleValue() * yScale
                    : 0;

            double secHeight = (secValue != null && secValue.compareTo(BigDecimal.ZERO) > 0)
                    ? secValue.stripTrailingZeros().doubleValue() * yScale
                    : 0;

            double x = insets.left + 20 + i * (barWidth * 2 + barSpacing);
            double y = getHeight() - insets.bottom - 20 - valueHeight;

            // Determine colors
            Color barColor = Theme.ACCENT_PRIMARY;
            Color secBarColor = Theme.ACCENT_PRIMARY_HOVER;

            // Draw secondary bar (grouped)
            if (isGrouped && secValue != null && secValue.compareTo(BigDecimal.ZERO) > 0) {
                double secX = x + barWidth + barSpacing / 2;
                double secY = getHeight() - insets.bottom - 20 - secHeight;
                g2.setColor(secBarColor);
                g2.fillRoundRect((int) secX, (int) secY, barWidth, (int) secHeight,
                        4, 4); // rounded top corners
            }

            // Draw main bar
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                g2.setColor(barColor);
                g2.fillRoundRect((int) x, (int) y, barWidth, (int) valueHeight,
                        4, 4); // rounded top corners
            }

            // Draw value label above bar
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.setFont(Theme.FONT_SMALL);
                String label = value.stripTrailingZeros().toPlainString();
                int labelW = g2.getFontMetrics().stringWidth(label);
                int labelX = (int) (x + barWidth / 2 - labelW / 2);
                int labelY = (int) (y - 8);
                g2.drawString(label, labelX, labelY);
            }
        }

        // Draw legend (for grouped mode)
        if (isGrouped && legendLabels != null) {
            for (int i = 0; i < legendLabels.size(); i++) {
                String legend = legendLabels.get(i);
                g2.setColor(i == 0 ? Theme.ACCENT_PRIMARY : Theme.ACCENT_PRIMARY_HOVER);
                g2.fillRect(insets.left + 20, insets.top + 60 + i * 20, 16, 16);
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.setFont(Theme.FONT_SMALL);
                int labelW = g2.getFontMetrics().stringWidth(legend);
                g2.drawString(legend, insets.left + 40, insets.top + 65 + i * 20);
            }
        }

        // Draw X axis labels
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setFont(Theme.FONT_SMALL);
        for (int i = 0; i < categoryNames.size(); i++) {
            String label = categoryNames.get(i);
            // Shorten long labels
            if (label.length() > 15) label = label.substring(0, 15) + "...";
            int x = insets.left + 20 + i * (barWidth * 2 + barSpacing) + barWidth / 2;
            int y = getHeight() - insets.bottom - 2;
            int labelW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x - labelW / 2, y);
        }

        // Draw Y axis label
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_HINT);
        String yLabel = "Rs.";
        int labelW = g2.getFontMetrics().stringWidth(yLabel);
        g2.drawString(yLabel, insets.left + 10, insets.top + 15);

        g2.dispose();
    }
}