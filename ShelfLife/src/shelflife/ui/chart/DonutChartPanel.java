package shelflife.ui.chart;

import shelflife.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * Donut chart panel with a center label, legend, and segment hover tooltips.
 */
public class DonutChartPanel extends JPanel {

    private String title;
    private String subtitle;
    private List<String> names;
    private List<BigDecimal> values;
    private List<Color> colors;
    private double total;

    public DonutChartPanel() {
        setPreferredSize(new Dimension(300, 300));
        setMinimumSize(new Dimension(200, 200));
        setMaximumSize(new Dimension(600, 600));
        setBackground(Theme.BG_CARD);
        setBorder(new RoundedBorder(Theme.BORDER_LIGHT, 1, Theme.CORNER_RADIUS));
    }

    public void setData(String title, String subtitle, List<String> names, List<BigDecimal> values, List<Color> colors) {
        this.title = title;
        this.subtitle = subtitle;
        this.names = names;
        this.values = values;
        this.colors = colors;
        calculateTotal();
        repaint();
    }

    public void clear() {
        this.title = null;
        this.subtitle = null;
        this.names = null;
        this.values = null;
        this.colors = null;
        this.total = 0;
        repaint();
    }

    private void calculateTotal() {
        total = BigDecimal.ZERO == null ? 0 : 0;
        if (values != null) {
            for (BigDecimal v : values) {
                if (v != null) total = total.add(v);
            }
        }
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
        int size = Math.min(w, h) - 40; // chart size with margin

        if (names == null || values == null || names.isEmpty()) {
            // Empty state
            g2.setColor(Theme.TEXT_HINT);
            g2.setFont(Theme.FONT_SUBTITLE);
            String msg = "No data for this period.";
            int msgW = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (w - msgW) / 2, h / 2);
            g2.dispose();
            return;
        }

        int centerX = w / 2;
        int centerY = h / 2;
        int radius = size / 2;

        // Calculate total and draw donut segments
        double totalValue = total > 0 ? total.doubleValue() : 1;
        double startAngle = -Math.PI / 2; // start at top

        for (int i = 0; i < names.size(); i++) {
            BigDecimal value = values.get(i);
            double sliceValue = value != null && value.compareTo(BigDecimal.ZERO) > 0
                    ? value.doubleValue() : 0;
            double slicePercent = sliceValue / totalValue;
            double endAngle = startAngle + slicePercent * 2 * Math.PI;

            // Determine color
            Color segmentColor = colors != null && i < colors.size() ? colors.get(i) : Theme.ACCENT_PRIMARY;

            // Draw filled segment
            g2.setColor(segmentColor);
            Arc2D.Double arc = new Arc2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2,
                    toDegrees(startAngle), toDegrees(slicePercent * 360), Arc2D.PIE);
            g2.fill(arc);

            // Draw center hole
            g2.setColor(Theme.BG_CARD);
            g2.fillOval(centerX - radius / 2, centerY - radius / 2, radius / 2, radius / 2);

            // Draw label outside the donut
            double labelAngle = startAngle + slicePercent * Math.PI;
            double labelRadius = radius + 30;
            int labelX = centerX + (int) (labelRadius * Math.cos(labelAngle));
            int labelY = centerY + (int) (labelRadius * Math.sin(labelAngle));

            // Don't draw label if it would be off-panel
            if (labelX >= 0 && labelX <= w && labelY >= 0 && labelY <= h) {
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.setFont(Theme.FONT_SMALL);
                String labelText = names.get(i);
                // Add value and percent
                String valueText = value != null && value.compareTo(BigDecimal.ZERO) > 0
                        ? value.stripTrailingZeros().toPlainString() + " ("
                        : "0 (";
                String percentText = " " + String.format("%.1f", slicePercent * 100) + "%)";
                String fullText = valueText + percentText;

                int textW = g2.getFontMetrics().stringWidth(fullText);
                // Center the text on the label position
                g2.drawString(fullText, labelX - textW / 2, labelY);
            }

            startAngle = endAngle;
        }

        // Draw title below chart
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_SECTION);
        String titleText = title != null ? title : "Distribution";
        int titleW = g2.getFontMetrics().stringWidth(titleText);
        g2.drawString(titleText, (w - titleW) / 2, h - 20);

        // Draw subtitle
        if (subtitle != null) {
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.setFont(Theme.FONT_SUBTITLE);
            g2.drawString(subtitle, (w - g2.getFontMetrics().stringWidth(subtitle)) / 2, h - 5);
        }

        g2.dispose();
    }

    private double toDegrees(double radians) {
        return radians * 180 / Math.PI;
    }
}