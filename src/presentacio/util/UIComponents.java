package presentacio.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

import herramienta.i18n.Escapers;
import herramienta.i18n.I18n;
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

    public record SectionPanel(JPanel panel, GroupLayout layout) {}

    private UIComponents() {}

    public static SectionPanel sectionPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(UITheme.palette().bgPanel());
        GroupLayout gl = new GroupLayout(panel);
        panel.setLayout(gl);
        gl.setAutoCreateGaps(true);
        gl.setAutoCreateContainerGaps(true);
        return new SectionPanel(panel, gl);
    }

    public static JLabel sectionHeader(String key) {
        JLabel lbl = new JLabel(I18n.t(key));
        lbl.setFont(UITheme.fontBold());
        lbl.setForeground(UITheme.palette().accent());
        return lbl;
    }

    public static JLabel label(String key) {
        JLabel lbl = new JLabel(I18n.t(key));
        styleLabel(lbl);
        return lbl;
    }

    public static JTextField field(String id, String val) {
        JTextField field = new JTextField(val);
        styleField(field);
        field.putClientProperty("id", id);
        return field;
    }

    public static JPasswordField passwordField(String id, String val) {
        JPasswordField field = new JPasswordField(val);
        styleField(field);
        field.putClientProperty("id", id);
        return field;
    }

    public static JComboBox<String> combo(String id, String[] labels, String[] keys, String current) {
        JComboBox<String> cmb = new JComboBox<>(labels);
        if (keys != null && current != null) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(current)) {
                    cmb.setSelectedIndex(i);
                    break;
                }
            }
        }
        cmb.setFont(UITheme.fontBase());
        cmb.putClientProperty("id", id);
        return cmb;
    }

    public static JButton secondaryBtn(String label, String tip, ActionListener listener) {
        JButton btn = new JButton(label);
        styleSecondaryButton(btn);
        btn.setToolTipText(tip);
        btn.addActionListener(listener);
        return btn;
    }

    private static void styleButtonChrome(JButton btn, Color bg, Color fg, Font font) {
        btn.setUI(new BasicButtonUI());
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(font);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleAccentButton(JButton btn) {
        styleButtonChrome(btn, UITheme.palette().accent(), Color.WHITE, UITheme.fontBold());
    }

    public static void styleSecondaryButton(JButton btn) {
        styleButtonChrome(btn, UITheme.palette().secondaryBtnBg(), Color.WHITE, UITheme.fontBold());
    }

    public static void styleDangerButton(JButton btn) {
        styleButtonChrome(btn, UITheme.palette().danger(), Color.WHITE, UITheme.fontBold());
    }

    public static void styleSidebarButton(JButton btn) {
        styleButtonChrome(btn, UITheme.palette().sidebarBg(), UITheme.palette().sidebarText(), UITheme.fontBase());
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    }

    public static void styleLabel(JLabel lbl) {
        lbl.setFont(UITheme.fontLabel());
        lbl.setForeground(UITheme.palette().textMid());
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(UITheme.palette().bgPanel());
    }

    public static Border paddedLineBorder(Color c) {
        return paddedLineBorder(c, 3, 7, 3, 7);
    }

    public static Border paddedLineBorder(Color c, int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c),
            BorderFactory.createEmptyBorder(top, left, bottom, right)
        );
    }

    public static void styleField(JTextField field) {
        field.setFont(UITheme.fontBase());
        field.setForeground(UITheme.palette().textDark());
        field.setBackground(UITheme.palette().fieldBg());
        field.setBorder(paddedLineBorder(UITheme.palette().borderClr()));
        if (field.getClientProperty("hoverInstalled") == null) {
            field.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (field.isEnabled())
                        field.setBorder(paddedLineBorder(UITheme.palette().accent()));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    field.setBorder(paddedLineBorder(UITheme.palette().borderClr()));
                }
            });
            field.putClientProperty("hoverInstalled", Boolean.TRUE);
        }
    }

    public static Border cardBorder() {
        return paddedLineBorder(UITheme.palette().borderClr(), 6, 6, 6, 6);
    }

    public static void applySelectedColors(JComponent c, boolean selected) {
        c.setBackground(selected ? UITheme.palette().accent() : UITheme.palette().bgPanel());
        c.setForeground(selected ? Color.WHITE : UITheme.palette().textDark());
    }

    public static void nameScrollBar(JScrollBar sb) {
        java.awt.Component[] comps = sb.getComponents();
        for (int i = 0; i < comps.length; i++) {
            comps[i].getAccessibleContext().setAccessibleName(
                i == 0 ? I18n.t("acc_scroll_up") : I18n.t("acc_scroll_down"));
        }
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(UITheme.palette().bgMain());
        combo.setForeground(UITheme.palette().textDark());
        combo.setFont(UITheme.fontBase());
    }

    public static void attachResponsiveColumns(JScrollPane scroll, JPanel grid, int entryMinW) {
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            private int ultimCols = 2;
            @Override
            public void componentResized(ComponentEvent e) {
                int vpW = scroll.getViewport().getWidth();
                if (vpW <= 0) return;
                int cols = Math.max(1, vpW / entryMinW);
                if (cols != ultimCols) {
                    ultimCols = cols;
                    ((GridLayout) grid.getLayout()).setColumns(cols);
                    grid.revalidate();
                    grid.repaint();
                }
            }
        });
    }

    public static JScrollPane responsiveGridScroll(JPanel grid, int entryMinW) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.palette().bgPanel());
        wrapper.add(grid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.palette().bgPanel());
        attachResponsiveColumns(scroll, grid, entryMinW);
        return scroll;
    }

    /** Escapa text per incrustar en HTML Swing (etiquetes, botons amb HTML) respectant salts de línia. */
    public static String escapeHtml(String s) {
        return Escapers.htmlWithBreaks(s);
    }
}
