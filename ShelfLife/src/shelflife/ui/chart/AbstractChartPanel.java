package shelflife.ui.chart;

import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared base for the lightweight Java2D charts (no external libraries).
 * Handles the card look, the title block, the empty state and PNG export.
 */
abstract class AbstractChartPanel extends JPanel {

    /** Colour sequence used for categories and donut segments. */
    static final Color[] PALETTE = {
            Color.decode("#2563EB"), Color.decode("#10B981"), Color.decode("#F59E0B"),
            Color.decode("#EF4444"), Color.decode("#8B5CF6"), Color.decode("#06B6D4"),
            Color.decode("#EC4899"), Color.decode("#84CC16")
    };

    protected static final int HEADER_HEIGHT = 52;

    private String title = "";
    private String subtitle = "";

    protected AbstractChartPanel(int prefWidth, int prefHeight) {
        setOpaque(true);
        setBackground(Theme.BG_CARD);
        setBorder(new Styles.RoundedBorder(Theme.BORDER_LIGHT, 1, Theme.CORNER_RADIUS));
        setPreferredSize(new Dimension(prefWidth, prefHeight));
        setMinimumSize(new Dimension(240, 180));
    }

    protected final void setHeader(String title, String subtitle) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
    }

    /** True when there is nothing to draw. Subclasses decide what "empty" means. */
    protected abstract boolean isEmpty();

    /** Draws the chart body inside the given rectangle (below the header). */
    protected abstract void paintBody(Graphics2D g2, int x, int y, int w, int h);

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Insets in = getInsets();
            int x = in.left + 12;
            int y = in.top + 8;
            int w = getWidth() - in.left - in.right - 24;
            int h = getHeight() - in.top - in.bottom - 16;

            g2.setColor(Theme.TEXT_PRIMARY);
            g2.setFont(Theme.FONT_BOLD);
            g2.drawString(title, x, y + 14);
            if (!subtitle.isEmpty()) {
                g2.setColor(Theme.TEXT_HINT);
                g2.setFont(Theme.FONT_SMALL);
                g2.drawString(subtitle, x, y + 30);
            }

            int bodyY = y + HEADER_HEIGHT - 8;
            int bodyH = h - (HEADER_HEIGHT - 8);
            if (w < 40 || bodyH < 40) {
                return;
            }
            if (isEmpty()) {
                drawCentered(g2, "No data for this period", x, bodyY, w, bodyH);
            } else {
                paintBody(g2, x, bodyY, w, bodyH);
            }
        } finally {
            g2.dispose();
        }
    }

    /** Renders the panel to an image (used by "Export image"). */
    public BufferedImage toImage() {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            paint(g2);
        } finally {
            g2.dispose();
        }
        return img;
    }

    // ---- helpers shared by the charts ----

    protected static void drawCentered(Graphics2D g2, String text, int x, int y, int w, int h) {
        g2.setColor(Theme.TEXT_HINT);
        g2.setFont(Theme.FONT_SUBTITLE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + h / 2);
    }

    /** 1,250 -> "1.3K", 2,400,000 -> "2.4M", 85 -> "85". */
    static String compact(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        BigDecimal abs = v.abs();
        if (abs.compareTo(new BigDecimal("1000000")) >= 0) {
            return v.divide(new BigDecimal("1000000"), 1, RoundingMode.HALF_UP).toPlainString() + "M";
        }
        if (abs.compareTo(new BigDecimal("1000")) >= 0) {
            return v.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP).toPlainString() + "K";
        }
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /** Shortens text with "..." so it fits in maxWidth pixels. */
    static String fit(FontMetrics fm, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 1 && fm.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    /** Rounds the maximum up to a "nice" axis limit (1, 2, 2.5, 5 x 10^n). */
    static double niceMax(double max) {
        if (max <= 0) {
            return 1;
        }
        double exp = Math.pow(10, Math.floor(Math.log10(max)));
        double f = max / exp;
        double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
        return nice * exp;
    }

    protected static Font small() {
        return Theme.FONT_SMALL;
    }
}
