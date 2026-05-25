package presentacio;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import herramienta.UITheme;

/** Thin presentacio-layer wrapper around {@link UITheme} style helpers. */
public final class UIComponents {

    private UIComponents() {}

    public static void styleAccentButton(JButton btn) { UITheme.styleAccentButton(btn); }
    public static void styleSecondaryButton(JButton btn) { UITheme.styleSecondaryButton(btn); }
    public static void styleLabel(JLabel lbl) { UITheme.styleLabel(lbl); }
    public static void styleField(JTextField field) { UITheme.styleField(field); }
    public static void stylePanel(JPanel panel) { UITheme.stylePanel(panel); }
}
