package shelflife.ui.chart;

import shelflife.ui.theme.Theme;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vertical bar chart with an optional second series (grouped bars),
 * grid lines, readable axis labels and hover tooltips.
 */
public class BarChartPanel extends AbstractChartPanel {

    private static final int LEFT_AXIS = 42;
    private static final int BOTTOM_AXIS = 22;

    private List<String> names = Collections.emptyList();
    private List<BigDecimal> primary = Collections.emptyList();
    private List<BigDecimal> secondary = Collections.emptyList();
    private String primaryLabel = "";
    private String secondaryLabel = "";

    private int plotX;
    private int plotW;

    public BarChartPanel() {
        super(420, 260);
        setToolTipText("");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setToolTipText(null);
            }
        });
    }

    /** Single series. */
    public void setData(String title, String subtitle, List<String> names, List<BigDecimal> values) {
        setGroupedData(title, subtitle, names, values, null, "", "");
    }

    /** Two series drawn side by side. Pass null for the second to get a single series. */
    public void setGroupedData(String title, String subtitle, List<String> names,
                               List<BigDecimal> first, List<BigDecimal> second,
                               String firstLabel, String secondLabel) {
        setHeader(title, subtitle);
        this.names = names == null ? new ArrayList<String>() : new ArrayList<String>(names);
        this.primary = first == null ? new ArrayList<BigDecimal>() : new ArrayList<BigDecimal>(first);
        this.secondary = second == null ? new ArrayList<BigDecimal>() : new ArrayList<BigDecimal>(second);
        this.primaryLabel = firstLabel == null ? "" : firstLabel;
        this.secondaryLabel = secondLabel == null ? "" : secondLabel;
        repaint();
    }

    public void clear() {
        setData("", "", null, null);
    }

    @Override
    protected boolean isEmpty() {
        if (names.isEmpty() || primary.isEmpty()) {
            return true;
        }
        for (BigDecimal v : primary) {
            if (v != null && v.signum() > 0) {
                return false;
            }
        }
        for (BigDecimal v : secondary) {
            if (v != null && v.signum() > 0) {
                return false;
            }
        }
        return true;
    }

    private boolean grouped() {
        return !secondary.isEmpty();
    }

    @Override
    protected void paintBody(Graphics2D g2, int x, int y, int w, int h) {
        double max = 0;
        for (BigDecimal v : primary) {
            max = Math.max(max, v == null ? 0 : v.doubleValue());
        }
        for (BigDecimal v : secondary) {
            max = Math.max(max, v == null ? 0 : v.doubleValue());
        }
        double top = niceMax(max);

        int legendH = grouped() ? 16 : 0;
        int chartTop = y + legendH;
        int chartH = h - BOTTOM_AXIS - legendH;
        int baseY = chartTop + chartH;
        plotX = x + LEFT_AXIS;
        plotW = w - LEFT_AXIS;

        g2.setFont(small());
        FontMetrics fm = g2.getFontMetrics();

        // grid + y labels
        int ticks = 4;
        for (int i = 0; i <= ticks; i++) {
            int gy = baseY - (int) Math.round(chartH * i / (double) ticks);
            g2.setColor(i == 0 ? Theme.BORDER_LIGHT : Theme.BG_TABLE_ROW_ALT);
            g2.drawLine(plotX, gy, plotX + plotW, gy);
            g2.setColor(Theme.TEXT_HINT);
            String label = compact(BigDecimal.valueOf(top * i / ticks));
            g2.drawString(label, plotX - 6 - fm.stringWidth(label), gy + 4);
        }

        int n = names.size();
        double slot = plotW / (double) n;
        double groupW = Math.min(56, slot * 0.7);
        double barW = grouped() ? groupW / 2 : groupW;
        int labelEvery = Math.max(1, (int) Math.ceil(30 / slot));

        for (int i = 0; i < n; i++) {
            double slotX = plotX + i * slot + (slot - groupW) / 2;
            drawBar(g2, slotX, baseY, chartH, top, valueAt(primary, i), barW, Theme.ACCENT_PRIMARY);
            if (grouped()) {
                drawBar(g2, slotX + barW, baseY, chartH, top, valueAt(secondary, i), barW, PALETTE[1]);
            }
            if (i % labelEvery == 0) {
                g2.setColor(Theme.TEXT_SECONDARY);
                String label = fit(fm, names.get(i), (int) slot * labelEvery - 4);
                int lx = (int) (plotX + i * slot + slot / 2 - fm.stringWidth(label) / 2.0);
                g2.drawString(label, lx, baseY + 15);
            }
        }

        if (grouped()) {
            g2.setColor(Theme.ACCENT_PRIMARY);
            g2.fillRect(plotX, y + 2, 10, 10);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(primaryLabel, plotX + 14, y + 11);
            int second = plotX + 22 + fm.stringWidth(primaryLabel) + 10;
            g2.setColor(PALETTE[1]);
            g2.fillRect(second, y + 2, 10, 10);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(secondaryLabel, second + 14, y + 11);
        }
    }

    private static BigDecimal valueAt(List<BigDecimal> list, int i) {
        return i < list.size() && list.get(i) != null ? list.get(i) : BigDecimal.ZERO;
    }

    private static void drawBar(Graphics2D g2, double x, int baseY, int chartH, double top,
                                BigDecimal value, double width, java.awt.Color color) {
        if (value.signum() <= 0) {
            return;
        }
        int bh = (int) Math.max(2, Math.round(chartH * value.doubleValue() / top));
        g2.setColor(color);
        g2.fillRoundRect((int) x, baseY - bh, (int) Math.max(3, width - 2), bh, 6, 6);
        g2.fillRect((int) x, baseY - Math.min(bh, 6), (int) Math.max(3, width - 2), Math.min(bh, 6));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (names.isEmpty() || plotW <= 0) {
            return null;
        }
        double slot = plotW / (double) names.size();
        int i = (int) ((e.getX() - plotX) / slot);
        if (i < 0 || i >= names.size()) {
            return null;
        }
        String text = names.get(i) + ": " + valueAt(primary, i).stripTrailingZeros().toPlainString();
        if (grouped()) {
            text = names.get(i) + " - " + primaryLabel + ": "
                    + valueAt(primary, i).stripTrailingZeros().toPlainString()
                    + ", " + secondaryLabel + ": "
                    + valueAt(secondary, i).stripTrailingZeros().toPlainString();
        }
        return text;
    }
}
