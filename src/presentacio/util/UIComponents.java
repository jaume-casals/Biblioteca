package presentacio.util;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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

import herramienta.i18n.Escapers;
import herramienta.i18n.I18n;
import herramienta.ui.Tokens;
import herramienta.ui.UITheme;

/**
 * Fàbrica de la capa de presentació per a components Swing sensibles al tema.
 *
 * <p>Tot el codi de presentació hauria de cridar aquesta classe en lloc de
 * tocar directament els camps d'estil de {@link UITheme}, de manera que
 * canviar el tema (o el Look &amp; Feel) no obligui a tocar cada
 * panell/control.
 *
 * <p>Els components es deixen pintar al Look &amp; Feel (FlatLaf): així
 * hereten focus, hover, deshabilitat i ordenació de tabulació correctes.
 * Aquesta classe nomésOverrideix el que el LAF no pot saber: el color
 * concret d'un botó primari, d'un botó destructiu o d'una etiqueta de
 * secció.
 *
 * <p>Els cossos d'estil (button / label / panel / field / cardBorder) viuen
 * aquí; els tokens (colors, espaiat, radis, mides) viuen a
 * {@link UITheme} i {@link Tokens}.
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
        lbl.setFont(UITheme.fontBold().deriveFont((float) Tokens.FONT_SIZE_SECTION));
        lbl.setForeground(UITheme.palette().textMuted());
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

    // ── Botons ───────────────────────────────────────────────────────────────

    /**
     * Aplica el paper d'un botó: accent, secundari o destructiu.
     *
     * <p>No es substitueix el {@code UI} del botó: FlatLaf ja dibuixa el
     * contorn de focus, els estats hover/pressed i el text deshabilitat.
     * Només se li indica el color de fons i de text.
     *
     * <p>FlatLaf manté el fons personalitzat també quan el botó es
     * deshabilita, cosa que fa que un botó destructiu deshabilitat continuï
     * vermell. Per això l'estat deshabilitat es torna a pintar amb els
     * tokens deshabilitats: el component ho indica amb el text, el color i
     * la vora, no només amb el color.
     */
    private static void styleButtonChrome(javax.swing.AbstractButton btn, Color bg, Color fg, Font font) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(font);
        btn.setFocusPainted(true);
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(true);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        installEnabledState(btn, bg, fg);
    }

    /**
     * Manté el paper del botó coherent quan es deshabilita.
     *
     * <p>FlatLaf no aplicava el to de deshabilitat sobre un fons
     * personalitzat: el botó quedava saturat i l'usuari no podia saber que
     * era inactionable. Aquí el fons passa al rol de superfície apagada i
     * el text al rol de text deshabilitat.
     */
    private static void installEnabledState(javax.swing.AbstractButton btn, Color bg, Color fg) {
        if (Boolean.TRUE.equals(btn.getClientProperty("enabledStateInstalled"))) return;
        btn.putClientProperty("enabledStateInstalled", Boolean.TRUE);
        btn.addPropertyChangeListener("enabled", e -> {
            boolean enabled = btn.isEnabled();
            btn.setBackground(enabled ? bg : UITheme.palette().bgSunken());
            btn.setForeground(enabled ? fg : UITheme.palette().textDisabled());
        });
    }

    /** Botó principal: un únic accent blau/indi a la vista. */
    public static void styleAccentButton(javax.swing.AbstractButton btn) {
        styleButtonChrome(btn, UITheme.palette().accent(), UITheme.palette().textOnAccent(), UITheme.fontBold());
    }

    /** Botó secundari: superfície neutra amb vora. */
    public static void styleSecondaryButton(javax.swing.AbstractButton btn) {
        styleButtonChrome(btn, UITheme.palette().secondaryBtnBg(), UITheme.palette().textDark(), UITheme.fontBold());
    }

    /** Botó destructiu: vermell, reservat a accions que esborren. */
    public static void styleDangerButton(javax.swing.AbstractButton btn) {
        styleButtonChrome(btn, UITheme.palette().danger(), UITheme.palette().textOnAccent(), UITheme.fontBold());
    }

    /**
     * Botó de commutació (filtres actius, mode de vista).
     *
     * <p>El fons es commuta entre el rol secundari i el rol accent: el
     * component continua distingint-se pel text i pel contorn de focus, no
     * només pel color.
     *
     * <p>El fons i el text s'apliquen aquí, i no només quan canvia l'estat
     * d'activació: un botó de commutació que no repinta el seu fons conserva
     * el color del tema amb què es va construir, i en mode fosc queda una
     * caixa blanca al mig de la barra d'eines.
     */
    public static void styleToggleButton(javax.swing.JToggleButton btn) {
        btn.setFont(UITheme.fontBase());
        btn.setFocusPainted(true);
        btn.setBorderPainted(true);
        btn.setContentAreaFilled(true);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btn.setBackground(UITheme.palette().secondaryBtnBg());
        btn.setForeground(UITheme.palette().textDark());
        btn.addPropertyChangeListener("enabled", e -> {
            boolean enabled = btn.isEnabled();
            btn.setBackground(enabled ? UITheme.palette().secondaryBtnBg() : UITheme.palette().bgSunken());
            btn.setForeground(enabled ? UITheme.palette().textDark() : UITheme.palette().textDisabled());
        });
    }

    /**
     * Garanteix una alçada mínima de control.
     *
     * <p>No es fixa la mida preferida: el Look &amp; Feel calcula l'amplada
     * del botó a partir del text i de la font, i això evita que un text
     * quedi tallat. Fixar una amplada preferred congelada pot deixar el
     * botó més estret del que cal, i el text es retalla.
     */
    public static void setAlçadaControl(javax.swing.AbstractButton b, int alçada) {
        b.setMinimumSize(new Dimension(1, alçada));
    }

    public static void styleLabel(JLabel lbl) {
        lbl.setFont(UITheme.fontBase());
        lbl.setForeground(UITheme.palette().textDark());
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

    /**
     * Estil d'un camp de text.
     *
     * <p>No es toca la vora: FlatLaf dibuixa la vora, el focus i l'estat
     * deshabilitat, i una vora personalitzada els amagaria.
     */
    public static void styleField(JTextField field) {
        field.setFont(UITheme.fontBase());
        field.setForeground(UITheme.palette().textDark());
        field.setBackground(UITheme.palette().fieldBg());
        field.setCaretColor(UITheme.palette().accent());
    }

    public static void applySelectedColors(JComponent c, boolean selected) {
        c.setBackground(selected ? UITheme.palette().selectionBg() : UITheme.palette().bgPanel());
        c.setForeground(selected ? UITheme.palette().selectionText() : UITheme.palette().textDark());
    }

    public static void nameScrollBar(JScrollBar sb) {
        java.awt.Component[] comps = sb.getComponents();
        for (int i = 0; i < comps.length; i++) {
            comps[i].getAccessibleContext().setAccessibleName(
                i == 0 ? I18n.t("acc_scroll_up") : I18n.t("acc_scroll_down"));
        }
    }

    public static void styleCombo(JComboBox<?> combo) {
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
