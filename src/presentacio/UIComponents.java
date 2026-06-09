package presentacio;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

import herramienta.UITheme;

/**
 * presentacio-layer factory for theme-aware Swing components.
 *
 * <p>All presentacio code should call into this class instead of touching
 * {@link UITheme} styling fields directly, so that swapping to a different
 * look-and-feel does not require touching every panel/control.
 *
 * <p>The styling bodies (button / label / panel / field / cardBorder) were
 * moved here from {@code herramienta.UITheme} during the session-11 build
 * fix so {@code UITheme} only owns the theme state (colors, fonts,
 * UIManager keys) and {@code UIComponents} owns the panel-layer style
 * application.  Field/colors that drive the look still come from
 * {@link UITheme}; the methods here just apply them.
 */
public final class UIComponents {

    private UIComponents() {}

    public static void styleAccentButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(UITheme.fontBold());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.SECONDARY_BTN_BG);
        btn.setForeground(Color.WHITE);
        btn.setFont(UITheme.fontBold());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSidebarButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.SIDEBAR_BG);
        btn.setForeground(UITheme.SIDEBAR_TEXT);
        btn.setFont(UITheme.fontBase());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleLabel(JLabel lbl) {
        lbl.setFont(UITheme.FONT_LABEL);
        lbl.setForeground(UITheme.TEXT_MID);
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(UITheme.BG_PANEL);
    }

    public static void styleField(JTextField field) {
        field.setFont(UITheme.fontBase());
        field.setForeground(UITheme.TEXT_DARK);
        field.setBackground(UITheme.FIELD_BG);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_CLR),
            BorderFactory.createEmptyBorder(3, 7, 3, 7)
        ));
        if (field.getClientProperty("hoverInstalled") == null) {
            field.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (field.isEnabled())
                        field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UITheme.ACCENT, 1),
                            BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER_CLR),
                        BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
            });
            field.putClientProperty("hoverInstalled", Boolean.TRUE);
        }
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_CLR),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        );
    }
}
