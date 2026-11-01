package shelflife.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Non-blocking toast notification banner that slides/pops up in the bottom-right corner
 * and automatically dismisses after 3 seconds without interrupting cashier workflow.
 */
public class ToastNotification extends JWindow {

    public enum Type {
        SUCCESS(new Color(0x166534), new Color(0xDCFCE7), "OK"),
        INFO(new Color(0x1E40AF), new Color(0xDBEAFE), "i"),
        WARNING(new Color(0x92400E), new Color(0xFEF3C7), "!"),
        ERROR(new Color(0x991B1B), new Color(0xFEE2E2), "X");

        final Color fg;
        final Color bg;
        final String icon;

        Type(Color fg, Color bg, String icon) {
            this.fg = fg;
            this.bg = bg;
            this.icon = icon;
        }
    }

    private final Type type;
    private final String message;

    public ToastNotification(Window owner, String message, Type type) {
        super(owner);
        this.message = message;
        this.type = type != null ? type : Type.INFO;

        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0)); // Transparent window background for rounded corners

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(type.bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(type.fg);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 12, 12));
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setLayout(new BorderLayout(10, 0));
        content.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel lblIcon = new JLabel(type.icon);
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblIcon.setForeground(type.fg);

        JLabel lblMsg = new JLabel("<html><b>" + escapeHtml(message) + "</b></html>");
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMsg.setForeground(type.fg);

        content.add(lblIcon, BorderLayout.WEST);
        content.add(lblMsg, BorderLayout.CENTER);

        setContentPane(content);
        pack();

        // Position at bottom-right of owner or screen
        if (owner != null && owner.isVisible()) {
            Point p = owner.getLocationOnScreen();
            int x = p.x + owner.getWidth() - getWidth() - 24;
            int y = p.y + owner.getHeight() - getHeight() - 36;
            setLocation(Math.max(10, x), Math.max(10, y));
        } else {
            Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(scr.width - getWidth() - 24, scr.height - getHeight() - 60);
        }

        // Auto-dismiss after 3000ms
        Timer dismissTimer = new Timer(3000, e -> {
            setVisible(false);
            dispose();
        });
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }

    public static void show(Component parent, String message, Type type) {
        SwingUtilities.invokeLater(() -> {
            Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
            ToastNotification toast = new ToastNotification(owner, message, type);
            toast.setVisible(true);
        });
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
