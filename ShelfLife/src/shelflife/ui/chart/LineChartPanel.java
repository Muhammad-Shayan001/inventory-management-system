package shelflife.ui.chart;

import shelflife.ui.theme.Theme;
import shelflife.ui.theme.Styles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Line chart panel with smooth antialiased rendering, grid lines,
 * and optional fill under the line.
 */
public class LineChartPanel extends JPanel {

    private List<LocalDate> xValues;
    private List<BigDecimal> yValues;
    private String title;
    private String subtitle;
    private LocalDate fromDate;
    private LocalDate toDate;

    public LineChartPanel() {
        setPreferredSize(new Dimension(400, 250));
        setMinimumSize(new Dimension(300, 200));
        setMaximumSize(new Dimension(800, 400));
        setBackground(Theme.BG_CARD);
        setBorder(new RoundedBorder(Theme.BORDER_LIGHT, 1, Theme.CORNER_RADIUS));
    }

    public void setData(String title, String subtitle, LocalDate fromDate, LocalDate toDate, List<LocalDate> xValues, List<BigDecimal> yValues) {
        this.title = title;
        this.subtitle = subtitle;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.xValues = xValues;
        this.yValues = yValues;
        repaint();
    }

    public void clear() {
        this.xValues = null;
        this.yValues = null;
        this.title = null;
        this.subtitle = null;
        this.fromDate = null;
        this.toDate = null;
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
        int plotW = w - insets.left - insets.right - 40;
        int plotH = h - insets.top - insets.bottom - 40;

        if (xValues == null || yValues == null || xValues.isEmpty() || yValues.isEmpty()) {
            // Empty state
            g2.setColor(Theme.TEXT_HINT);
            g2.setFont(Theme.FONT_SUBTITLE);
            String msg = "No data for this period.";
            int msgW = g2.getFontMetrics().stringWidth(msg);
            g2.drawString(msg, (w - msgW) / 2, h / 2);
            g2.dispose();
            return;
        }

        // Determine scales
        BigDecimal minY = yValues.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxY = yValues.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // Add some padding
        BigDecimal range = maxY.subtract(minY);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            range = BigDecimal.ONE;
        }
        BigDecimal minPadded = minY.subtract(range.divide(new BigDecimal("4"), 2, BigDecimal.ROUND_HALF_UP));
        BigDecimal maxPadded = maxY.add(range.divide(new BigDecimal("4"), 2, BigDecimal.ROUND_HALF_UP));

        // Calculate scales
        double yScale = (double) plotH / range.stripTrailingZeros().doubleValue();
        double yOffset = minPadded.stripTrailingZeros().doubleValue() * yScale;

        // X scale
        int xStep = Math.max(1, xValues.size() / plotW);
        double xScale = (double) plotW / (xValues.size() - 1);

        // Draw title
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_SECTION);
        String titleText = title != null ? title : "Sales Trend";
        int titleW = g2.getFontMetrics().stringWidth(titleText);
        g2.drawString(titleText, (w - titleW) / 2, insets.top + 20);

        // Draw subtitle/date range
        if (subtitle != null) {
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.setFont(Theme.FONT_SUBTITLE);
            int subW = g2.getFontMetrics().stringWidth(subtitleText);
            g2.drawString(subtitleText, (w - subW) / 2, insets.top + 40);
        }

        // Draw Y axis (left side)
        drawYAxis(g2, plotH, plotW, minPadded, maxPadded);

        // Draw X axis (bottom)
        drawXAxis(g2, plotW, plotH, xStep, xScale);

        // Draw grid lines and data line
        g2.setColor(new Color(233, 236, 239)); // light grid color
        g2.setStroke(new BasicStroke(1.0f));

        // Find max/min X for clipping
        int startIdx = 0;
        int endIdx = xValues.size() - 1;

        // Plot the line connecting points
        if (yValues.size() > 1) {
            // Draw fill under the line (light color)
            Path2D.Float fillPath = new Path2D.Float();
            // Start at bottom left
            double firstX = 20 + xStep / 2;
            double firstY = h - insets.bottom - 20;
            if (yValues.get(0) != null) {
                double plotY0 = plotH - (yValues.get(0).stripTrailingZeros().doubleValue() - minPadded.stripTrailingZeros().doubleValue()) * yScale;
                fillPath.moveTo(firstX, plotY0 + insets.bottom + 20);
            }

            boolean firstPoint = true;
            for (int i = startIdx; i <= endIdx; i++) {
                if (yValues.get(i) == null) continue;

                double x = 20 + (i - startIdx) * xScale;
                double yValue = yValues.get(i).stripTrailingZeros().doubleValue();
                double plotY = plotH - (yValue - minPadded.stripTrailingZeros().doubleValue()) * yScale;

                if (firstPoint) {
                    fillPath.lineTo(x, plotY + insets.bottom + 20);
                    firstPoint = false;
                }

                if (i > startIdx) {
                    // Draw connecting line segment
                    g2.setColor(new Color(180, 190, 210, 200)); // light accent
                    g2.setStroke(new BasicStroke(2.0f));
                    double prevX = 20 + ((i - 1 - startIdx) * xScale);
                    double prevY = plotH - ((yValues.get(i - 1).stripTrailingZeros().doubleValue() - minPadded.stripTrailingZeros().doubleValue()) * yScale);
                    g2.drawLine((int) prevX, (int) prevY, (int) x, (int) plotY);
                }

                // Draw point marker
                g2.setColor(Theme.ACCENT_PRIMARY);
                g2.setStroke(new BasicStroke(1.0f));
                g2.fill(new Ellipse2D.Double(x - 4, plotY - 4, 8, 8));

                if (firstPoint) {
                    fillPath.moveTo(x, plotY + insets.bottom + 20);
                    firstPoint = false;
                }
            }

            // Fill under the line
            if (!firstPoint) {
                g2.setColor(new Color(220, 230, 241, 180)); // light blue fill
                g2.fill(fillPath);
            }
        }

        // Draw Y axis labels
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setFont(Theme.FONT_SMALL);
        int numYTicks = 5;
        for (int i = 0; i <= numYTicks; i++) {
            double yValue = minPadded.stripTrailingZeros().doubleValue() + (range.stripTrailingZeros().doubleValue() / numYTicks) * i;
            String label = formatBigDecimal(yValue);
            int labelW = g2.getFontMetrics().stringWidth(label);
            int xLabel = insets.left + 5 - labelW;
            int yLabel = insets.top + 20 + (i * plotH / numYTicks) + g2.getFontMetrics().getAscent();
            g2.drawString(label, xLabel, yLabel);
        }

        g2.dispose();
    }

    private String formatBigDecimal(BigDecimal val) {
        if (val == null) return "0.00";
        // Format with 2 decimal places, strip unnecessary zeros
        String s = val.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        return s;
    }

    private String subtitleText() {
        if (subtitle != null && !subtitle.isEmpty()) return subtitle;
        if (fromDate != null && toDate != null) {
            return fromDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) + " - " + toDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
        }
        return "";
    }

    private void drawYAxis(Graphics2D g2, int plotH, int plotW, BigDecimal minPadded, BigDecimal maxPadded) {
        g2.setColor(Theme.BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1.0f));

        // Y axis line
        g2.drawLine(20, insets.top + 20, 20, getHeight() - insets.bottom - 20);

        // Y axis arrow
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_HINT);
        String label = "Rs.";
        int labelW = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, 5, insets.top + 15);
    }

    private void drawXAxis(Graphics2D g2, int plotW, int plotH, int xStep, double xScale) {
        g2.setColor(Theme.BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1.0f));

        // X axis line
        g2.drawLine(20, getHeight() - insets.bottom - 20, getWidth() - 20, getHeight() - insets.bottom - 20);

        // X axis labels
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.setFont(Theme.FONT_SMALL);
        for (int i = 0; i < xValues.size(); i += xStep) {
            if (i >= xValues.size()) break;
            String label = xValues.get(i).format(java.time.format.DateTimeFormatter.ofPattern("dd"));
            int labelW = g2.getFontMetrics().stringWidth(label);
            int x = 20 + (int) (i * xScale);
            int y = getHeight() - insets.bottom - 5;
            g2.drawString(label, x - labelW / 2, y);
        }

        // X axis label
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_HINT);
        String label = "Date";
        int labelW = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, getWidth() - 20 - labelW, getHeight() - insets.bottom + 20);
    }
}

/**
 * Custom rounded border for chart panels.
 */
class RoundedBorder implements Border {
    private final Color color;
    private final int thickness;
    private final int radius;

    public RoundedBorder(Color color, int thickness, int radius) {
        this.color = color;
        this.thickness = thickness;
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        g2.draw(new RoundRectangle2D.Float(x + thickness / 2.0f, y + thickness / 2.0f, width - thickness, height - thickness, radius, radius));
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}