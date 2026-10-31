package shelflife.ui;

import shelflife.ui.theme.Styles;
import shelflife.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Unified UITheme Facade.
 * Bridges legacy calls directly to the central Theme and Styles framework.
 */
public final class UITheme {

    // --- Color Palette ---
    public static final Color PRIMARY = Theme.ACCENT_PRIMARY;
    public static final Color PRIMARY_HOVER = Theme.ACCENT_PRIMARY_HOVER;
    public static final Color PRIMARY_DARK = Theme.ACCENT_PRIMARY_DARK;

    public static final Color HEADER_BG = Theme.BG_HEADER_NAVY;
    public static final Color SIDEBAR_BG = Theme.BG_SIDEBAR;
    public static final Color SIDEBAR_BTN_BG = Theme.BG_SIDEBAR;
    public static final Color SIDEBAR_BTN_HOVER = Theme.BG_SIDEBAR_HOVER;
    public static final Color SIDEBAR_BTN_ACTIVE = Theme.BG_SIDEBAR_SELECTED;

    public static final Color BG_PAGE = Theme.BG_WINDOW;
    public static final Color CARD_BG = Theme.BG_CARD;
    public static final Color BORDER = Theme.BORDER_LIGHT;
    public static final Color BORDER_DARK = Theme.BORDER_LIGHT;

    public static final Color TEXT_MAIN = Theme.TEXT_PRIMARY;
    public static final Color TEXT_MUTED = Theme.TEXT_SECONDARY;
    public static final Color TEXT_LIGHT = Theme.FG_SIDEBAR_TEXT;

    public static final Color SUCCESS = Theme.COLOR_SUCCESS_FG;
    public static final Color WARNING = Theme.COLOR_WARNING_FG;
    public static final Color DANGER = Theme.COLOR_DANGER_FG;

    public static final Color TABLE_HEADER_BG = Theme.BG_TABLE_HEADER;
    public static final Color TABLE_ROW_ALT = Theme.BG_TABLE_ROW_ALT;
    public static final Color TABLE_SELECTION_BG = Theme.BG_TABLE_SELECTED;
    public static final Color TABLE_SELECTION_FG = Theme.FG_TABLE_SELECTED_TEXT;

    // --- Typography ---
    public static final Font FONT_APP_TITLE = Theme.FONT_APP_TITLE;
    public static final Font FONT_PAGE_TITLE = Theme.FONT_PAGE_TITLE;
    public static final Font FONT_SECTION = Theme.FONT_SECTION;
    public static final Font FONT_SUBTITLE = Theme.FONT_SUBTITLE;
    public static final Font FONT_REGULAR = Theme.FONT_REGULAR;
    public static final Font FONT_BOLD = Theme.FONT_BOLD;
    public static final Font FONT_SMALL = Theme.FONT_SMALL;
    public static final Font FONT_KPI_VAL = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_KPI_TITLE = Theme.FONT_BOLD;
    public static final Font FONT_TABLE_HEADER = Theme.FONT_TABLE_HEADER;
    public static final Font FONT_TABLE_CELL = Theme.FONT_TABLE_CELL;

    private UITheme() {}

    public static void initLookAndFeel() {
        Theme.initLookAndFeel();
    }

    public static void styleTable(JTable table) {
        Styles.styleTable(table);
    }

    public static JButton createPrimaryButton(String text) {
        return Styles.createPrimaryButton(text);
    }

    public static JButton createSecondaryButton(String text) {
        return Styles.createSecondaryButton(text);
    }

    public static JButton createDangerButton(String text) {
        return Styles.createDangerButton(text);
    }

    public static JButton createSuccessButton(String text) {
        return Styles.createSuccessButton(text);
    }

    public static void styleTextField(JTextField tf) {
        Styles.styleTextField(tf);
    }

    public static void styleComboBox(JComboBox<?> cb) {
        Styles.styleComboBox(cb);
    }

    public static void styleSpinner(JSpinner sp) {
        Styles.styleSpinner(sp);
    }

    public static JPanel createCard() {
        return Styles.createCard();
    }

    public static JScrollPane createScrollPane(java.awt.Component view) {
        return Styles.createScrollPane(view);
    }

    /**
     * Creates a standard page header panel with title and subtitle labels.
     * Used by BillingPanel and other module panels.
     */
    public static JPanel createPageHeader(String title, String subtitle) {
        JPanel header = new JPanel(new java.awt.BorderLayout(10, 4));
        header.setBackground(Theme.BG_WINDOW);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_LIGHT),
                new javax.swing.border.EmptyBorder(12, 0, 12, 0)
        ));

        JPanel titlePanel = new JPanel(new java.awt.GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(Theme.FONT_SUBTITLE);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        header.add(titlePanel, java.awt.BorderLayout.WEST);
        return header;
    }
}
