package presentacio;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import herramienta.UITheme;

/**
 * presentacio-layer factory for theme-aware Swing components.
 *
 * <p>All presentacio code should call into this class instead of
 * {@link UITheme#styleAccentButton(UITheme, JButton) UITheme.styleXxx} so
 * that swapping to a different look-and-feel does not require touching
 * every panel/control.
 */
public final class UIComponents {

    private UIComponents() {}

    public static void styleAccentButton(JButton btn) { UITheme.styleAccentButton(btn); }
    public static void styleSecondaryButton(JButton btn) { UITheme.styleSecondaryButton(btn); }
    public static void styleSidebarButton(JButton btn) { UITheme.styleSidebarButton(btn); }
    public static void styleLabel(JLabel lbl) { UITheme.styleLabel(lbl); }
    public static void styleField(JTextField field) { UITheme.styleField(field); }
    public static void stylePanel(JPanel panel) { UITheme.stylePanel(panel); }
    public static Border cardBorder() { return UITheme.cardBorder(); }
}
