package shelflife.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Enterprise UI Component Styler for ShelfLife.
 * Implements consistent 8px corner radii, high-contrast readable states,
 * standard 36px control heights, and clean layout patterns.
 */
public final class Styles {

    private Styles() {}

    // =========================================================================
    // Button Components (Primary, Secondary, Danger)
    // =========================================================================

    public enum ButtonType {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS
    }

    /**
     * Custom JButton that self-paints with an exact 8px corner radius,
     * smooth antialiasing, and guaranteed contrast across all interactive states.
     */
    public static class RoundedButton extends JButton {
        private final ButtonType type;
        private boolean isHovered = false;
        private boolean isPressed = false;

        public RoundedButton(String text, ButtonType type) {
            super(text);
            this.type = type;
            setFont(Theme.FONT_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(getPreferredSize().width, Theme.BUTTON_HEIGHT));
            setMinimumSize(new Dimension(80, Theme.BUTTON_HEIGHT));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width + 24, 88), Theme.BUTTON_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();

            Color bg;
            Color fg;
            Color border;

            if (!enabled) {
                bg = Theme.COLOR_DISABLED_BG;
                fg = Theme.COLOR_DISABLED_FG;
                border = Theme.BORDER_LIGHT;
            } else if (type == ButtonType.PRIMARY) {
                bg = isPressed ? Theme.ACCENT_PRIMARY_PRESSED : (isHovered ? Theme.ACCENT_PRIMARY_HOVER : Theme.ACCENT_PRIMARY);
                fg = Color.WHITE;
                border = bg;
            } else if (type == ButtonType.DANGER) {
                bg = isPressed ? new Color(153, 27, 27) : (isHovered ? new Color(185, 28, 28) : new Color(220, 38, 38));
                fg = Color.WHITE;
                border = bg;
            } else if (type == ButtonType.SUCCESS) {
                bg = isPressed ? new Color(20, 83, 45) : (isHovered ? new Color(22, 101, 52) : new Color(21, 128, 61));
                fg = Color.WHITE;
                border = bg;
            } else { // SECONDARY
                bg = isPressed ? new Color(226, 232, 240) : (isHovered ? new Color(241, 245, 249) : Color.WHITE);
                fg = Theme.TEXT_PRIMARY;
                border = isHovered ? Theme.ACCENT_PRIMARY : new Color(203, 213, 225);
            }

            // Fill rounded background
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));

            // Draw border
            g2.setColor(border);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 8, 8));

            // Focus ring (2px blue)
            if (hasFocus() && enabled) {
                g2.setColor(Theme.ACCENT_PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 7, 7));
            }

            // Text
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(fg);
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    public static JButton createPrimaryButton(String text) {
        return new RoundedButton(text, ButtonType.PRIMARY);
    }

    public static JButton createSecondaryButton(String text) {
        return new RoundedButton(text, ButtonType.SECONDARY);
    }

    public static JButton createDangerButton(String text) {
        return new RoundedButton(text, ButtonType.DANGER);
    }

    public static JButton createSuccessButton(String text) {
        return new RoundedButton(text, ButtonType.SUCCESS);
    }

    // =========================================================================
    // Text Inputs, Combos & Spinners
    // =========================================================================

    public static void styleTextField(JTextField tf) {
        if (tf == null) return;
        tf.setFont(Theme.FONT_REGULAR);
        tf.setForeground(Theme.TEXT_PRIMARY);
        tf.setBackground(Theme.BG_CARD);
        tf.setCaretColor(Theme.ACCENT_PRIMARY);
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, Theme.INPUT_HEIGHT));

        Border normalBorder = new CompoundBorder(
                new RoundedBorder(Theme.BORDER_LIGHT, 1, 8),
                new EmptyBorder(6, 10, 6, 10)
        );
        Border focusedBorder = new CompoundBorder(
                new RoundedBorder(Theme.BORDER_FOCUS, 2, 8),
                new EmptyBorder(5, 9, 5, 9)
        );

        tf.setBorder(normalBorder);
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(focusedBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(normalBorder);
            }
        });
    }

    public static void styleComboBox(JComboBox<?> cb) {
        if (cb == null) return;
        cb.setFont(Theme.FONT_REGULAR);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setBackground(Theme.BG_CARD);
        cb.setPreferredSize(new Dimension(cb.getPreferredSize().width, Theme.INPUT_HEIGHT));
        cb.setBorder(new CompoundBorder(
                new RoundedBorder(Theme.BORDER_LIGHT, 1, 8),
                new EmptyBorder(2, 6, 2, 6)
        ));
    }

    public static void styleSpinner(JSpinner sp) {
        if (sp == null) return;
        sp.setFont(Theme.FONT_REGULAR);
        sp.setPreferredSize(new Dimension(sp.getPreferredSize().width, Theme.INPUT_HEIGHT));
        sp.setBorder(new CompoundBorder(
                new RoundedBorder(Theme.BORDER_LIGHT, 1, 8),
                new EmptyBorder(2, 6, 2, 6)
        ));
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(Color.WHITE);
            tf.setForeground(Theme.TEXT_PRIMARY);
            tf.setFont(Theme.FONT_REGULAR);
        }
    }

    // =========================================================================
    // Table Styling (34px rows, 38px headers, zebra striping)
    // =========================================================================

    public static void styleTable(JTable table) {
        if (table == null) return;

        table.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        table.setFont(Theme.FONT_TABLE_CELL);
        table.setShowGrid(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(Theme.BG_TABLE_SELECTED);
        table.setSelectionForeground(Theme.FG_TABLE_SELECTED_TEXT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Header Styling
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setPreferredSize(new Dimension(0, Theme.TABLE_HEADER_HEIGHT));
            header.setFont(Theme.FONT_TABLE_HEADER);
            header.setBackground(Theme.BG_TABLE_HEADER);
            header.setForeground(Theme.FG_TABLE_HEADER_TEXT);
            header.setReorderingAllowed(false);
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                    lbl.setBackground(Theme.BG_TABLE_HEADER);
                    lbl.setForeground(Theme.FG_TABLE_HEADER_TEXT);
                    lbl.setFont(Theme.FONT_TABLE_HEADER);
                    lbl.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                            new EmptyBorder(0, 12, 0, 12)
                    ));
                    return lbl;
                }
            });
        }

        // Row striping & cell padding renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : Theme.BG_TABLE_ROW_ALT);
                    c.setForeground(Theme.TEXT_PRIMARY);
                } else {
                    c.setBackground(Theme.BG_TABLE_SELECTED);
                    c.setForeground(Theme.FG_TABLE_SELECTED_TEXT);
                }
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(new EmptyBorder(0, 12, 0, 12));
                }
                return c;
            }
        });
    }

    // =========================================================================
    // Card Containers & ScrollPanes
    // =========================================================================

    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(Theme.BORDER_LIGHT, 1, 8),
                new EmptyBorder(18, 20, 18, 20)
        ));
        return card;
    }

    public static JScrollPane createScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.BG_WINDOW);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static JLabel createSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SECTION);
        lbl.setForeground(Theme.TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel createSubtitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SUBTITLE);
        lbl.setForeground(Theme.TEXT_SECONDARY);
        return lbl;
    }

    // =========================================================================
    // Helper Custom Rounded Border
    // =========================================================================

    public static class RoundedBorder implements Border {
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
            g2.draw(new RoundRectangle2D.Float(
                    x + thickness / 2.0f,
                    y + thickness / 2.0f,
                    width - thickness,
                    height - thickness,
                    radius,
                    radius
            ));
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
}