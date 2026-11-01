package shelflife.ui.chart;

import shelflife.ui.theme.Theme;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Line chart with a soft fill, grid lines, a few evenly spaced x labels
 * and a tooltip that shows the value of the nearest point.
 */
public class LineChartPanel extends AbstractChartPanel {

    private static final int LEFT_AXIS = 42;
    private static final int BOTTOM_AXIS = 22;

    private List<String> labels = Collections.emptyList();
    private List<BigDecimal> values = Collections.emptyList();

    private int plotX;
    private int plotW;

    public LineChartPanel() {
        super(420, 260);
        setToolTipText("");
    }

    public void setData(String title, String subtitle, List<String> labels, List<BigDecimal> values) {
        setHeader(title, subtitle);
        this.labels = labels == null ? new ArrayList<String>() : new ArrayList<String>(labels);
        this.values = values == null ? new ArrayList<BigDecimal>() : new ArrayList<BigDecimal>(values);
        repaint();
    }

    @Override
    protected boolean isEmpty() {
        if (values.size() < 2 || labels.size() != values.size()) {
            return true;
        }
        for (BigDecimal v : values) {
            if (v != null && v.signum() > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void paintBody(Graphics2D g2, int x, int y, int w, int h) {
        double max = 0;
        for (BigDecimal v : values) {
            max = Math.max(max, v == null ? 0 : v.doubleValue());
        }
        double top = niceMax(max);
        int chartH = h - BOTTOM_AXIS;
        int baseY = y + chartH;
        plotX = x + LEFT_AXIS;
        plotW = w - LEFT_AXIS - 6;

        g2.setFont(small());
        FontMetrics fm = g2.getFontMetrics();

        int ticks = 4;
        for (int i = 0; i <= ticks; i++) {
            int gy = baseY - (int) Math.round(chartH * i / (double) ticks);
            g2.setColor(i == 0 ? Theme.BORDER_LIGHT : Theme.BG_TABLE_ROW_ALT);
            g2.drawLine(plotX, gy, plotX + plotW, gy);
            g2.setColor(Theme.TEXT_HINT);
            String label = compact(BigDecimal.valueOf(top * i / ticks));
            g2.drawString(label, plotX - 6 - fm.stringWidth(label), gy + 4);
        }

        int n = values.size();
        Path2D.Double line = new Path2D.Double();
        double firstX = 0;
        double lastX = 0;
        for (int i = 0; i < n; i++) {
            double px = plotX + plotW * i / (double) (n - 1);
            double v = values.get(i) == null ? 0 : values.get(i).doubleValue();
            double py = baseY - chartH * v / top;
            if (i == 0) {
                line.moveTo(px, py);
                firstX = px;
            } else {
                line.lineTo(px, py);
            }
            lastX = px;
        }

        Path2D.Double area = new Path2D.Double(line);
        area.lineTo(lastX, baseY);
        area.lineTo(firstX, baseY);
        area.closePath();
        java.awt.Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
        g2.setColor(Theme.ACCENT_PRIMARY);
        g2.fill(area);
        g2.setComposite(old);

        g2.setColor(Theme.ACCENT_PRIMARY);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);
        g2.setStroke(new BasicStroke(1f));

        int labelCount = Math.min(6, n);
        for (int k = 0; k < labelCount; k++) {
            int i = labelCount == 1 ? 0 : (int) Math.round(k * (n - 1) / (double) (labelCount - 1));
            String label = labels.get(i);
            int lx = (int) (plotX + plotW * i / (double) (n - 1));
            int tw = fm.stringWidth(label);
            int drawX = Math.max(plotX - 10, Math.min(lx - tw / 2, plotX + plotW - tw));
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(label, drawX, baseY + 15);
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (values.size() < 2 || plotW <= 0) {
            return null;
        }
        double ratio = (e.getX() - plotX) / (double) plotW;
        int i = (int) Math.round(ratio * (values.size() - 1));
        if (i < 0 || i >= values.size()) {
            return null;
        }
        BigDecimal v = values.get(i) == null ? BigDecimal.ZERO : values.get(i);
        return labels.get(i) + ": " + v.stripTrailingZeros().toPlainString();
    }
}
