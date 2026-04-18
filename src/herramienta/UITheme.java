package herramienta;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

public class UITheme {

    public static boolean isDark = false;

    public static Color BG_MAIN    = new Color(0xEEF2F7);
    public static Color BG_PANEL   = new Color(0xFFFFFF);
    public static Color ACCENT     = new Color(0x2471A3);
    public static Color ACCENT_ALT = new Color(0x1A5276);
    public static Color TEXT_DARK  = new Color(0x1C2833);
    public static Color TEXT_MID   = new Color(0x566573);
    public static Color BORDER_CLR = new Color(0xD5D8DC);
    public static Color HEADER_BG  = new Color(0x1C2833);
    public static Color HEADER_FG  = Color.WHITE;
    public static Color TABLE_GRID = new Color(0xE5EBF1);
    public static Color TABLE_ALT  = new Color(0xEBF5FB);

    public static final Font FONT_BASE  = new Font("SansSerif", Font.PLAIN,  13);
    public static final Font FONT_BOLD  = new Font("SansSerif", Font.BOLD,   13);
    public static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD,   12);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD,   18);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN,  11);

    // Updates all color fields and Nimbus UIManager properties for the chosen mode
    public static void setDark(boolean dark) {
        isDark = dark;
        if (dark) {
            BG_MAIN    = new Color(0x2B2B2B);
            BG_PANEL   = new Color(0x3C3F41);
            ACCENT     = new Color(0x589DF6);
            ACCENT_ALT = new Color(0x3A6DB5);
            TEXT_DARK  = new Color(0xCCCCCC);
            TEXT_MID   = new Color(0x999999);
            BORDER_CLR = new Color(0x565656);
            HEADER_BG  = new Color(0x1E1E1E);
            HEADER_FG  = new Color(0xCCCCCC);
            TABLE_GRID = new Color(0x4A4A4A);
            TABLE_ALT  = new Color(0x353535);
        } else {
            BG_MAIN    = new Color(0xEEF2F7);
            BG_PANEL   = new Color(0xFFFFFF);
            ACCENT     = new Color(0x2471A3);
            ACCENT_ALT = new Color(0x1A5276);
            TEXT_DARK  = new Color(0x1C2833);
            TEXT_MID   = new Color(0x566573);
            BORDER_CLR = new Color(0xD5D8DC);
            HEADER_BG  = new Color(0x1C2833);
            HEADER_FG  = Color.WHITE;
            TABLE_GRID = new Color(0xE5EBF1);
            TABLE_ALT  = new Color(0xEBF5FB);
        }
        UIManager.put("control",                    BG_MAIN);
        UIManager.put("text",                       TEXT_DARK);
        UIManager.put("nimbusBase",                 ACCENT);
        UIManager.put("nimbusBlueGrey",             isDark ? new Color(0x3D4451) : new Color(0x5D8AA8));
        UIManager.put("nimbusFocus",                ACCENT);
        UIManager.put("nimbusSelectionBackground",  ACCENT);
        UIManager.put("nimbusSelectedText",         Color.WHITE);
        UIManager.put("Table.alternateRowColor",    TABLE_ALT);
    }

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
        btn.setBackground(isDark ? new Color(0x3D4451) : new Color(0xABB2B9));
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleLabel(JLabel lbl) {
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MID);
    }

    public static void styleField(JTextField field) {
        field.setFont(FONT_BASE);
        field.setForeground(TEXT_DARK);
        field.setBackground(isDark ? new Color(0x45474A) : new Color(0xFDFEFF));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR),
            BorderFactory.createEmptyBorder(3, 7, 3, 7)
        ));
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        );
    }
}
