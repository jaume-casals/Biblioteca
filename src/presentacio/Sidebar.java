package presentacio;

import herramienta.UITheme;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

/**
 * Sidebar widget factory. Centralizes look (background, accent color, theme-button style)
 * so {@link MostrarBibliotecaPanel} and {@link MainFramePanel} can share appearance.
 */
public final class Sidebar {
    private Sidebar() {}

    public static JButton makeBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setForeground(fg != null ? fg : UITheme.SIDEBAR_TEXT);
        b.setBackground(UITheme.SIDEBAR_BG);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return b;
    }
}
