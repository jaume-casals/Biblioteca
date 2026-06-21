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

import herramienta.ui.UITheme;

/**
 * Fàbrica de la capa de presentacio per a components Swing sensibles al tema.
 *
 * <p>Tot el codi de presentacio hauria de cridar aquesta classe en lloc de
 * tocar directament els camps d'estil de {@link UITheme}, de manera que
 * canviar a un altre look-and-feel no obligui a tocar cada panell/control.
 *
 * <p>Els cossos d'estil (button / label / panel / field / cardBorder) es
 * van moure aquí des de {@code herramienta.ui.UITheme} durant la
 * correcció de build de la sessió 11 perquè {@code UITheme} només
 * posseeixi l'estat del tema (colors, fonts, claus UIManager) i
 * {@code UIComponents} posseeixi l'aplicació d'estils a la capa de
 * panell. Els camps i colors que defineixen l'aspecte continuen venint
 * de {@link UITheme}; els mètodes d'aquí simplement els apliquen.
 */
public final class UIComponents {

    private UIComponents() {}

    public static void styleAccentButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.palette().accent());
        btn.setForeground(Color.WHITE);
        btn.setFont(UITheme.fontBold());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.palette().secondaryBtnBg());
        btn.setForeground(Color.WHITE);
        btn.setFont(UITheme.fontBold());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleDangerButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.palette().danger());
        btn.setForeground(Color.WHITE);
        btn.setFont(UITheme.fontBold());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSidebarButton(JButton btn) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(UITheme.palette().sidebarBg());
        btn.setForeground(UITheme.palette().sidebarText());
        btn.setFont(UITheme.fontBase());
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleLabel(JLabel lbl) {
        lbl.setFont(UITheme.fontLabel());
        lbl.setForeground(UITheme.palette().textMid());
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(UITheme.palette().bgPanel());
    }

    public static void styleField(JTextField field) {
        field.setFont(UITheme.fontBase());
        field.setForeground(UITheme.palette().textDark());
        field.setBackground(UITheme.palette().fieldBg());
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.palette().borderClr()),
            BorderFactory.createEmptyBorder(3, 7, 3, 7)
        ));
        if (field.getClientProperty("hoverInstalled") == null) {
            field.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (field.isEnabled())
                        field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UITheme.palette().accent(), 1),
                            BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.palette().borderClr()),
                        BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
            });
            field.putClientProperty("hoverInstalled", Boolean.TRUE);
        }
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.palette().borderClr()),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        );
    }
}
