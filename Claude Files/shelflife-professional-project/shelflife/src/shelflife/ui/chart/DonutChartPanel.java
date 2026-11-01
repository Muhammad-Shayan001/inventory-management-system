package shelflife.ui.chart;

import shelflife.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Donut chart with the total in the centre and a legend that shows
 * each segment's share. Segment colours default to the shared palette.
 */
public class DonutChartPanel extends AbstractChartPanel {

    private List<String> names = Collections.emptyList();
    private List<BigDecimal> values = Collections.emptyList();
    private List<Color> colors = Collections.emptyList();
    private String centerCaption = "Total";

    public DonutChartPanel() {
        super(320, 260);
    }

    public void setData(String title, String subtitle, List<String> names, List<BigDecimal> values) {
        setData(title, subtitle, names, values, null);
    }

    /** colors may be null (palette is used) or one colour per segment. */
    public void setData(String title, String subtitle, List<String> names,
                        List<BigDecimal> values, List<Color> colors) {
        setHeader(title, subtitle);
        this.names = names == null ? new ArrayList<String>() : new ArrayList<String>(names);
        this.values = values == null ? new ArrayList<BigDecimal>() : new ArrayList<BigDecimal>(values);
        this.colors = colors == null ? new ArrayList<Color>() : new ArrayList<Color>(colors);
        repaint();
    }

    public void setCenterCaption(String caption) {
        this.centerCaption = caption == null ? "" : caption;
        repaint();
    }

    private double total() {
        double t = 0;
        for (BigDecimal v : values) {
            t += v == null ? 0 : Math.max(0, v.doubleValue());
        }
        return t;
    }

    @Override
    protected boolean isEmpty() {
        return names.isEmpty() || values.size() != names.size() || total() <= 0;
    }

    private Color colorAt(int i) {
        return i < colors.size() && colors.get(i) != null ? colors.get(i) : PALETTE[i % PALETTE.length];
    }

    @Override
    protected void paintBody(Graphics2D g2, int x, int y, int w, int h) {
        double total = total();
        int diameter = Math.min(h, (int) (w * 0.52));
        int ringX = x + 4;
        int ringY = y + (h - diameter) / 2;
        float thickness = Math.max(14f, diameter * 0.2f);
        double inset = thickness / 2.0;

        double start = 90;
        g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        for (int i = 0; i < values.size(); i++) {
            double v = values.get(i) == null ? 0 : Math.max(0, values.get(i).doubleValue());
            if (v <= 0) {
                continue;
            }
            double extent = -360.0 * v / total;
            g2.setColor(colorAt(i));
            g2.draw(new Arc2D.Double(ringX + inset, ringY + inset, diameter - thickness, diameter - thickness,
                    start, extent, Arc2D.OPEN));
            start += extent;
        }
        g2.setStroke(new BasicStroke(1f));

        // centre label
        String totalText = compact(BigDecimal.valueOf(total));
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.FONT_SECTION);
        FontMetrics fm = g2.getFontMetrics();
        int cx = ringX + diameter / 2;
        int cy = ringY + diameter / 2;
        g2.drawString(totalText, cx - fm.stringWidth(totalText) / 2, cy + 2);
        g2.setFont(small());
        g2.setColor(Theme.TEXT_HINT);
        FontMetrics sm = g2.getFontMetrics();
        g2.drawString(centerCaption, cx - sm.stringWidth(centerCaption) / 2, cy + 18);

        // legend
        int legendX = ringX + diameter + 18;
        int legendW = x + w - legendX;
        if (legendW < 60) {
            return;
        }
        int rowH = 22;
        int count = Math.min(names.size(), Math.max(1, h / rowH));
        int legendY = y + (h - count * rowH) / 2;
        g2.setFont(Theme.FONT_SUBTITLE);
        FontMetrics lf = g2.getFontMetrics();
        for (int i = 0; i < count; i++) {
            double v = values.get(i) == null ? 0 : Math.max(0, values.get(i).doubleValue());
            int pct = (int) Math.round(100 * v / total);
            int ry = legendY + i * rowH;
            g2.setColor(colorAt(i));
            g2.fillRoundRect(legendX, ry + 4, 10, 10, 3, 3);
            g2.setColor(Theme.TEXT_PRIMARY);
            String text = fit(lf, names.get(i), legendW - 52) ;
            g2.drawString(text, legendX + 16, ry + 14);
            String share = pct + "%";
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(share, legendX + legendW - lf.stringWidth(share), ry + 14);
        }
    }
}
