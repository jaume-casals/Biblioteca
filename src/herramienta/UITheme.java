package herramienta;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;

public class UITheme {

    private UITheme() {}

    public enum Theme {
        LIGHT(false),
        DARK( true),
        SEPIA(false),
        OCEAN(false);

        public final boolean dark;
        Theme(boolean dark) { this.dark = dark; }

        public String displayName() { return I18n.t("theme_" + name().toLowerCase()); }

        public String key() { return name().toLowerCase(); }

        public static Theme fromKey(String key) {
            if (key == null) return LIGHT;
            for (Theme t : values()) if (t.name().equalsIgnoreCase(key)) return t;
            return LIGHT;
        }
    }

    private static Theme currentTheme = Theme.LIGHT;

    public static Theme getTheme() { return currentTheme; }
    public static boolean isDark() { return currentTheme.dark; }

    // ── Palette (updated by setDark) ─────────────────────────────────────────
    public static Color BG_MAIN;
    public static Color BG_PANEL;
    public static Color ACCENT;
    public static Color ACCENT_ALT;
    public static Color TEXT_DARK;
    public static Color TEXT_MID;
    public static Color BORDER_CLR;
    public static Color HEADER_BG;
    public static Color HEADER_FG;
    public static Color TABLE_GRID;
    public static Color TABLE_ALT;

    // Derived — used internally by style methods
    static Color SECONDARY_BTN_BG;
    static Color FIELD_BG;
    static Color NIMBUS_BLUE_GREY;

    // ── Fixed colors (theme-independent) ─────────────────────────────────────
    public static final Color DANGER = new Color(0xC0392B);
    public static final Color GREEN  = new Color(0x117A65);

    // ── Sidebar colors (updated by setDark) ───────────────────────────────────
    public static Color SIDEBAR_BG;
    public static Color SIDEBAR_ACCENT;
    public static Color SIDEBAR_TEXT;
    public static Color SIDEBAR_TEXT_MID;
    public static Color SIDEBAR_SEL_BG;

    // ── Light palette (warm library theme) ───────────────────────────────────
    private static final Color L_BG_MAIN        = new Color(0xF5F3EE);
    private static final Color L_BG_PANEL       = new Color(0xFFFFFF);
    private static final Color L_ACCENT         = new Color(0x9B5E1A);
    private static final Color L_ACCENT_ALT     = new Color(0x7A4A14);
    private static final Color L_TEXT_DARK      = new Color(0x2C1810);
    private static final Color L_TEXT_MID       = new Color(0x7A6E66);
    private static final Color L_BORDER_CLR     = new Color(0xE5E0D8);
    private static final Color L_HEADER_BG      = new Color(0x2A2520);
    private static final Color L_HEADER_FG      = new Color(0xE8E0D8);
    private static final Color L_TABLE_GRID     = new Color(0xEDE8E1);
    private static final Color L_TABLE_ALT      = new Color(0xFAF8F5);
    private static final Color L_SECONDARY_BTN  = new Color(0xB8A89A);
    private static final Color L_FIELD_BG       = new Color(0xFDFCFA);
    private static final Color L_NIMBUS_BG      = new Color(0x9B5E1A);

    // ── Dark palette ──────────────────────────────────────────────────────────
    private static final Color D_BG_MAIN        = new Color(0x2B2B2B);
    private static final Color D_BG_PANEL       = new Color(0x3C3F41);
    private static final Color D_ACCENT         = new Color(0x589DF6);
    private static final Color D_ACCENT_ALT     = new Color(0x3A6DB5);
    private static final Color D_TEXT_DARK      = new Color(0xCCCCCC);
    private static final Color D_TEXT_MID       = new Color(0x999999);
    private static final Color D_BORDER_CLR     = new Color(0x565656);
    private static final Color D_HEADER_BG      = new Color(0x1E1E1E);
    private static final Color D_HEADER_FG      = new Color(0xCCCCCC);
    private static final Color D_TABLE_GRID     = new Color(0x4A4A4A);
    private static final Color D_TABLE_ALT      = new Color(0x353535);
    private static final Color D_SECONDARY_BTN  = new Color(0x3D4451);
    private static final Color D_FIELD_BG       = new Color(0x45474A);
    private static final Color D_NIMBUS_BG      = new Color(0x3D4451);

    // ── Sepia palette (warm parchment) ────────────────────────────────────────
    private static final Color SP_BG_MAIN       = new Color(0xF5EDD3);
    private static final Color SP_BG_PANEL      = new Color(0xFFF9F0);
    private static final Color SP_ACCENT        = new Color(0x8B4513);
    private static final Color SP_ACCENT_ALT    = new Color(0x6B3410);
    private static final Color SP_TEXT_DARK     = new Color(0x2C1A0E);
    private static final Color SP_TEXT_MID      = new Color(0x7A6050);
    private static final Color SP_BORDER_CLR    = new Color(0xDDD0B8);
    private static final Color SP_HEADER_BG     = new Color(0x3A2010);
    private static final Color SP_HEADER_FG     = new Color(0xF0E8D8);
    private static final Color SP_TABLE_GRID    = new Color(0xE8D8C0);
    private static final Color SP_TABLE_ALT     = new Color(0xFAF3E8);
    private static final Color SP_SECONDARY_BTN = new Color(0xB89878);
    private static final Color SP_FIELD_BG      = new Color(0xFFFDF5);
    private static final Color SP_NIMBUS_BG     = new Color(0x8B4513);

    // ── Ocean palette (cool blue-teal) ────────────────────────────────────────
    private static final Color OC_BG_MAIN       = new Color(0xEBF4F8);
    private static final Color OC_BG_PANEL      = new Color(0xFFFFFF);
    private static final Color OC_ACCENT        = new Color(0x0077A8);
    private static final Color OC_ACCENT_ALT    = new Color(0x005A80);
    private static final Color OC_TEXT_DARK     = new Color(0x0A2840);
    private static final Color OC_TEXT_MID      = new Color(0x4A7090);
    private static final Color OC_BORDER_CLR    = new Color(0xBDD8E8);
    private static final Color OC_HEADER_BG     = new Color(0x082030);
    private static final Color OC_HEADER_FG     = new Color(0xD0EAF8);
    private static final Color OC_TABLE_GRID    = new Color(0xCCE4F0);
    private static final Color OC_TABLE_ALT     = new Color(0xF2F9FD);
    private static final Color OC_SECONDARY_BTN = new Color(0x6898B8);
    private static final Color OC_FIELD_BG      = new Color(0xF5FBFF);
    private static final Color OC_NIMBUS_BG     = new Color(0x0077A8);

    // ── Fonts (rebuilt by rebuildFonts) ──────────────────────────────────────
    public static Font FONT_BASE  = new Font("SansSerif", Font.PLAIN, 13);
    public static Font FONT_BOLD  = new Font("SansSerif", Font.BOLD,  13);
    public static Font FONT_LABEL = new Font("SansSerif", Font.BOLD,  12);
    public static Font FONT_TITLE = new Font("SansSerif", Font.BOLD,  18);
    public static Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    static { setTheme(Theme.LIGHT); }

    // ── Theme switching ───────────────────────────────────────────────────────
    public static void setTheme(Theme t) {
        if (!java.awt.EventQueue.isDispatchThread() && !java.awt.GraphicsEnvironment.isHeadless())
            System.err.println("[UITheme] setTheme() called off EDT — theme changes must happen on the EDT");
        currentTheme = t;
        switch (t) {
            case DARK:
                SIDEBAR_BG       = new Color(0x1A1510);
                SIDEBAR_ACCENT   = new Color(0xC48F45);
                SIDEBAR_TEXT     = new Color(0xE0D8D0);
                SIDEBAR_TEXT_MID = new Color(0x8B7E74);
                SIDEBAR_SEL_BG   = new Color(0x2A2010);
                BG_MAIN          = D_BG_MAIN;   BG_PANEL    = D_BG_PANEL;
                ACCENT           = D_ACCENT;    ACCENT_ALT  = D_ACCENT_ALT;
                TEXT_DARK        = D_TEXT_DARK; TEXT_MID    = D_TEXT_MID;
                BORDER_CLR       = D_BORDER_CLR;
                HEADER_BG        = D_HEADER_BG; HEADER_FG   = D_HEADER_FG;
                TABLE_GRID       = D_TABLE_GRID; TABLE_ALT  = D_TABLE_ALT;
                SECONDARY_BTN_BG = D_SECONDARY_BTN;
                FIELD_BG         = D_FIELD_BG;  NIMBUS_BLUE_GREY = D_NIMBUS_BG;
                break;
            case SEPIA:
                SIDEBAR_BG       = new Color(0x2A1800);
                SIDEBAR_ACCENT   = new Color(0xD4A040);
                SIDEBAR_TEXT     = new Color(0xF0E8D8);
                SIDEBAR_TEXT_MID = new Color(0x9B8870);
                SIDEBAR_SEL_BG   = new Color(0x4A3020);
                BG_MAIN          = SP_BG_MAIN;  BG_PANEL    = SP_BG_PANEL;
                ACCENT           = SP_ACCENT;   ACCENT_ALT  = SP_ACCENT_ALT;
                TEXT_DARK        = SP_TEXT_DARK; TEXT_MID   = SP_TEXT_MID;
                BORDER_CLR       = SP_BORDER_CLR;
                HEADER_BG        = SP_HEADER_BG; HEADER_FG  = SP_HEADER_FG;
                TABLE_GRID       = SP_TABLE_GRID; TABLE_ALT = SP_TABLE_ALT;
                SECONDARY_BTN_BG = SP_SECONDARY_BTN;
                FIELD_BG         = SP_FIELD_BG; NIMBUS_BLUE_GREY = SP_NIMBUS_BG;
                break;
            case OCEAN:
                SIDEBAR_BG       = new Color(0x071828);
                SIDEBAR_ACCENT   = new Color(0x40C8E0);
                SIDEBAR_TEXT     = new Color(0xC8E8F8);
                SIDEBAR_TEXT_MID = new Color(0x6898B0);
                SIDEBAR_SEL_BG   = new Color(0x102840);
                BG_MAIN          = OC_BG_MAIN;  BG_PANEL    = OC_BG_PANEL;
                ACCENT           = OC_ACCENT;   ACCENT_ALT  = OC_ACCENT_ALT;
                TEXT_DARK        = OC_TEXT_DARK; TEXT_MID   = OC_TEXT_MID;
                BORDER_CLR       = OC_BORDER_CLR;
                HEADER_BG        = OC_HEADER_BG; HEADER_FG  = OC_HEADER_FG;
                TABLE_GRID       = OC_TABLE_GRID; TABLE_ALT = OC_TABLE_ALT;
                SECONDARY_BTN_BG = OC_SECONDARY_BTN;
                FIELD_BG         = OC_FIELD_BG; NIMBUS_BLUE_GREY = OC_NIMBUS_BG;
                break;
            default: // LIGHT
                SIDEBAR_BG       = new Color(0x2A2520);
                SIDEBAR_ACCENT   = new Color(0xC48F45);
                SIDEBAR_TEXT     = new Color(0xE8E0D8);
                SIDEBAR_TEXT_MID = new Color(0x9B8E84);
                SIDEBAR_SEL_BG   = new Color(0x3D3028);
                BG_MAIN          = L_BG_MAIN;   BG_PANEL    = L_BG_PANEL;
                ACCENT           = L_ACCENT;    ACCENT_ALT  = L_ACCENT_ALT;
                TEXT_DARK        = L_TEXT_DARK; TEXT_MID    = L_TEXT_MID;
                BORDER_CLR       = L_BORDER_CLR;
                HEADER_BG        = L_HEADER_BG; HEADER_FG   = L_HEADER_FG;
                TABLE_GRID       = L_TABLE_GRID; TABLE_ALT  = L_TABLE_ALT;
                SECONDARY_BTN_BG = L_SECONDARY_BTN;
                FIELD_BG         = L_FIELD_BG;  NIMBUS_BLUE_GREY = L_NIMBUS_BG;
                break;
        }
        applyUIManager();
    }

    /** Compat — maps to LIGHT or DARK. */
    public static void setDark(boolean dark) {
        setTheme(dark ? Theme.DARK : Theme.LIGHT);
    }

    public static void rebuildFonts(String size) {
        int sz = "small".equals(size) ? 11 : "large".equals(size) ? 16 : 13;
        FONT_BASE  = new Font("SansSerif", Font.PLAIN, sz);
        FONT_BOLD  = new Font("SansSerif", Font.BOLD,  sz);
        FONT_LABEL = new Font("SansSerif", Font.BOLD,  Math.max(9, sz - 1));
        FONT_TITLE = new Font("SansSerif", Font.BOLD,  sz + 5);
        FONT_SMALL = new Font("SansSerif", Font.PLAIN, Math.max(9, sz - 2));
        UIManager.put("defaultFont", FONT_BASE);
    }

    public static void applyUIManager() {
        UIManager.put("control",                   BG_MAIN);
        UIManager.put("text",                      TEXT_DARK);
        UIManager.put("nimbusBase",                ACCENT);
        UIManager.put("nimbusBlueGrey",            NIMBUS_BLUE_GREY);
        UIManager.put("nimbusFocus",               ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("nimbusSelectedText",        Color.WHITE);
        UIManager.put("Table.alternateRowColor",   TABLE_ALT);
        // Prevent Nimbus SynthBorder painter ClassCastException on label-based cell renderers
        UIManager.put("Table.cellNoFocusBorder",      BorderFactory.createEmptyBorder(0, 2, 0, 2));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(0, 2, 0, 2));
    }

    // ── Style helpers ─────────────────────────────────────────────────────────
    public static void styleAccentButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(SECONDARY_BTN_BG);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSidebarButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(SIDEBAR_BG);
        btn.setForeground(SIDEBAR_TEXT);
        btn.setFont(FONT_BASE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleLabel(JLabel lbl) {
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MID);
    }

    public static void styleField(JTextField field) {
        field.setFont(FONT_BASE);
        field.setForeground(TEXT_DARK);
        field.setBackground(FIELD_BG);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR),
            BorderFactory.createEmptyBorder(3, 7, 3, 7)
        ));
        if (field.getClientProperty("hoverInstalled") == null) {
            field.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (field.isEnabled())
                        field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ACCENT, 1),
                            BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_CLR),
                        BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
            });
            field.putClientProperty("hoverInstalled", Boolean.TRUE);
        }
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        );
    }

    public static ImageIcon scaledIcon(byte[] data, int size) {
        if (data == null) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return null;
            return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_FAST));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static File chooseImageFile(Component parent) {
        String dir = Config.getDefaultImgDir();
        JFileChooser chooser = new JFileChooser(new File(dir).exists() ? dir : System.getProperty("user.home"));
        chooser.setFileFilter(new FileNameExtensionFilter("Imatges", "jpg", "jpeg", "png", "gif", "bmp", "webp"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
            ? chooser.getSelectedFile() : null;
    }
}
