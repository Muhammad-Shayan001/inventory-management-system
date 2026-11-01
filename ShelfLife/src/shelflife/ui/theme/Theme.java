package shelflife.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * Central Enterprise Design System for ShelfLife.
 * Strictly adheres to UI Repair Specifications:
 * - Exactly defined color palette with high contrast ratios
 * - 8px corner radius consistency across all boxes, cards, inputs, and buttons
 * - Segoe UI typography hierarchy
 * - 34px table row height, 38px table header height
 * - 8px grid spacing system
 */
public final class Theme {

    // === Color Palette (Section 6) ===
    public static final Color BG_WINDOW = Color.decode("#F4F6FA");
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_HEADER_NAVY = Color.decode("#14213D");
    public static final Color BG_SIDEBAR = Color.decode("#14213D");
    public static final Color FG_SIDEBAR_TEXT = Color.decode("#E5E9F2");
    public static final Color BG_SIDEBAR_HOVER = Color.decode("#1F3055");
    public static final Color BG_SIDEBAR_SELECTED = Color.decode("#2563EB");
    public static final Color FG_SIDEBAR_SELECTED_TEXT = Color.WHITE;

    public static final Color ACCENT_PRIMARY = Color.decode("#2563EB");
    public static final Color ACCENT_PRIMARY_HOVER = Color.decode("#1D4ED8");
    public static final Color ACCENT_PRIMARY_PRESSED = Color.decode("#1E40AF");
    public static final Color ACCENT_PRIMARY_DARK = Color.decode("#1E3A8A");

    public static final Color TEXT_PRIMARY = Color.decode("#111827");
    public static final Color TEXT_SECONDARY = Color.decode("#4B5563");
    public static final Color TEXT_HINT = Color.decode("#6B7280");
    public static final Color BORDER_LIGHT = Color.decode("#D1D5DB");
    public static final Color BORDER_FOCUS = Color.decode("#2563EB");

    public static final Color BG_TABLE_HEADER = Color.decode("#E8EDF5");
    public static final Color FG_TABLE_HEADER_TEXT = Color.decode("#111827");
    public static final Color BG_TABLE_ROW_ALT = Color.decode("#F8FAFC");
    public static final Color TABLE_ROW_ALT = BG_TABLE_ROW_ALT;
    public static final Color BG_TABLE_SELECTED = Color.decode("#DBEAFE");
    public static final Color FG_TABLE_SELECTED_TEXT = Color.decode("#1E3A8A");

    public static final Color COLOR_SUCCESS_BG = Color.decode("#DCFCE7");
    public static final Color COLOR_SUCCESS_FG = Color.decode("#166534");
    public static final Color COLOR_WARNING_BG = Color.decode("#FEF3C7");
    public static final Color COLOR_WARNING_FG = Color.decode("#92400E");
    public static final Color COLOR_DANGER_BG = Color.decode("#FEE2E2");
    public static final Color COLOR_DANGER_FG = Color.decode("#991B1B");
    public static final Color COLOR_DISABLED_BG = Color.decode("#F3F4F6");
    public static final Color COLOR_DISABLED_FG = Color.decode("#9CA3AF");
    public static final Color FG_ERROR = Color.decode("#991B1B");

    // === Typography (Section 8) ===
    public static final Font FONT_APP_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_PAGE_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_HINT = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TABLE_CELL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_RECEIPT = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_MONOSPACED = new Font("Monospaced", Font.PLAIN, 13);
    public static final Font FONT_DIALOG = FONT_REGULAR;

    // === Spacing & Dimensions (Section 7) ===
    public static final int CORNER_RADIUS = 8;
    public static final int BORDER_RADIUS = 8;
    public static final int BUTTON_HEIGHT = 36;
    public static final int INPUT_HEIGHT = 36;
    public static final int TABLE_ROW_HEIGHT = 34;
    public static final int TABLE_HEADER_HEIGHT = 38;
    public static final int SIDEBAR_WIDTH = 230;
    public static final int SIDEBAR_ITEM_HEIGHT = 44;
    public static final int PAGE_PADDING = 24;

    public static final int SPACING_4 = 4;
    public static final int SPACING_8 = 8;
    public static final int SPACING_12 = 12;
    public static final int SPACING_16 = 16;
    public static final int SPACING_24 = 24;
    public static final int SPACING_32 = 32;

    private Theme() {}

    /**
     * Installs global look-and-feel and UIManager defaults once at application launch.
     */
    public static void initLookAndFeel() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        applyDefaults();
    }

    public static void applyDefaults() {
        // Backgrounds
        UIManager.put("Panel.background", BG_WINDOW);
        UIManager.put("OptionPane.background", BG_WINDOW);
        UIManager.put("Dialog.background", BG_WINDOW);
        UIManager.put("Frame.background", BG_WINDOW);
        UIManager.put("Viewport.background", BG_WINDOW);
        UIManager.put("ScrollPane.background", BG_WINDOW);

        // Fonts
        UIManager.put("Label.font", FONT_REGULAR);
        UIManager.put("Button.font", FONT_BOLD);
        UIManager.put("TextField.font", FONT_REGULAR);
        UIManager.put("TextArea.font", FONT_REGULAR);
        UIManager.put("PasswordField.font", FONT_REGULAR);
        UIManager.put("ComboBox.font", FONT_REGULAR);
        UIManager.put("Table.font", FONT_TABLE_CELL);
        UIManager.put("TableHeader.font", FONT_TABLE_HEADER);
        UIManager.put("TabbedPane.font", FONT_BOLD);
        UIManager.put("Spinner.font", FONT_REGULAR);
        UIManager.put("Menu.font", FONT_REGULAR);
        UIManager.put("MenuItem.font", FONT_REGULAR);
        UIManager.put("ToolTip.font", FONT_SMALL);

        // Foregrounds
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("Spinner.foreground", TEXT_PRIMARY);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("TableHeader.foreground", FG_TABLE_HEADER_TEXT);
        UIManager.put("Button.foreground", TEXT_PRIMARY);

        // Table
        UIManager.put("Table.rowHeight", TABLE_ROW_HEIGHT);
        UIManager.put("Table.selectionBackground", BG_TABLE_SELECTED);
        UIManager.put("Table.selectionForeground", FG_TABLE_SELECTED_TEXT);
        UIManager.put("Table.gridColor", new Color(229, 231, 235));
        UIManager.put("TableHeader.background", BG_TABLE_HEADER);
        UIManager.put("TableHeader.cellBorder", BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        // Text selections
        UIManager.put("TextField.selectionBackground", BG_TABLE_SELECTED);
        UIManager.put("TextField.selectionForeground", FG_TABLE_SELECTED_TEXT);
        UIManager.put("TextArea.selectionBackground", BG_TABLE_SELECTED);
        UIManager.put("TextArea.selectionForeground", FG_TABLE_SELECTED_TEXT);

        // TabbedPane
        UIManager.put("TabbedPane.background", BG_WINDOW);
        UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(8, 0, 0, 0));
        UIManager.put("TabbedPane.tabInsets", new Insets(7, 14, 7, 14));

        // Suppress rectangular focus/border boxes on JLabel and components (Windows LAF)
        UIManager.put("Label.border", BorderFactory.createEmptyBorder());
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.focusColor", new Color(0, 0, 0, 0));
        UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(1, 1, 1, 1));
        UIManager.put("ScrollBar.width", 8);
    }
}
