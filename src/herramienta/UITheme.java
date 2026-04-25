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

    public static boolean isDark = false;

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

    // ── Fonts (rebuilt by rebuildFonts) ──────────────────────────────────────
    public static Font FONT_BASE  = new Font("SansSerif", Font.PLAIN, 13);
    public static Font FONT_BOLD  = new Font("SansSerif", Font.BOLD,  13);
    public static Font FONT_LABEL = new Font("SansSerif", Font.BOLD,  12);
    public static Font FONT_TITLE = new Font("SansSerif", Font.BOLD,  18);
    public static Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    static { setDark(false); }

    // ── Theme switching ───────────────────────────────────────────────────────
    public static void setDark(boolean dark) {
        isDark = dark;
        SIDEBAR_BG       = dark ? new Color(0x1A1510) : new Color(0x2A2520);
        SIDEBAR_ACCENT   = new Color(0xC48F45);
        SIDEBAR_TEXT     = dark ? new Color(0xE0D8D0) : new Color(0xE8E0D8);
        SIDEBAR_TEXT_MID = dark ? new Color(0x8B7E74) : new Color(0x9B8E84);
        SIDEBAR_SEL_BG   = dark ? new Color(0x2A2010) : new Color(0x3D3028);
        BG_MAIN          = dark ? D_BG_MAIN       : L_BG_MAIN;
        BG_PANEL         = dark ? D_BG_PANEL      : L_BG_PANEL;
        ACCENT           = dark ? D_ACCENT        : L_ACCENT;
        ACCENT_ALT       = dark ? D_ACCENT_ALT    : L_ACCENT_ALT;
        TEXT_DARK        = dark ? D_TEXT_DARK     : L_TEXT_DARK;
        TEXT_MID         = dark ? D_TEXT_MID      : L_TEXT_MID;
        BORDER_CLR       = dark ? D_BORDER_CLR    : L_BORDER_CLR;
        HEADER_BG        = dark ? D_HEADER_BG     : L_HEADER_BG;
        HEADER_FG        = dark ? D_HEADER_FG     : L_HEADER_FG;
        TABLE_GRID       = dark ? D_TABLE_GRID    : L_TABLE_GRID;
        TABLE_ALT        = dark ? D_TABLE_ALT     : L_TABLE_ALT;
        SECONDARY_BTN_BG = dark ? D_SECONDARY_BTN : L_SECONDARY_BTN;
        FIELD_BG         = dark ? D_FIELD_BG      : L_FIELD_BG;
        NIMBUS_BLUE_GREY = dark ? D_NIMBUS_BG     : L_NIMBUS_BG;
        applyUIManager();
    }

    public static void rebuildFonts(String size) {
        int sz = "small".equals(size) ? 13 : "large".equals(size) ? 20 : 16;
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
            return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
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
